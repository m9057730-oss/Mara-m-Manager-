package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.DailyExpense
import com.example.data.model.ExpenseCategory
import com.example.data.model.InventoryCategory
import com.example.data.model.InventoryItem
import com.example.data.model.MenuItem
import com.example.data.model.OrderChannel
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.PartyBooking
import com.example.data.model.PaymentMethod
import com.example.data.model.RestaurantProfile
import com.example.data.model.StaffMember
import com.example.data.repository.AiManagerReply
import com.example.data.repository.AiManagerRepository
import com.example.data.repository.AiSalaryReport
import com.example.data.repository.RestaurantRepository
import com.example.data.repository.ScannedMenuDraft
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class DashboardMetrics(
    val todaySales: Double = 0.0,
    val todayOrdersCount: Int = 0,
    val liveOrdersCount: Int = 0,
    val pendingOrdersCount: Int = 0,
    val zomatoCount: Int = 0,
    val swiggyCount: Int = 0,
    val otherAppCount: Int = 0,
    val thirdPartySales: Double = 0.0,
    val ownDeliveryCount: Int = 0,
    val ownDeliverySales: Double = 0.0,
    val tableCount: Int = 0,
    val pickupCount: Int = 0,
    val onlineCount: Int = 0,
    val dailyRentCost: Double = 0.0,
    val dailyElectricityCost: Double = 0.0,
    val dailyStaffFoodCost: Double = 0.0,
    val dailyLaborCost: Double = 0.0,
    val todayGasAndMaterialCost: Double = 0.0,
    val todayExpiryLoss: Double = 0.0,
    val todayOtherExpenses: Double = 0.0,
    val todayTotalCost: Double = 0.0,
    val todayNetPnl: Double = 0.0,
    val tipsToday: Double = 0.0,
    val tipsMonth: Double = 0.0,
    val targetProgress: Float = 0f
)

data class MaraWorkspaceData(
    val profile: RestaurantProfile = RestaurantProfile(),
    val menuItems: List<MenuItem> = emptyList(),
    val orders: List<OrderRecord> = emptyList(),
    val staff: List<StaffMember> = emptyList(),
    val attendance: List<AttendanceRecord> = emptyList(),
    val inventory: List<InventoryItem> = emptyList(),
    val expenses: List<DailyExpense> = emptyList(),
    val partyBookings: List<PartyBooking> = emptyList(),
    val metrics: DashboardMetrics = DashboardMetrics(),
    val lowStockItems: List<InventoryItem> = emptyList(),
    val nowMillis: Long = System.currentTimeMillis()
)

data class AiChatMessage(
    val isUser: Boolean,
    val text: String,
    val executedActionNote: String? = null
)

class MaraManagerViewModel(
    private val repo: RestaurantRepository,
    private val aiRepo: AiManagerRepository,
    private val currentUserId: String
) : ViewModel() {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    init {
        viewModelScope.launch {
            repo.ensureProfileInitialized()
        }
    }

    private val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }

    private val coreCollectionsFlow = combine(
        repo.observeProfile(currentUserId),
        repo.observeMenuItems(currentUserId),
        repo.observeOrders(currentUserId),
        repo.observeStaff(currentUserId),
        repo.observeAttendance(currentUserId)
    ) { profile, menu, orders, staff, attendance ->
        Quint(profile, menu, orders, staff, attendance)
    }

    private val opsCollectionsFlow = combine(
        repo.observeInventory(currentUserId),
        repo.observeExpenses(currentUserId),
        repo.observePartyBookings(currentUserId),
        tickerFlow
    ) { inventory, expenses, bookings, nowMillis ->
        Quad(inventory, expenses, bookings, nowMillis)
    }

    val workspaceState: StateFlow<UiState<MaraWorkspaceData>> = combine(
        coreCollectionsFlow,
        opsCollectionsFlow
    ) { core, ops ->
        val (profile, menu, orders, staff, attendance) = core
        val (inventory, expenses, bookings, nowMillis) = ops
        val metrics = computeMetrics(profile, orders, staff, attendance, expenses)
        val lowStock = inventory.filter { it.isLowStock }
        UiState.Success(
            MaraWorkspaceData(
                profile = profile,
                menuItems = menu,
                orders = orders,
                staff = staff,
                attendance = attendance,
                inventory = inventory,
                expenses = expenses,
                partyBookings = bookings,
                metrics = metrics,
                lowStockItems = lowStock,
                nowMillis = nowMillis
            )
        ) as UiState<MaraWorkspaceData>
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.e(TAG, "Error observing restaurant workspace", rootError)
        emit(UiState.Error(rootError.localizedMessage ?: "Failed to sync restaurant data"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading
    )

    // --- AI States ---
    private val _scannedMenuState = MutableStateFlow<UiState<List<ScannedMenuDraft>>?>(null)
    val scannedMenuState: StateFlow<UiState<List<ScannedMenuDraft>>?> = _scannedMenuState.asStateFlow()

    private val _aiSalaryState = MutableStateFlow<UiState<AiSalaryReport>?>(null)
    val aiSalaryState: StateFlow<UiState<AiSalaryReport>?> = _aiSalaryState.asStateFlow()

    private val _aiChatMessages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val aiChatMessages: StateFlow<List<AiChatMessage>> = _aiChatMessages.asStateFlow()

    private val _aiChatLoading = MutableStateFlow(false)
    val aiChatLoading: StateFlow<Boolean> = _aiChatLoading.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun clearToast() {
        _toastMessage.value = null
    }

    fun clearScannedMenu() {
        _scannedMenuState.value = null
    }

    fun clearAiSalary() {
        _aiSalaryState.value = null
    }

    // --- Profile & Settings ---
    fun saveProfile(profile: RestaurantProfile) {
        viewModelScope.launch {
            repo.saveProfile(profile)
                .onSuccess { _toastMessage.value = "Settings saved" }
                .onFailure { _toastMessage.value = it.localizedMessage ?: "Save failed" }
        }
    }

    fun updateLanguage(profile: RestaurantProfile, languageCode: String) {
        saveProfile(profile.copy(language = languageCode))
    }

    fun testThirdPartyWebhook(url: String) {
        if (url.isBlank()) {
            _toastMessage.value = "Enter a webhook or bridge URL first"
            return
        }
        viewModelScope.launch {
            val statusMsg = withContext(Dispatchers.IO) {
                try {
                    val req = Request.Builder().url(url.trim()).get().build()
                    httpClient.newCall(req).execute().use { resp ->
                        "Webhook responded HTTP ${resp.code}"
                    }
                } catch (e: Exception) {
                    "Connection failed: ${e.localizedMessage}"
                }
            }
            _toastMessage.value = statusMsg
        }
    }

    // --- Menu & AI Photo Scan ---
    fun addMenuItem(item: MenuItem) {
        viewModelScope.launch {
            repo.addMenuItem(item)
                .onSuccess { _toastMessage.value = "${item.name} added to menu" }
                .onFailure { _toastMessage.value = it.localizedMessage }
        }
    }

    fun toggleMenuAvailability(item: MenuItem) {
        viewModelScope.launch {
            repo.updateMenuItem(item.copy(available = !item.available))
        }
    }

    fun deleteMenuItem(itemId: String) {
        viewModelScope.launch {
            repo.deleteMenuItem(itemId)
        }
    }

    fun scanMenuPhoto(bitmap: Bitmap, languageCode: String) {
        viewModelScope.launch {
            _scannedMenuState.value = UiState.Loading
            aiRepo.scanMenuFromBitmap(bitmap, languageCode)
                .onSuccess { drafts ->
                    _scannedMenuState.value = UiState.Success(drafts)
                }
                .onFailure { err ->
                    _scannedMenuState.value = UiState.Error(err.localizedMessage ?: "Menu scan failed")
                }
        }
    }

    fun saveScannedMenuDrafts(drafts: List<ScannedMenuDraft>) {
        viewModelScope.launch {
            val items = drafts.map {
                MenuItem(
                    name = it.name,
                    category = it.category,
                    price = it.price,
                    costPrice = it.costPrice,
                    description = it.description,
                    veg = it.veg,
                    available = true,
                    prepMinutes = it.prepMinutes,
                    courseType = it.courseType
                )
            }
            repo.addMenuItemsBatch(items)
                .onSuccess { count ->
                    _toastMessage.value = "$count scanned dishes saved to Fine-Dine Menu!"
                    _scannedMenuState.value = null
                }
                .onFailure { _toastMessage.value = it.localizedMessage }
        }
    }

    // --- Orders & Billing ---
    fun createOrder(
        order: OrderRecord,
        onCreated: (OrderRecord) -> Unit = {}
    ) {
        viewModelScope.launch {
            repo.createOrder(order)
                .onSuccess { id ->
                    _toastMessage.value = "Order ${order.orderNumber} created"
                    onCreated(order.copy(id = id))
                }
                .onFailure { _toastMessage.value = it.localizedMessage }
        }
    }

    fun advanceOrderStatus(order: OrderRecord, newStatus: OrderStatus) {
        viewModelScope.launch {
            repo.updateOrderStatus(order.id, newStatus)
                .onFailure { _toastMessage.value = it.localizedMessage }
        }
    }

    fun toggleOrderGst(order: OrderRecord, defaultGstPercent: Double) {
        if (order.status == OrderStatus.COMPLETED.key || order.status == OrderStatus.CANCELLED.key) {
            _toastMessage.value = "Completed order billing is locked"
            return
        }
        viewModelScope.launch {
            val newGstEnabled = !order.gstEnabled
            val pct = if (order.gstPercent > 0.0) order.gstPercent else defaultGstPercent
            val newGstAmt = if (newGstEnabled) (order.subtotal * pct / 100.0) else 0.0
            val newTotal = order.subtotal + newGstAmt
            repo.updateOrderBilling(
                orderId = order.id,
                gstEnabled = newGstEnabled,
                gstPercent = pct,
                gstAmount = newGstAmt,
                totalAmount = newTotal,
                tipAmount = order.tipAmount,
                paymentMethod = order.paymentMethod
            ).onSuccess {
                _toastMessage.value = if (newGstEnabled) "GST Added" else "GST Cancelled"
            }
        }
    }

    fun markKotPrinted(orderId: String) {
        viewModelScope.launch { repo.markOrderPrinted(orderId, kot = true) }
    }

    fun markBillPrinted(orderId: String) {
        viewModelScope.launch { repo.markOrderPrinted(orderId, bill = true) }
    }

    fun deleteOrder(orderId: String) {
        viewModelScope.launch { repo.deleteOrder(orderId) }
    }

    // --- Staff & Attendance & AI Salary ---
    fun addStaff(member: StaffMember) {
        viewModelScope.launch {
            repo.addStaffMember(member)
                .onSuccess { _toastMessage.value = "${member.name} added" }
                .onFailure { _toastMessage.value = it.localizedMessage }
        }
    }

    fun deleteStaff(staffId: String) {
        viewModelScope.launch { repo.deleteStaffMember(staffId) }
    }

    fun markAttendance(
        staff: StaffMember,
        status: AttendanceStatus,
        tipEarned: Double,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val now = Date()
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now)
            val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(now)
            val record = AttendanceRecord(
                staffId = staff.id,
                staffName = staff.name,
                role = staff.role,
                dateStr = dateStr,
                checkInTimeStr = if (status == AttendanceStatus.ABSENT) "-" else timeStr,
                checkOutTimeStr = "",
                status = status.key,
                tipEarned = tipEarned,
                notes = notes
            )
            repo.recordAttendance(record)
                .onSuccess { _toastMessage.value = "${staff.name}: ${status.key} at $timeStr" }
                .onFailure { _toastMessage.value = it.localizedMessage }
        }
    }

    fun checkOutAttendance(attendanceId: String, tipEarned: Double) {
        viewModelScope.launch {
            val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
            repo.updateAttendanceCheckOut(attendanceId, timeStr, tipEarned)
                .onSuccess { _toastMessage.value = "Checked out at $timeStr" }
        }
    }

    fun runAiSalaryCalculation(
        staff: List<StaffMember>,
        attendance: List<AttendanceRecord>,
        tipPool: Double,
        languageCode: String
    ) {
        if (staff.isEmpty()) {
            _toastMessage.value = "Add staff members first"
            return
        }
        viewModelScope.launch {
            _aiSalaryState.value = UiState.Loading
            aiRepo.calculateSalaryAndTips(staff, attendance, tipPool, languageCode)
                .onSuccess { report -> _aiSalaryState.value = UiState.Success(report) }
                .onFailure { err ->
                    _aiSalaryState.value = UiState.Error(err.localizedMessage ?: "AI calculation failed")
                }
        }
    }

    // --- Inventory ---
    fun addInventoryItem(item: InventoryItem) {
        viewModelScope.launch {
            repo.addInventoryItem(item)
                .onSuccess { _toastMessage.value = "${item.name} added to inventory" }
                .onFailure { _toastMessage.value = it.localizedMessage }
        }
    }

    fun adjustInventoryQuantity(item: InventoryItem, delta: Double) {
        viewModelScope.launch {
            val updated = (item.quantity + delta).coerceAtLeast(0.0)
            repo.updateInventoryQuantity(item.id, updated)
        }
    }

    fun deleteInventoryItem(itemId: String) {
        viewModelScope.launch { repo.deleteInventoryItem(itemId) }
    }

    // --- Expenses ---
    fun addExpense(expense: DailyExpense) {
        viewModelScope.launch {
            repo.addExpense(expense)
                .onSuccess { _toastMessage.value = "Expense logged" }
                .onFailure { _toastMessage.value = it.localizedMessage }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch { repo.deleteExpense(expenseId) }
    }

    // --- Party Bookings ---
    fun addPartyBooking(booking: PartyBooking) {
        viewModelScope.launch {
            repo.addPartyBooking(booking)
                .onSuccess { _toastMessage.value = "Party booked: ${booking.partyName}" }
                .onFailure { _toastMessage.value = it.localizedMessage }
        }
    }

    fun updatePartyBooking(booking: PartyBooking) {
        viewModelScope.launch {
            repo.updatePartyBooking(booking)
                .onSuccess { _toastMessage.value = "Party booking updated" }
                .onFailure { _toastMessage.value = it.localizedMessage }
        }
    }

    fun deletePartyBooking(bookingId: String) {
        viewModelScope.launch { repo.deletePartyBooking(bookingId) }
    }

    // --- AI Auto-Manager Assistant ---
    fun sendAiAssistantMessage(prompt: String, data: MaraWorkspaceData) {
        if (prompt.isBlank()) return
        val currentList = _aiChatMessages.value + AiChatMessage(isUser = true, text = prompt)
        _aiChatMessages.value = currentList
        _aiChatLoading.value = true

        viewModelScope.launch {
            val res = aiRepo.askAutoManager(
                userPrompt = prompt,
                restaurantName = data.profile.restaurantName,
                todayRevenue = data.metrics.todaySales,
                todayTotalCost = data.metrics.todayTotalCost,
                lowStockItems = data.lowStockItems,
                ordersCount = data.metrics.todayOrdersCount,
                staffCount = data.staff.size,
                languageCode = data.profile.language
            )
            res.onSuccess { reply ->
                val actionNote = executeAiAutoAction(reply)
                _aiChatMessages.value = _aiChatMessages.value + AiChatMessage(
                    isUser = false,
                    text = reply.replyText,
                    executedActionNote = actionNote
                )
            }.onFailure { err ->
                _aiChatMessages.value = _aiChatMessages.value + AiChatMessage(
                    isUser = false,
                    text = "Error: ${err.localizedMessage ?: "AI call failed"}"
                )
            }
            _aiChatLoading.value = false
        }
    }

    private suspend fun executeAiAutoAction(reply: AiManagerReply): String? {
        val act = reply.suggestedAction
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return when (act.actionType) {
            "ADD_INVENTORY" -> {
                if (act.titleOrName.isBlank()) return null
                val cat = InventoryCategory.entries.firstOrNull { it.key == act.category }?.key
                    ?: InventoryCategory.KITCHEN.key
                repo.addInventoryItem(
                    InventoryItem(
                        name = act.titleOrName,
                        category = cat,
                        quantity = act.quantity.coerceAtLeast(0.0),
                        unit = act.unit.ifBlank { "pcs" },
                        minThreshold = 5.0,
                        costPerUnit = act.amountOrPrice.coerceAtLeast(0.0)
                    )
                )
                "Auto-added inventory: ${act.titleOrName} (${act.quantity} ${act.unit})"
            }
            "ADD_EXPENSE" -> {
                if (act.titleOrName.isBlank() || act.amountOrPrice <= 0.0) return null
                val cat = ExpenseCategory.entries.firstOrNull { it.key == act.category }?.key
                    ?: ExpenseCategory.OTHER.key
                repo.addExpense(
                    DailyExpense(
                        title = act.titleOrName,
                        category = cat,
                        amount = act.amountOrPrice,
                        dateStr = todayStr
                    )
                )
                "Auto-logged expense: ${act.titleOrName} (₹${act.amountOrPrice.toInt()})"
            }
            "ADD_MENU_ITEM" -> {
                if (act.titleOrName.isBlank()) return null
                repo.addMenuItem(
                    MenuItem(
                        name = act.titleOrName,
                        category = act.category.ifBlank { "Main Course" },
                        price = act.amountOrPrice.coerceAtLeast(50.0),
                        veg = act.veg,
                        available = true
                    )
                )
                "Auto-added dish: ${act.titleOrName} (₹${act.amountOrPrice.toInt()})"
            }
            else -> null
        }
    }

    private fun computeMetrics(
        profile: RestaurantProfile,
        orders: List<OrderRecord>,
        staff: List<StaffMember>,
        attendance: List<AttendanceRecord>,
        expenses: List<DailyExpense>
    ): DashboardMetrics {
        val now = Date()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now)
        val monthPrefix = SimpleDateFormat("yyyy-MM", Locale.US).format(now)
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val todayOrders = orders.filter { ord ->
            val d = ord.createdAt?.toDate() ?: Date(ord.createdAtMillis)
            dateFmt.format(d) == todayStr && ord.status != OrderStatus.CANCELLED.key
        }
        val monthOrders = orders.filter { ord ->
            val d = ord.createdAt?.toDate() ?: Date(ord.createdAtMillis)
            dateFmt.format(d).startsWith(monthPrefix) && ord.status != OrderStatus.CANCELLED.key
        }

        val todaySales = todayOrders.sumOf { it.totalAmount }
        val liveCount = orders.count { it.status == OrderStatus.RUNNING.key }
        val pendingCount = orders.count { it.status == OrderStatus.PENDING.key }

        val zomato = todayOrders.filter { it.channel == OrderChannel.ZOMATO.key }
        val swiggy = todayOrders.filter { it.channel == OrderChannel.SWIGGY.key }
        val otherApp = todayOrders.filter { it.channel == OrderChannel.OTHER_APP.key }
        val ownDelivery = todayOrders.filter { it.channel == OrderChannel.OWN_DELIVERY.key }
        val table = todayOrders.filter { it.channel == OrderChannel.TABLE.key }
        val pickup = todayOrders.filter { it.channel == OrderChannel.PICKUP.key }
        val online = todayOrders.filter { it.channel == OrderChannel.ONLINE.key }

        val thirdPartySales = zomato.sumOf { it.totalAmount } +
            swiggy.sumOf { it.totalAmount } +
            otherApp.sumOf { it.totalAmount }

        val dailyRent = profile.monthlyRent / 30.0
        val dailyElec = profile.monthlyElectricity / 30.0

        val todayExpenses = expenses.filter { it.dateStr == todayStr }
        val gasMaterial = todayExpenses.filter { it.category == ExpenseCategory.MATERIAL_GAS.key }.sumOf { it.amount }
        val expiryLoss = todayExpenses.filter { it.category == ExpenseCategory.EXPIRY_LOSS.key }.sumOf { it.amount }
        val loggedFood = todayExpenses.filter { it.category == ExpenseCategory.STAFF_FOOD.key }.sumOf { it.amount }
        val loggedSalary = todayExpenses.filter { it.category == ExpenseCategory.STAFF_SALARY.key }.sumOf { it.amount }
        val otherExp = todayExpenses.filter {
            it.category in listOf(
                ExpenseCategory.OTHER.key,
                ExpenseCategory.ELECTRICITY.key,
                ExpenseCategory.SHOP_RENT.key
            )
        }.sumOf { it.amount }

        val todayAttendance = attendance.filter { it.dateStr == todayStr }
        val rosterDailyWage = if (todayAttendance.isNotEmpty()) {
            todayAttendance.sumOf { att ->
                val member = staff.firstOrNull { it.id == att.staffId }
                val perDay = (member?.monthlySalary ?: 0.0) / 30.0
                when (att.status) {
                    AttendanceStatus.PRESENT.key -> perDay
                    AttendanceStatus.HALF_DAY.key -> perDay * 0.5
                    else -> 0.0
                }
            }
        } else {
            staff.filter { it.active }.sumOf { it.monthlySalary / 30.0 }
        }
        val staffFood = profile.dailyStaffFoodBudget + loggedFood
        val dailyLabor = rosterDailyWage + loggedSalary

        val totalCost = dailyLabor + dailyRent + dailyElec + staffFood + gasMaterial + expiryLoss + otherExp
        val netPnl = todaySales - totalCost

        val tipsToday = todayOrders.sumOf { it.tipAmount } + todayAttendance.sumOf { it.tipEarned }
        val monthAttTips = attendance.filter { it.dateStr.startsWith(monthPrefix) }.sumOf { it.tipEarned }
        val tipsMonth = monthOrders.sumOf { it.tipAmount } + monthAttTips

        val targetProg = if (profile.dailyTarget > 0) (todaySales / profile.dailyTarget).toFloat().coerceIn(0f, 1.5f) else 0f

        return DashboardMetrics(
            todaySales = todaySales,
            todayOrdersCount = todayOrders.size,
            liveOrdersCount = liveCount,
            pendingOrdersCount = pendingCount,
            zomatoCount = zomato.size,
            swiggyCount = swiggy.size,
            otherAppCount = otherApp.size,
            thirdPartySales = thirdPartySales,
            ownDeliveryCount = ownDelivery.size,
            ownDeliverySales = ownDelivery.sumOf { it.totalAmount },
            tableCount = table.size,
            pickupCount = pickup.size,
            onlineCount = online.size,
            dailyRentCost = dailyRent,
            dailyElectricityCost = dailyElec,
            dailyStaffFoodCost = staffFood,
            dailyLaborCost = dailyLabor,
            todayGasAndMaterialCost = gasMaterial,
            todayExpiryLoss = expiryLoss,
            todayOtherExpenses = otherExp,
            todayTotalCost = totalCost,
            todayNetPnl = netPnl,
            tipsToday = tipsToday,
            tipsMonth = tipsMonth,
            targetProgress = targetProg
        )
    }

    private data class Quint<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    private companion object {
        const val TAG = "MaraManagerVM"
    }
}
