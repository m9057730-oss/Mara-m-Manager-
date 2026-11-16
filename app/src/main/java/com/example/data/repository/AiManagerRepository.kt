package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.AttendanceRecord
import com.example.data.model.InventoryItem
import com.example.data.model.MenuItem
import com.example.data.model.OrderRecord
import com.example.data.model.StaffMember
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class ScannedMenuDraft(
    val name: String,
    val category: String,
    val price: Double,
    val costPrice: Double,
    val description: String,
    val veg: Boolean,
    val prepMinutes: Long,
    val courseType: String
)

data class AiStaffSalaryItem(
    val staffName: String,
    val role: String,
    val daysEquivalent: Double,
    val earnedSalary: Double,
    val tipShare: Double,
    val foodDeductionOrAllowance: Double,
    val netPayout: Double,
    val breakdownNote: String
)

data class AiSalaryReport(
    val summary: String,
    val totalPayroll: Double,
    val totalTipsDistributed: Double,
    val items: List<AiStaffSalaryItem>
)

data class AiAutoAction(
    val actionType: String, // "NONE", "ADD_INVENTORY", "ADD_EXPENSE", "ADD_MENU_ITEM"
    val titleOrName: String = "",
    val category: String = "",
    val amountOrPrice: Double = 0.0,
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val veg: Boolean = true
)

data class AiManagerReply(
    val replyText: String,
    val suggestedAction: AiAutoAction = AiAutoAction("NONE")
)

class AiManagerRepository {

    private val menuScannerModel by lazy {
        val itemSchema = Schema.obj(
            properties = mapOf(
                "name" to Schema.string("Dish name"),
                "category" to Schema.enumeration(
                    listOf("Starters", "Main Course", "Tandoor & Breads", "Beverages", "Desserts"),
                    "Menu category"
                ),
                "price" to Schema.double("Selling price in currency"),
                "costPrice" to Schema.double("Estimated ingredient cost price"),
                "description" to Schema.string("Short appetizing fine-dining description"),
                "veg" to Schema.boolean("True if vegetarian"),
                "prepMinutes" to Schema.integer("Preparation time in minutes"),
                "courseType" to Schema.enumeration(
                    listOf("Fine Dine", "Chef Special", "Classic"),
                    "Course style"
                )
            )
        )
        val rootSchema = Schema.obj(
            properties = mapOf(
                "items" to Schema.array(itemSchema, "Extracted menu dishes")
            )
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = rootSchema
            }
        )
    }

    private val salaryModel by lazy {
        val staffSalarySchema = Schema.obj(
            properties = mapOf(
                "staffName" to Schema.string("Staff member name"),
                "role" to Schema.string("Staff role"),
                "daysEquivalent" to Schema.double("Effective worked days (present=1, half_day=0.5)"),
                "earnedSalary" to Schema.double("Prorated earned base salary"),
                "tipShare" to Schema.double("Recommended fair tip share"),
                "foodDeductionOrAllowance" to Schema.double("Food allowance benefit"),
                "netPayout" to Schema.double("Total net payout (earnedSalary + tipShare)"),
                "breakdownNote" to Schema.string("Short explanation")
            )
        )
        val reportSchema = Schema.obj(
            properties = mapOf(
                "summary" to Schema.string("Executive summary of payroll and tip split"),
                "totalPayroll" to Schema.double("Sum of earned salaries"),
                "totalTipsDistributed" to Schema.double("Sum of tips distributed"),
                "items" to Schema.array(staffSalarySchema, "Per-staff calculations")
            )
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = reportSchema
            }
        )
    }

    private val assistantModel by lazy {
        val actionSchema = Schema.obj(
            properties = mapOf(
                "actionType" to Schema.enumeration(
                    listOf("NONE", "ADD_INVENTORY", "ADD_EXPENSE", "ADD_MENU_ITEM"),
                    "Whether to auto-create a restaurant record"
                ),
                "titleOrName" to Schema.string("Item, expense, or dish name"),
                "category" to Schema.string("Category matching app enums"),
                "amountOrPrice" to Schema.double("Price or expense amount"),
                "quantity" to Schema.double("Inventory quantity if applicable"),
                "unit" to Schema.string("Inventory unit if applicable"),
                "veg" to Schema.boolean("Veg flag for menu item")
            ),
            optionalProperties = listOf("titleOrName", "category", "amountOrPrice", "quantity", "unit", "veg")
        )
        val responseSchemaObj = Schema.obj(
            properties = mapOf(
                "replyText" to Schema.string("Helpful, actionable restaurant manager reply in the user's language"),
                "suggestedAction" to actionSchema
            )
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = responseSchemaObj
            }
        )
    }

    suspend fun scanMenuFromBitmap(rawBitmap: Bitmap, languageCode: String): Result<List<ScannedMenuDraft>> {
        return try {
            val scaled = scaleBitmapDown(rawBitmap, 1280)
            val response = withTimeout(60_000L) {
                menuScannerModel.generateContent(
                    content {
                        image(scaled)
                        text(
                            "Extract every food and drink item visible on this menu photo. " +
                                "Adapt descriptions into an inviting fine-dining style. " +
                                "If price is missing, estimate a reasonable local restaurant price. " +
                                "Preferred language for descriptions: $languageCode."
                        )
                    }
                )
            }
            val jsonText = response.text ?: return Result.failure(IllegalStateException("Empty AI response"))
            val root = JSONObject(jsonText)
            val arr = root.getJSONArray("items")
            val result = (0 until arr.length()).map { idx ->
                val obj = arr.getJSONObject(idx)
                ScannedMenuDraft(
                    name = obj.optString("name", "Chef Special Dish").trim(),
                    category = obj.optString("category", "Main Course"),
                    price = obj.optDouble("price", 150.0).coerceAtLeast(0.0),
                    costPrice = obj.optDouble("costPrice", 60.0).coerceAtLeast(0.0),
                    description = obj.optString("description", ""),
                    veg = obj.optBoolean("veg", true),
                    prepMinutes = obj.optLong("prepMinutes", 15L).coerceIn(1L, 180L),
                    courseType = obj.optString("courseType", "Fine Dine")
                )
            }
            Result.success(result)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Menu scan timed out", e)
            Result.failure(Exception("Menu scan timed out after 60 seconds. Please try a clearer photo."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Menu scan failed", e)
            Result.failure(e)
        }
    }

    suspend fun calculateSalaryAndTips(
        staff: List<StaffMember>,
        attendance: List<AttendanceRecord>,
        totalTipPool: Double,
        languageCode: String
    ): Result<AiSalaryReport> {
        return try {
            val staffText = staff.joinToString("\n") { s ->
                val records = attendance.filter { it.staffId == s.id }
                val present = records.count { it.status == "PRESENT" }
                val half = records.count { it.status == "HALF_DAY" }
                val absent = records.count { it.status == "ABSENT" }
                val loggedTips = records.sumOf { it.tipEarned }
                "- ${s.name} (${s.role}): monthlySalary=${s.monthlySalary}, dailyFoodAllowance=${s.dailyFoodAllowance}, presentDays=$present, halfDays=$half, absentDays=$absent, loggedTips=$loggedTips"
            }
            val prompt = """
                Calculate fair earned salary and tip distribution for this restaurant staff based on a 30-day month.
                If attendance logs are 0 for a staff member, calculate 1-day prorated salary + fair tip share so the owner sees today's payout.
                Total daily/pool tips to distribute: $totalTipPool.
                Respond in language '$languageCode' for summary and breakdownNote.
                Staff data:
                $staffText
            """.trimIndent()

            val response = withTimeout(60_000L) {
                salaryModel.generateContent(prompt)
            }
            val jsonText = response.text ?: return Result.failure(IllegalStateException("Empty AI response"))
            val root = JSONObject(jsonText)
            val arr = root.getJSONArray("items")
            val items = (0 until arr.length()).map { idx ->
                val obj = arr.getJSONObject(idx)
                AiStaffSalaryItem(
                    staffName = obj.optString("staffName"),
                    role = obj.optString("role"),
                    daysEquivalent = obj.optDouble("daysEquivalent", 1.0),
                    earnedSalary = obj.optDouble("earnedSalary", 0.0),
                    tipShare = obj.optDouble("tipShare", 0.0),
                    foodDeductionOrAllowance = obj.optDouble("foodDeductionOrAllowance", 0.0),
                    netPayout = obj.optDouble("netPayout", 0.0),
                    breakdownNote = obj.optString("breakdownNote", "")
                )
            }
            Result.success(
                AiSalaryReport(
                    summary = root.optString("summary", ""),
                    totalPayroll = root.optDouble("totalPayroll", 0.0),
                    totalTipsDistributed = root.optDouble("totalTipsDistributed", 0.0),
                    items = items
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI salary calc timed out", e)
            Result.failure(Exception("Salary calculation timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI salary calc failed", e)
            Result.failure(e)
        }
    }

    suspend fun askAutoManager(
        userPrompt: String,
        restaurantName: String,
        todayRevenue: Double,
        todayTotalCost: Double,
        lowStockItems: List<InventoryItem>,
        ordersCount: Int,
        staffCount: Int,
        languageCode: String
    ): Result<AiManagerReply> {
        return try {
            val lowStockStr = if (lowStockItems.isEmpty()) "None"
            else lowStockItems.joinToString(", ") { "${it.name} (${it.quantity} ${it.unit})" }

            val prompt = """
                You are Mara Manager AI, a proactive smart restaurant operations assistant for '$restaurantName'.
                Live snapshot:
                - Today Revenue: $todayRevenue
                - Today Total Cost: $todayTotalCost
                - Today Net P&L: ${todayRevenue - todayTotalCost}
                - Active/Total Orders: $ordersCount
                - Staff Count: $staffCount
                - Low Stock Alerts: $lowStockStr
                Preferred response language code: '$languageCode' (en=English, hi=Hindi, pa=Punjabi).
                
                If the user asks to add an inventory item (e.g. "add 20kg tandoor coal", "add 24 Pepsi"), set actionType="ADD_INVENTORY" and category in ["KITCHEN","PACKING","TANDOOR_COAL","COLD_DRINKS"].
                If the user asks to log an expense (e.g. "add gas expense 1900"), set actionType="ADD_EXPENSE" and category in ["STAFF_SALARY","SHOP_RENT","MATERIAL_GAS","EXPIRY_LOSS","ELECTRICITY","STAFF_FOOD","OTHER"].
                If the user asks to add a dish/menu item (e.g. "add Paneer Tikka 280"), set actionType="ADD_MENU_ITEM" and category in ["Starters","Main Course","Tandoor & Breads","Beverages","Desserts"].
                Otherwise set actionType="NONE" and give a crisp helpful answer.
                
                User request: $userPrompt
            """.trimIndent()

            val response = withTimeout(60_000L) {
                assistantModel.generateContent(prompt)
            }
            val jsonText = response.text ?: return Result.failure(IllegalStateException("Empty AI response"))
            val root = JSONObject(jsonText)
            val actObj = root.optJSONObject("suggestedAction")
            val action = if (actObj != null) {
                AiAutoAction(
                    actionType = actObj.optString("actionType", "NONE"),
                    titleOrName = actObj.optString("titleOrName", ""),
                    category = actObj.optString("category", ""),
                    amountOrPrice = actObj.optDouble("amountOrPrice", 0.0),
                    quantity = actObj.optDouble("quantity", 1.0),
                    unit = actObj.optString("unit", "pcs"),
                    veg = actObj.optBoolean("veg", true)
                )
            } else {
                AiAutoAction("NONE")
            }
            Result.success(
                AiManagerReply(
                    replyText = root.optString("replyText", ""),
                    suggestedAction = action
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI Assistant timed out", e)
            Result.failure(Exception("AI Assistant timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI Assistant failed", e)
            Result.failure(e)
        }
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxEdge && h <= maxEdge) return bitmap
        val ratio = maxEdge.toFloat() / maxOf(w, h).toFloat()
        return Bitmap.createScaledBitmap(
            bitmap,
            (w * ratio).toInt().coerceAtLeast(1),
            (h * ratio).toInt().coerceAtLeast(1),
            true
        )
    }

    private companion object {
        const val TAG = "AiManagerRepo"
    }
}
