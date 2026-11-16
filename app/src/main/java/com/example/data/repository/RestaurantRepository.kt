package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AttendanceRecord
import com.example.data.model.DailyExpense
import com.example.data.model.InventoryItem
import com.example.data.model.MenuItem
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.PartyBooking
import com.example.data.model.RestaurantProfile
import com.example.data.model.StaffMember
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class RestaurantRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private inline fun <T> Flow<T>.withFirestoreLogging(
        op: OperationType,
        path: String,
        uid: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val toLog = firestoreError ?: (error as? Exception)
        if (toLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(toLog, op, path)
        }
        throw error
    }

    // --- Restaurant Profile ---

    fun observeProfile(uid: String): Flow<RestaurantProfile> {
        val docRef = userDoc(uid)
        return docRef.snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    RestaurantProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: RestaurantProfile(userId = uid)
            }
            .withFirestoreLogging(OperationType.GET, docRef.path, uid)
    }

    suspend fun ensureProfileInitialized(defaultName: String = "Mara Spice Kitchen"): Result<Unit> =
        runCatching {
            val uid = requireUserId()
            val docRef = userDoc(uid)
            try {
                val snap = docRef.get().await()
                if (!snap.exists()) {
                    val profile = RestaurantProfile(userId = uid, restaurantName = defaultName)
                    docRef.set(profile.toCreateMap(uid)).await()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, docRef.path)
                throw e
            }
        }

    suspend fun saveProfile(profile: RestaurantProfile): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid)
        try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(profile.toUpdateMap()).await()
            } else {
                docRef.set(profile.toCreateMap(uid)).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Menu Items ---

    fun observeMenuItems(uid: String): Flow<List<MenuItem>> {
        val col = userDoc(uid).collection("menu_items")
        return col.snapshots()
            .map { snap ->
                snap.toObjects(
                    MenuItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.category }
            }
            .withFirestoreLogging(OperationType.LIST, col.path, uid)
    }

    suspend fun addMenuItem(item: MenuItem): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("menu_items").document()
        try {
            docRef.set(item.toCreateMap()).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun addMenuItemsBatch(items: List<MenuItem>): Result<Int> = runCatching {
        val uid = requireUserId()
        val col = userDoc(uid).collection("menu_items")
        val batch = db.batch()
        items.forEach { item ->
            val ref = col.document()
            batch.set(ref, item.toCreateMap())
        }
        try {
            batch.commit().await()
            items.size
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, col.path)
            throw e
        }
    }

    suspend fun updateMenuItem(item: MenuItem): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(item.id.isNotBlank()) { "MenuItem ID is required for update" }
        val docRef = userDoc(uid).collection("menu_items").document(item.id)
        try {
            docRef.update(item.toUpdateMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteMenuItem(itemId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("menu_items").document(itemId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Orders ---

    fun observeOrders(uid: String): Flow<List<OrderRecord>> {
        val col = userDoc(uid).collection("orders")
        return col.snapshots()
            .map { snap ->
                snap.toObjects(
                    OrderRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAtMillis }
            }
            .withFirestoreLogging(OperationType.LIST, col.path, uid)
    }

    suspend fun createOrder(order: OrderRecord): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("orders").document()
        try {
            docRef.set(order.toCreateMap()).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus): Result<Unit> =
        runCatching {
            val uid = requireUserId()
            val docRef = userDoc(uid).collection("orders").document(orderId)
            try {
                docRef.update(
                    mapOf(
                        "status" to newStatus.key,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                throw e
            }
        }

    suspend fun updateOrderBilling(
        orderId: String,
        gstEnabled: Boolean,
        gstPercent: Double,
        gstAmount: Double,
        totalAmount: Double,
        tipAmount: Double,
        paymentMethod: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("orders").document(orderId)
        try {
            docRef.update(
                mapOf(
                    "gstEnabled" to gstEnabled,
                    "gstPercent" to gstPercent.coerceIn(0.0, 30.0),
                    "gstAmount" to gstAmount.coerceAtLeast(0.0),
                    "totalAmount" to totalAmount.coerceAtLeast(0.0),
                    "tipAmount" to tipAmount.coerceAtLeast(0.0),
                    "paymentMethod" to paymentMethod,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun markOrderPrinted(orderId: String, kot: Boolean = false, bill: Boolean = false): Result<Unit> =
        runCatching {
            val uid = requireUserId()
            val docRef = userDoc(uid).collection("orders").document(orderId)
            val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            if (kot) updates["kotPrinted"] = true
            if (bill) updates["billPrinted"] = true
            try {
                docRef.update(updates).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                throw e
            }
        }

    suspend fun deleteOrder(orderId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("orders").document(orderId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Staff ---

    fun observeStaff(uid: String): Flow<List<StaffMember>> {
        val col = userDoc(uid).collection("staff")
        return col.snapshots()
            .map { snap ->
                snap.toObjects(
                    StaffMember::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.name }
            }
            .withFirestoreLogging(OperationType.LIST, col.path, uid)
    }

    suspend fun addStaffMember(member: StaffMember): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("staff").document()
        try {
            docRef.set(member.toCreateMap()).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteStaffMember(staffId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("staff").document(staffId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Attendance ---

    fun observeAttendance(uid: String): Flow<List<AttendanceRecord>> {
        val col = userDoc(uid).collection("attendance")
        return col.snapshots()
            .map { snap ->
                snap.toObjects(
                    AttendanceRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.dateStr + it.checkInTimeStr }
            }
            .withFirestoreLogging(OperationType.LIST, col.path, uid)
    }

    suspend fun recordAttendance(record: AttendanceRecord): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("attendance").document()
        try {
            docRef.set(record.toCreateMap()).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateAttendanceCheckOut(
        attendanceId: String,
        checkOutTimeStr: String,
        tipEarned: Double
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("attendance").document(attendanceId)
        try {
            docRef.update(
                mapOf(
                    "checkOutTimeStr" to checkOutTimeStr.take(30),
                    "tipEarned" to tipEarned.coerceAtLeast(0.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Inventory ---

    fun observeInventory(uid: String): Flow<List<InventoryItem>> {
        val col = userDoc(uid).collection("inventory")
        return col.snapshots()
            .map { snap ->
                snap.toObjects(
                    InventoryItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.name }
            }
            .withFirestoreLogging(OperationType.LIST, col.path, uid)
    }

    suspend fun addInventoryItem(item: InventoryItem): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("inventory").document()
        try {
            docRef.set(item.toCreateMap()).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateInventoryQuantity(itemId: String, newQuantity: Double): Result<Unit> =
        runCatching {
            val uid = requireUserId()
            val docRef = userDoc(uid).collection("inventory").document(itemId)
            try {
                docRef.update(
                    mapOf(
                        "quantity" to newQuantity.coerceAtLeast(0.0),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                throw e
            }
        }

    suspend fun deleteInventoryItem(itemId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("inventory").document(itemId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Daily Expenses ---

    fun observeExpenses(uid: String): Flow<List<DailyExpense>> {
        val col = userDoc(uid).collection("expenses")
        return col.snapshots()
            .map { snap ->
                snap.toObjects(
                    DailyExpense::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.dateStr }
            }
            .withFirestoreLogging(OperationType.LIST, col.path, uid)
    }

    suspend fun addExpense(expense: DailyExpense): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("expenses").document()
        try {
            docRef.set(expense.toCreateMap()).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("expenses").document(expenseId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Party Bookings ---

    fun observePartyBookings(uid: String): Flow<List<PartyBooking>> {
        val col = userDoc(uid).collection("party_bookings")
        return col.snapshots()
            .map { snap ->
                snap.toObjects(
                    PartyBooking::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.eventDate }
            }
            .withFirestoreLogging(OperationType.LIST, col.path, uid)
    }

    suspend fun addPartyBooking(booking: PartyBooking): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("party_bookings").document()
        try {
            docRef.set(booking.toCreateMap()).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updatePartyBooking(booking: PartyBooking): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(booking.id.isNotBlank()) { "PartyBooking ID is required for update" }
        val docRef = userDoc(uid).collection("party_bookings").document(booking.id)
        try {
            docRef.update(
                mapOf(
                    "partyName" to booking.partyName.take(100),
                    "partyPhone" to booking.partyPhone.take(30),
                    "eventDate" to booking.eventDate.take(40),
                    "guestCount" to booking.guestCount.coerceIn(1L, 10000L),
                    "totalBudget" to booking.totalBudget.coerceAtLeast(0.0),
                    "advancePaid" to booking.advancePaid.coerceAtLeast(0.0),
                    "inventoryUsedSummary" to booking.inventoryUsedSummary.take(1000),
                    "chefRequirements" to booking.chefRequirements.take(1000),
                    "status" to booking.status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deletePartyBooking(bookingId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("party_bookings").document(bookingId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
