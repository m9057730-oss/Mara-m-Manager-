package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AppLanguage(val code: String, val nativeLabel: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "हिन्दी"),
    PUNJABI("pa", "ਪੰਜਾਬੀ");

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

enum class ThemePreset(val key: String, val title: String, val primaryHex: String, val secondaryHex: String) {
    SUNNY_MANGO("sunny_mango", "Sunny Mango", "#FF6B35", "#00B894"),
    MINT_FIESTA("mint_fiesta", "Mint Fiesta", "#00A878", "#FF9F1C"),
    BERRY_BLOSSOM("berry_blossom", "Berry Blossom", "#E84393", "#00CEC9"),
    ROYAL_SAFFRON("royal_saffron", "Royal Saffron", "#D97706", "#2563EB");

    companion object {
        fun fromKey(key: String): ThemePreset =
            entries.firstOrNull { it.key == key } ?: SUNNY_MANGO
    }
}

enum class OrderChannel(val key: String, val isThirdParty: Boolean) {
    TABLE("TABLE", false),
    OWN_DELIVERY("OWN_DELIVERY", false),
    PICKUP("PICKUP", false),
    ONLINE("ONLINE", false),
    ZOMATO("ZOMATO", true),
    SWIGGY("SWIGGY", true),
    OTHER_APP("OTHER_APP", true);

    companion object {
        fun fromKey(key: String): OrderChannel =
            entries.firstOrNull { it.key == key } ?: TABLE
    }
}

enum class OrderStatus(val key: String) {
    PENDING("PENDING"),
    RUNNING("RUNNING"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED");

    companion object {
        fun fromKey(key: String): OrderStatus =
            entries.firstOrNull { it.key == key } ?: PENDING
    }
}

enum class PaymentMethod(val key: String) {
    CASH("CASH"),
    CARD("CARD"),
    QR_UPI("QR_UPI"),
    ONLINE_PREPAID("ONLINE_PREPAID");

    companion object {
        fun fromKey(key: String): PaymentMethod =
            entries.firstOrNull { it.key == key } ?: CASH
    }
}

enum class AttendanceStatus(val key: String) {
    PRESENT("PRESENT"),
    HALF_DAY("HALF_DAY"),
    ABSENT("ABSENT");

    companion object {
        fun fromKey(key: String): AttendanceStatus =
            entries.firstOrNull { it.key == key } ?: PRESENT
    }
}

enum class InventoryCategory(val key: String) {
    KITCHEN("KITCHEN"),
    PACKING("PACKING"),
    TANDOOR_COAL("TANDOOR_COAL"),
    COLD_DRINKS("COLD_DRINKS");

    companion object {
        fun fromKey(key: String): InventoryCategory =
            entries.firstOrNull { it.key == key } ?: KITCHEN
    }
}

enum class ExpenseCategory(val key: String) {
    STAFF_SALARY("STAFF_SALARY"),
    SHOP_RENT("SHOP_RENT"),
    MATERIAL_GAS("MATERIAL_GAS"),
    EXPIRY_LOSS("EXPIRY_LOSS"),
    ELECTRICITY("ELECTRICITY"),
    STAFF_FOOD("STAFF_FOOD"),
    OTHER("OTHER");

    companion object {
        fun fromKey(key: String): ExpenseCategory =
            entries.firstOrNull { it.key == key } ?: OTHER
    }
}

enum class PartyStatus(val key: String) {
    BOOKED("BOOKED"),
    CONFIRMED("CONFIRMED"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED");

    companion object {
        fun fromKey(key: String): PartyStatus =
            entries.firstOrNull { it.key == key } ?: BOOKED
    }
}

@IgnoreExtraProperties
data class RestaurantProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val restaurantName: String = "Mara Spice Kitchen",
    val address: String = "Main Market Road",
    val phone: String = "",
    val gstin: String = "",
    val gstPercent: Double = 5.0,
    val dailyTarget: Double = 25000.0,
    val monthlyRent: Double = 30000.0,
    val monthlyElectricity: Double = 9000.0,
    val dailyStaffFoodBudget: Double = 600.0,
    val upiId: String = "marakitchen@upi",
    val autoKotEnabled: Boolean = true,
    val zomatoStoreId: String = "",
    val swiggyStoreId: String = "",
    val otherAppWebhook: String = "",
    val language: String = "en",
    val themePreset: String = "sunny_mango",
    val accentColorHex: String = "",
    val textColorHex: String = "",
    val wallpaperPath: String = "",
    val wallpaperOpacity: Double = 0.22,
    val darkMode: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "restaurantName" to restaurantName.ifBlank { "Mara Spice Kitchen" }.take(80),
        "address" to address.take(200),
        "phone" to phone.take(30),
        "gstin" to gstin.take(30),
        "gstPercent" to gstPercent.coerceIn(0.0, 30.0),
        "dailyTarget" to dailyTarget.coerceAtLeast(0.0),
        "monthlyRent" to monthlyRent.coerceAtLeast(0.0),
        "monthlyElectricity" to monthlyElectricity.coerceAtLeast(0.0),
        "dailyStaffFoodBudget" to dailyStaffFoodBudget.coerceAtLeast(0.0),
        "upiId" to upiId.take(100),
        "autoKotEnabled" to autoKotEnabled,
        "zomatoStoreId" to zomatoStoreId.take(100),
        "swiggyStoreId" to swiggyStoreId.take(100),
        "otherAppWebhook" to otherAppWebhook.take(300),
        "language" to language,
        "themePreset" to themePreset.take(40),
        "accentColorHex" to accentColorHex.take(16),
        "textColorHex" to textColorHex.take(16),
        "wallpaperPath" to wallpaperPath.take(500),
        "wallpaperOpacity" to wallpaperOpacity.coerceIn(0.0, 1.0),
        "darkMode" to darkMode,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "restaurantName" to restaurantName.ifBlank { "Mara Spice Kitchen" }.take(80),
        "address" to address.take(200),
        "phone" to phone.take(30),
        "gstin" to gstin.take(30),
        "gstPercent" to gstPercent.coerceIn(0.0, 30.0),
        "dailyTarget" to dailyTarget.coerceAtLeast(0.0),
        "monthlyRent" to monthlyRent.coerceAtLeast(0.0),
        "monthlyElectricity" to monthlyElectricity.coerceAtLeast(0.0),
        "dailyStaffFoodBudget" to dailyStaffFoodBudget.coerceAtLeast(0.0),
        "upiId" to upiId.take(100),
        "autoKotEnabled" to autoKotEnabled,
        "zomatoStoreId" to zomatoStoreId.take(100),
        "swiggyStoreId" to swiggyStoreId.take(100),
        "otherAppWebhook" to otherAppWebhook.take(300),
        "language" to language,
        "themePreset" to themePreset.take(40),
        "accentColorHex" to accentColorHex.take(16),
        "textColorHex" to textColorHex.take(16),
        "wallpaperPath" to wallpaperPath.take(500),
        "wallpaperOpacity" to wallpaperOpacity.coerceIn(0.0, 1.0),
        "darkMode" to darkMode,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class MenuItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val category: String = "Main Course",
    val price: Double = 0.0,
    val costPrice: Double = 0.0,
    val description: String = "",
    val veg: Boolean = true,
    val available: Boolean = true,
    val prepMinutes: Long = 15L,
    val courseType: String = "Fine Dine",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "name" to name.take(100),
        "category" to category.ifBlank { "Main Course" }.take(50),
        "price" to price.coerceAtLeast(0.0),
        "costPrice" to costPrice.coerceAtLeast(0.0),
        "description" to description.take(300),
        "veg" to veg,
        "available" to available,
        "prepMinutes" to prepMinutes.coerceIn(0L, 240L),
        "courseType" to courseType.take(40),
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "name" to name.take(100),
        "category" to category.ifBlank { "Main Course" }.take(50),
        "price" to price.coerceAtLeast(0.0),
        "costPrice" to costPrice.coerceAtLeast(0.0),
        "description" to description.take(300),
        "veg" to veg,
        "available" to available,
        "prepMinutes" to prepMinutes.coerceIn(0L, 240L),
        "courseType" to courseType.take(40),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class OrderRecord(
    @DocumentId val id: String = "",
    val orderNumber: String = "",
    val channel: String = OrderChannel.TABLE.key,
    val tableOrRef: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val itemsSummary: String = "",
    val itemsCount: Long = 1L,
    val subtotal: Double = 0.0,
    val gstEnabled: Boolean = true,
    val gstPercent: Double = 5.0,
    val gstAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val tipAmount: Double = 0.0,
    val paymentMethod: String = PaymentMethod.CASH.key,
    val status: String = OrderStatus.RUNNING.key,
    val kotPrinted: Boolean = false,
    val billPrinted: Boolean = false,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "orderNumber" to orderNumber.ifBlank { "ORD-${System.currentTimeMillis() % 10000}" }.take(30),
        "channel" to channel,
        "tableOrRef" to tableOrRef.take(60),
        "customerName" to customerName.take(80),
        "customerPhone" to customerPhone.take(30),
        "itemsSummary" to itemsSummary.ifBlank { "Assorted Order" }.take(1000),
        "itemsCount" to itemsCount.coerceIn(1L, 500L),
        "subtotal" to subtotal.coerceAtLeast(0.0),
        "gstEnabled" to gstEnabled,
        "gstPercent" to gstPercent.coerceIn(0.0, 30.0),
        "gstAmount" to gstAmount.coerceAtLeast(0.0),
        "totalAmount" to totalAmount.coerceAtLeast(0.0),
        "tipAmount" to tipAmount.coerceAtLeast(0.0),
        "paymentMethod" to paymentMethod,
        "status" to status,
        "kotPrinted" to kotPrinted,
        "billPrinted" to billPrinted,
        "notes" to notes.take(300),
        "createdAtMillis" to createdAtMillis.coerceAtLeast(0L),
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class StaffMember(
    @DocumentId val id: String = "",
    val name: String = "",
    val role: String = "Chef",
    val phone: String = "",
    val monthlySalary: Double = 15000.0,
    val dailyFoodAllowance: Double = 100.0,
    val shiftHours: Long = 9L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "name" to name.take(80),
        "role" to role.ifBlank { "Staff" }.take(50),
        "phone" to phone.take(30),
        "monthlySalary" to monthlySalary.coerceAtLeast(0.0),
        "dailyFoodAllowance" to dailyFoodAllowance.coerceAtLeast(0.0),
        "shiftHours" to shiftHours.coerceIn(1L, 24L),
        "active" to active,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class AttendanceRecord(
    @DocumentId val id: String = "",
    val staffId: String = "",
    val staffName: String = "",
    val role: String = "",
    val dateStr: String = "",
    val checkInTimeStr: String = "",
    val checkOutTimeStr: String = "",
    val status: String = AttendanceStatus.PRESENT.key,
    val tipEarned: Double = 0.0,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "staffId" to staffId.take(128),
        "staffName" to staffName.take(80),
        "role" to role.take(50),
        "dateStr" to dateStr.take(20),
        "checkInTimeStr" to checkInTimeStr.take(30),
        "checkOutTimeStr" to checkOutTimeStr.take(30),
        "status" to status,
        "tipEarned" to tipEarned.coerceAtLeast(0.0),
        "notes" to notes.take(200),
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class InventoryItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val category: String = InventoryCategory.KITCHEN.key,
    val quantity: Double = 0.0,
    val unit: String = "kg",
    val minThreshold: Double = 5.0,
    val costPerUnit: Double = 0.0,
    val expiryDate: String = "",
    val supplierPhone: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isLowStock: Boolean get() = quantity <= minThreshold
    val isOutOfStock: Boolean get() = quantity <= 0.0

    fun toCreateMap(): Map<String, Any> = mapOf(
        "name" to name.take(100),
        "category" to category,
        "quantity" to quantity.coerceAtLeast(0.0),
        "unit" to unit.ifBlank { "pcs" }.take(20),
        "minThreshold" to minThreshold.coerceAtLeast(0.0),
        "costPerUnit" to costPerUnit.coerceAtLeast(0.0),
        "expiryDate" to expiryDate.take(30),
        "supplierPhone" to supplierPhone.take(30),
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class DailyExpense(
    @DocumentId val id: String = "",
    val title: String = "",
    val category: String = ExpenseCategory.MATERIAL_GAS.key,
    val amount: Double = 0.0,
    val dateStr: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "title" to title.take(120),
        "category" to category,
        "amount" to amount.coerceAtLeast(0.0),
        "dateStr" to dateStr.take(20),
        "notes" to notes.take(300),
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class PartyBooking(
    @DocumentId val id: String = "",
    val partyName: String = "",
    val partyPhone: String = "",
    val eventDate: String = "",
    val guestCount: Long = 20L,
    val totalBudget: Double = 0.0,
    val advancePaid: Double = 0.0,
    val inventoryUsedSummary: String = "",
    val chefRequirements: String = "",
    val status: String = PartyStatus.BOOKED.key,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "partyName" to partyName.take(100),
        "partyPhone" to partyPhone.take(30),
        "eventDate" to eventDate.take(40),
        "guestCount" to guestCount.coerceIn(1L, 10000L),
        "totalBudget" to totalBudget.coerceAtLeast(0.0),
        "advancePaid" to advancePaid.coerceAtLeast(0.0),
        "inventoryUsedSummary" to inventoryUsedSummary.take(1000),
        "chefRequirements" to chefRequirements.take(1000),
        "status" to status,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
