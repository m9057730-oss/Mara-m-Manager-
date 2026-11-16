package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AttendanceRecord
import com.example.data.model.DailyExpense
import com.example.data.model.InventoryCategory
import com.example.data.model.InventoryItem
import com.example.data.model.MenuItem
import com.example.data.model.OrderChannel
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.PartyBooking
import com.example.data.model.PaymentMethod
import com.example.data.model.StaffMember
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class RestaurantRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ownerProfileAndCrudOperations_succeedAgainstRules() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = RestaurantRepository(firestore)

        val profileRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.ensureProfileInitialized("Royal Dhaba") }
        assertTrue(profileRes.isSuccess)

        val menuRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addMenuItem(
                MenuItem(
                    name = "Butter Chicken ${UUID.randomUUID().toString().take(4)}",
                    category = "Main Course",
                    price = 420.0,
                    costPrice = 160.0,
                    veg = false,
                    available = true,
                    prepMinutes = 20L
                )
            )
        }
        assertTrue(menuRes.isSuccess)

        val orderRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createOrder(
                OrderRecord(
                    orderNumber = "KOT-${UUID.randomUUID().toString().take(4)}",
                    channel = OrderChannel.ZOMATO.key,
                    tableOrRef = "ZOM-102",
                    itemsSummary = "1x Butter Chicken",
                    itemsCount = 1L,
                    subtotal = 420.0,
                    gstEnabled = true,
                    gstPercent = 5.0,
                    gstAmount = 21.0,
                    totalAmount = 441.0,
                    tipAmount = 20.0,
                    paymentMethod = PaymentMethod.ONLINE_PREPAID.key,
                    status = OrderStatus.RUNNING.key
                )
            )
        }
        assertTrue(orderRes.isSuccess)
        val orderId = orderRes.getOrThrow()

        val statusRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.updateOrderStatus(orderId, OrderStatus.COMPLETED)
        }
        assertTrue(statusRes.isSuccess)

        val staffId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addStaffMember(StaffMember(name = "Harpreet", role = "Chef", monthlySalary = 22000.0)).getOrThrow()
        }
        assertTrue(
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repo.recordAttendance(
                    AttendanceRecord(
                        staffId = staffId,
                        staffName = "Harpreet",
                        role = "Chef",
                        dateStr = "2026-11-16",
                        checkInTimeStr = "09:00 AM",
                        status = "PRESENT",
                        tipEarned = 150.0
                    )
                )
            }.isSuccess
        )

        assertTrue(
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repo.addInventoryItem(
                    InventoryItem(
                        name = "Pepsi 500ml",
                        category = InventoryCategory.COLD_DRINKS.key,
                        quantity = 24.0,
                        unit = "bottles",
                        minThreshold = 10.0,
                        costPerUnit = 28.0
                    )
                )
            }.isSuccess
        )

        assertTrue(
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repo.addExpense(
                    DailyExpense(
                        title = "Gas Cylinder",
                        category = "MATERIAL_GAS",
                        amount = 1900.0,
                        dateStr = "2026-11-16"
                    )
                )
            }.isSuccess
        )

        assertTrue(
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repo.addPartyBooking(
                    PartyBooking(
                        partyName = "Gill Birthday",
                        partyPhone = "9812345678",
                        eventDate = "2026-11-22",
                        guestCount = 40L,
                        totalBudget = 24000.0,
                        advancePaid = 5000.0,
                        inventoryUsedSummary = "Cold drinks 2 crates",
                        chefRequirements = "Paneer 8kg, Coal 20kg"
                    )
                )
            }.isSuccess
        )

        val orders = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeOrders(uid).first { it.any { o -> o.id == orderId } }
        }
        assertTrue(orders.any { it.id == orderId })
    }

    @Test
    fun observeOrders_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = RestaurantRepository(firestore)
        var captured: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeOrders("some_uid_123").first()
            }
        } catch (t: Throwable) {
            captured = t
        }
        val fsEx = checkNotNull(captured?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $captured"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
    }

    @Test
    fun observeOrders_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        signInTestUser(BOB_EMAIL)
        val repo = RestaurantRepository(firestore)
        var captured: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeOrders(aliceUid).first()
            }
        } catch (t: Throwable) {
            captured = t
        }
        val fsEx = checkNotNull(captured?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $captured"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_mara@test.com"
        const val BOB_EMAIL = "bob_mara@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
