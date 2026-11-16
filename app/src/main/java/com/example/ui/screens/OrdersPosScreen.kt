package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.MenuItem
import com.example.data.model.OrderChannel
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.RestaurantProfile
import com.example.ui.theme.LossCoral
import com.example.ui.theme.ProfitEmerald
import com.example.util.AppStrings
import com.example.util.PrintAndShareHelper
import com.example.viewmodel.MaraManagerViewModel
import com.example.viewmodel.MaraWorkspaceData
import java.util.Locale

@Composable
fun OrdersPosScreen(
    data: MaraWorkspaceData,
    strings: AppStrings,
    viewModel: MaraManagerViewModel
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var channelFilter by remember { mutableStateOf<String?>(null) }
    var showNewOrderDialog by remember { mutableStateOf(false) }
    var qrPaymentOrder by remember { mutableStateOf<OrderRecord?>(null) }

    val statusTabs = listOf(
        strings.liveOrders to OrderStatus.RUNNING.key,
        strings.pendingOrders to OrderStatus.PENDING.key,
        strings.completedOrders to OrderStatus.COMPLETED.key
    )

    val filteredOrders = data.orders.filter { ord ->
        ord.status == statusTabs[selectedTab].second &&
            (channelFilter == null || ord.channel == channelFilter)
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewOrderDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("new_order_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = strings.quickNewOrder, tint = Color.White)
            }
        }
    ) { innerPad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPad.calculateBottomPadding())
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                statusTabs.forEachIndexed { idx, (label, key) ->
                    val count = data.orders.count { it.status == key }
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text("$label ($count)", maxLines = 1) }
                    )
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = channelFilter == null,
                        onClick = { channelFilter = null },
                        label = { Text("All") }
                    )
                }
                items(OrderChannel.entries) { ch ->
                    FilterChip(
                        selected = channelFilter == ch.key,
                        onClick = { channelFilter = if (channelFilter == ch.key) null else ch.key },
                        label = { Text(strings.channelLabel(ch)) }
                    )
                }
            }

            if (filteredOrders.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Rounded.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No ${statusTabs[selectedTab].first} yet",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap + to create a Table, Own Delivery, Pickup, Online, Zomato, or Swiggy order with Auto-KOT & GST.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredOrders, key = { it.id }) { order ->
                        OrderCard(
                            order = order,
                            nowMillis = data.nowMillis,
                            profile = data.profile,
                            strings = strings,
                            onPrintKot = {
                                PrintAndShareHelper.printKotHtml(context, data.profile, order)
                                viewModel.markKotPrinted(order.id)
                            },
                            onPrintBill = {
                                PrintAndShareHelper.printCustomerBillHtml(context, data.profile, order)
                                viewModel.markBillPrinted(order.id)
                            },
                            onToggleGst = { viewModel.toggleOrderGst(order, data.profile.gstPercent) },
                            onStartRunning = { viewModel.advanceOrderStatus(order, OrderStatus.RUNNING) },
                            onComplete = { viewModel.advanceOrderStatus(order, OrderStatus.COMPLETED) },
                            onShowQrPay = { qrPaymentOrder = order },
                            onShareWhatsApp = {
                                val text = "*${data.profile.restaurantName}* Bill #${order.orderNumber}\n" +
                                    "Items: ${order.itemsSummary}\n" +
                                    "Total: ₹${order.totalAmount.toInt()} (${order.paymentMethod})"
                                PrintAndShareHelper.shareToWhatsAppOrChooser(context, text)
                            },
                            onDelete = { viewModel.deleteOrder(order.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (showNewOrderDialog) {
        NewOrderDialog(
            profile = data.profile,
            menuItems = data.menuItems.filter { it.available },
            strings = strings,
            onDismiss = { showNewOrderDialog = false },
            onCreateOrder = { newOrder ->
                showNewOrderDialog = false
                viewModel.createOrder(newOrder) { created ->
                    if (data.profile.autoKotEnabled) {
                        PrintAndShareHelper.printKotHtml(context, data.profile, created)
                        viewModel.markKotPrinted(created.id)
                    }
                }
            }
        )
    }

    if (qrPaymentOrder != null) {
        val ord = qrPaymentOrder!!
        val payable = ord.totalAmount + ord.tipAmount
        val upiUri = "upi://pay?pa=${data.profile.upiId}&pn=${data.profile.restaurantName}&am=${
            String.format(Locale.US, "%.2f", payable)
        }&cu=INR&tn=${ord.orderNumber}"
        val qrBmp = remember(upiUri) { PrintAndShareHelper.generateQrBitmap(upiUri, 512) }

        AlertDialog(
            onDismissRequest = { qrPaymentOrder = null },
            title = { Text("UPI QR • ${ord.orderNumber}") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Payable: ${formatCurrency(payable)}", style = MaterialTheme.typography.titleLarge, color = ProfitEmerald)
                    if (qrBmp != null) {
                        Image(
                            bitmap = qrBmp.asImageBitmap(),
                            contentDescription = "UPI Payment QR",
                            modifier = Modifier.size(220.dp)
                        )
                    }
                    Text("UPI ID: ${data.profile.upiId}", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                Button(onClick = { qrPaymentOrder = null }) { Text("Done") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderCard(
    order: OrderRecord,
    nowMillis: Long,
    profile: RestaurantProfile,
    strings: AppStrings,
    onPrintKot: () -> Unit,
    onPrintBill: () -> Unit,
    onToggleGst: () -> Unit,
    onStartRunning: () -> Unit,
    onComplete: () -> Unit,
    onShowQrPay: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onDelete: () -> Unit
) {
    val channel = OrderChannel.fromKey(order.channel)
    val payment = PaymentMethod.fromKey(order.paymentMethod)
    val elapsedSec = ((nowMillis - order.createdAtMillis) / 1000L).coerceAtLeast(0L)
    val timerStr = String.format(Locale.US, "%02d:%02d", elapsedSec / 60L, elapsedSec % 60L)
    val isLocked = order.status == OrderStatus.COMPLETED.key || order.status == OrderStatus.CANCELLED.key

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${order.orderNumber} • ${strings.channelLabel(channel)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (order.tableOrRef.isNotBlank() || order.customerName.isNotBlank()) {
                        Text(
                            text = listOf(order.tableOrRef, order.customerName).filter { it.isNotBlank() }.joinToString(" • "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!isLocked) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "⏱ $timerStr",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Text(
                text = order.itemsSummary,
                style = MaterialTheme.typography.bodyLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Subtotal: ${formatCurrency(order.subtotal)} | ${
                            if (order.gstEnabled) "${strings.gstApplied} (${order.gstPercent.toInt()}%): ${formatCurrency(order.gstAmount)}"
                            else strings.gstCancelled
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Total: ${formatCurrency(order.totalAmount + order.tipAmount)} • ${strings.paymentLabel(payment)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = ProfitEmerald,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!isLocked) {
                    TextButton(onClick = onToggleGst) {
                        Text(if (order.gstEnabled) "Cancel GST" else "Add GST")
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onPrintKot) {
                    Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (order.kotPrinted) "KOT ✓" else strings.printKot)
                }
                OutlinedButton(onClick = onPrintBill) {
                    Icon(Icons.Rounded.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (order.billPrinted) "Bill ✓" else strings.printBill)
                }
                OutlinedButton(onClick = onShowQrPay) {
                    Icon(Icons.Rounded.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("QR Pay")
                }
                IconButton(onClick = onShareWhatsApp) {
                    Icon(Icons.Rounded.Share, contentDescription = "Share Bill")
                }
                if (order.status == OrderStatus.PENDING.key) {
                    Button(onClick = onStartRunning) {
                        Text("Start Live")
                    }
                }
                if (order.status == OrderStatus.RUNNING.key) {
                    Button(onClick = onComplete) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Complete")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Order", tint = LossCoral)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewOrderDialog(
    profile: RestaurantProfile,
    menuItems: List<MenuItem>,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onCreateOrder: (OrderRecord) -> Unit
) {
    var selectedChannel by remember { mutableStateOf(OrderChannel.TABLE) }
    var tableOrRef by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var customItemsText by remember { mutableStateOf("") }
    var manualSubtotalText by remember { mutableStateOf("") }
    var tipText by remember { mutableStateOf("0") }
    var gstEnabled by remember { mutableStateOf(true) }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var startAsPending by remember { mutableStateOf(false) }

    val selectedQuantities = remember { mutableStateMapOf<String, Int>() }
    val menuSubtotal = menuItems.sumOf { item -> (selectedQuantities[item.id] ?: 0) * item.price }
    val subtotal = if (menuSubtotal > 0.0) menuSubtotal else (manualSubtotalText.toDoubleOrNull() ?: 0.0)
    val gstAmount = if (gstEnabled) subtotal * profile.gstPercent / 100.0 else 0.0
    val total = subtotal + gstAmount
    val tip = tipText.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.quickNewOrder) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Order Channel", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OrderChannel.entries.forEach { ch ->
                        FilterChip(
                            selected = selectedChannel == ch,
                            onClick = { selectedChannel = ch },
                            label = { Text(strings.channelLabel(ch)) }
                        )
                    }
                }

                OutlinedTextField(
                    value = tableOrRef,
                    onValueChange = { tableOrRef = it },
                    label = { Text("Table # / Zomato-Swiggy Ref") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer Name (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (menuItems.isNotEmpty()) {
                    Text("Tap Menu Items", style = MaterialTheme.typography.labelLarge)
                    menuItems.take(8).forEach { dish ->
                        val qty = selectedQuantities[dish.id] ?: 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${dish.name} (₹${dish.price.toInt()})", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { if (qty > 0) selectedQuantities[dish.id] = qty - 1 }) { Text("-") }
                                Text(qty.toString(), fontWeight = FontWeight.Bold)
                                TextButton(onClick = { selectedQuantities[dish.id] = qty + 1 }) { Text("+") }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = customItemsText,
                    onValueChange = { customItemsText = it },
                    label = { Text("Items Note (or custom items)") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (menuSubtotal <= 0.0) {
                    OutlinedTextField(
                        value = manualSubtotalText,
                        onValueChange = { manualSubtotalText = it },
                        label = { Text("Subtotal Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = tipText,
                    onValueChange = { tipText = it },
                    label = { Text("Tip Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Apply GST (${profile.gstPercent}%)")
                    Switch(checked = gstEnabled, onCheckedChange = { gstEnabled = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Save as Pending (Queue)")
                    Switch(checked = startAsPending, onCheckedChange = { startAsPending = it })
                }

                Text("Payment Method", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PaymentMethod.entries.forEach { pm ->
                        FilterChip(
                            selected = paymentMethod == pm,
                            onClick = { paymentMethod = pm },
                            label = { Text(strings.paymentLabel(pm)) }
                        )
                    }
                }

                Text(
                    text = "Total: ${formatCurrency(total)} (+ Tip ${formatCurrency(tip)})",
                    style = MaterialTheme.typography.titleMedium,
                    color = ProfitEmerald
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pickedSummary = selectedQuantities.entries
                        .filter { it.value > 0 }
                        .mapNotNull { (id, qty) ->
                            val m = menuItems.firstOrNull { it.id == id }
                            m?.let { "${qty}x ${it.name}" }
                        }
                    val summary = (pickedSummary + listOf(customItemsText).filter { it.isNotBlank() })
                        .joinToString(", ")
                        .ifBlank { "Restaurant Order" }
                    val itemCount = selectedQuantities.values.sum().coerceAtLeast(1).toLong()
                    val order = OrderRecord(
                        orderNumber = "ORD-${(System.currentTimeMillis() % 10000).toString().padStart(4, '0')}",
                        channel = selectedChannel.key,
                        tableOrRef = tableOrRef,
                        customerName = customerName,
                        itemsSummary = summary,
                        itemsCount = itemCount,
                        subtotal = subtotal,
                        gstEnabled = gstEnabled,
                        gstPercent = profile.gstPercent,
                        gstAmount = gstAmount,
                        totalAmount = total,
                        tipAmount = tip,
                        paymentMethod = paymentMethod.key,
                        status = if (startAsPending) OrderStatus.PENDING.key else OrderStatus.RUNNING.key,
                        createdAtMillis = System.currentTimeMillis()
                    )
                    onCreateOrder(order)
                },
                enabled = subtotal > 0.0 || customItemsText.isNotBlank()
            ) {
                Text(if (profile.autoKotEnabled) "Create & Auto KOT" else "Create Order")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
