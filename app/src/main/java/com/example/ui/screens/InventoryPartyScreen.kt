package com.example.ui.screens

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
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.InventoryCategory
import com.example.data.model.InventoryItem
import com.example.data.model.PartyBooking
import com.example.data.model.PartyStatus
import com.example.ui.theme.LossCoral
import com.example.ui.theme.ProfitEmerald
import com.example.util.AppStrings
import com.example.util.PrintAndShareHelper
import com.example.viewmodel.MaraManagerViewModel
import com.example.viewmodel.MaraWorkspaceData
import java.util.Locale

@Composable
fun InventoryPartyScreen(
    data: MaraWorkspaceData,
    strings: AppStrings,
    viewModel: MaraManagerViewModel
) {
    val context = LocalContext.current
    val lang = AppLanguage.fromCode(data.profile.language)
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedInvCat by remember { mutableStateOf<String?>(null) }
    var lowStockOnly by remember { mutableStateOf(false) }
    var showAddInvDialog by remember { mutableStateOf(false) }
    var showAddPartyDialog by remember { mutableStateOf(false) }
    var editingParty by remember { mutableStateOf<PartyBooking?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (selectedTab == 0) showAddInvDialog = true else showAddPartyDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("stock_party_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add", tint = Color.White)
            }
        }
    ) { innerPad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPad.calculateBottomPadding())
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("${strings.inventoryTab} (${data.inventory.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("${strings.partyBookTab} (${data.partyBookings.size})") }
                )
            }

            if (selectedTab == 0) {
                // WhatsApp Full Inventory Report Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val report = PrintAndShareHelper.formatInventoryWhatsAppReport(
                                data.profile.restaurantName,
                                data.inventory
                            )
                            PrintAndShareHelper.shareToWhatsAppOrChooser(context, report)
                        },
                        modifier = Modifier.weight(1f).testTag("whatsapp_inventory_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.sendWhatsAppReport, maxLines = 1)
                    }
                    FilterChip(
                        selected = lowStockOnly,
                        onClick = { lowStockOnly = !lowStockOnly },
                        label = { Text("Alerts (${data.lowStockItems.size})") },
                        leadingIcon = {
                            Icon(Icons.Rounded.WarningAmber, contentDescription = null, modifier = Modifier.size(16.dp), tint = LossCoral)
                        }
                    )
                }

                // Category Filters: Kitchen, Packing, Tandoor Coal, Cold Drinks (Coke/Pepsi)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedInvCat == null,
                            onClick = { selectedInvCat = null },
                            label = { Text("All") }
                        )
                    }
                    items(InventoryCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedInvCat == cat.key,
                            onClick = { selectedInvCat = if (selectedInvCat == cat.key) null else cat.key },
                            label = { Text(strings.inventoryCatLabel(cat, lang)) }
                        )
                    }
                }

                val filtered = data.inventory.filter { item ->
                    (selectedInvCat == null || item.category == selectedInvCat) &&
                        (!lowStockOnly || item.isLowStock)
                }

                if (filtered.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.Inventory2, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        Spacer(Modifier.height(10.dp))
                        Text("No inventory items here", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Add Kitchen Items, Packing Material, Tandoor Coal, or Cold Drinks (Coke/Pepsi) and get instant Low-Stock alerts & WhatsApp reports.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.id }) { item ->
                            InventoryItemCard(
                                item = item,
                                categoryLabel = strings.inventoryCatLabel(InventoryCategory.fromKey(item.category), lang),
                                onMinus = { viewModel.adjustInventoryQuantity(item, -1.0) },
                                onPlus = { viewModel.adjustInventoryQuantity(item, 1.0) },
                                onDelete = { viewModel.deleteInventoryItem(item.id) }
                            )
                        }
                        item { Spacer(Modifier.height(72.dp)) }
                    }
                }
            } else {
                // Party Booking System
                if (data.partyBookings.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.Celebration, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
                        Spacer(Modifier.height(10.dp))
                        Text("Party Book System", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Tap + to book a party with Party Name, Phone Number, Inventory Used, and Chef Inventory Requirements (shareable to WhatsApp).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(data.partyBookings, key = { it.id }) { booking ->
                            PartyBookingCard(
                                booking = booking,
                                strings = strings,
                                onEdit = { editingParty = booking },
                                onShareWhatsApp = {
                                    val report = PrintAndShareHelper.formatPartyChefWhatsAppReport(
                                        data.profile.restaurantName,
                                        booking
                                    )
                                    PrintAndShareHelper.shareToWhatsAppOrChooser(context, report)
                                },
                                onDelete = { viewModel.deletePartyBooking(booking.id) }
                            )
                        }
                        item { Spacer(Modifier.height(72.dp)) }
                    }
                }
            }
        }
    }

    if (showAddInvDialog) {
        AddInventoryDialog(
            lang = lang,
            strings = strings,
            onDismiss = { showAddInvDialog = false },
            onAdd = { item ->
                showAddInvDialog = false
                viewModel.addInventoryItem(item)
            }
        )
    }

    if (showAddPartyDialog || editingParty != null) {
        PartyBookingDialog(
            existing = editingParty,
            strings = strings,
            onDismiss = {
                showAddPartyDialog = false
                editingParty = null
            },
            onSave = { booking ->
                if (editingParty != null) {
                    viewModel.updatePartyBooking(booking)
                } else {
                    viewModel.addPartyBooking(booking)
                }
                showAddPartyDialog = false
                editingParty = null
            }
        )
    }
}

@Composable
private fun InventoryItemCard(
    item: InventoryItem,
    categoryLabel: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onDelete: () -> Unit
) {
    val qtyText = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString()
    else String.format(Locale.US, "%.1f", item.quantity)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isLowStock) Color(0xFFFFF1F1)
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (item.isLowStock) {
                        Surface(color = LossCoral, shape = RoundedCornerShape(50)) {
                            Text(
                                text = if (item.isOutOfStock) "OUT" else "LOW",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(categoryLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Min Alert: ${item.minThreshold.toInt()} ${item.unit}" +
                        (if (item.expiryDate.isNotBlank()) " • Exp: ${item.expiryDate}" else ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onMinus, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("-") }
                Text(
                    text = "$qtyText ${item.unit}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = if (item.isLowStock) LossCoral else ProfitEmerald
                )
                OutlinedButton(onClick = onPlus, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("+") }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = LossCoral)
                }
            }
        }
    }
}

@Composable
private fun PartyBookingCard(
    booking: PartyBooking,
    strings: AppStrings,
    onEdit: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(booking.partyName, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "📞 ${booking.partyPhone.ifBlank { "No phone" }} • 📅 ${booking.eventDate} • 👥 ${booking.guestCount} Guests",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(50)) {
                    Text(booking.status, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
            Text(
                text = "Budget: ${formatCurrency(booking.totalBudget)} • Advance: ${formatCurrency(booking.advancePaid)}",
                style = MaterialTheme.typography.labelLarge,
                color = ProfitEmerald
            )
            if (booking.inventoryUsedSummary.isNotBlank()) {
                Text("${strings.inventoryUsed}: ${booking.inventoryUsedSummary}", style = MaterialTheme.typography.bodyMedium)
            }
            if (booking.chefRequirements.isNotBlank()) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp)) {
                    Text(
                        text = "👨‍🍳 ${strings.chefRequirement}:\n${booking.chefRequirements}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(10.dp).fillMaxWidth()
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onShareWhatsApp,
                    colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.sendChefReqWhatsApp, maxLines = 1)
                }
                OutlinedButton(onClick = onEdit) { Text("Update") }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = LossCoral)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddInventoryDialog(
    lang: AppLanguage,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onAdd: (InventoryItem) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(InventoryCategory.KITCHEN) }
    var qty by remember { mutableStateOf("10") }
    var unit by remember { mutableStateOf("kg") }
    var minThreshold by remember { mutableStateOf("5") }
    var costPerUnit by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addInventory) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Category", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    InventoryCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                category = cat
                                unit = when (cat) {
                                    InventoryCategory.TANDOOR_COAL -> "kg"
                                    InventoryCategory.COLD_DRINKS -> "bottles"
                                    InventoryCategory.PACKING -> "pcs"
                                    InventoryCategory.KITCHEN -> "kg"
                                }
                            },
                            label = { Text(strings.inventoryCatLabel(cat, lang)) }
                        )
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Name (e.g. Coke 500ml, Coal, Foil)") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qty,
                        onValueChange = { qty = it },
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minThreshold,
                        onValueChange = { minThreshold = it },
                        label = { Text("Min Alert") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = costPerUnit,
                        onValueChange = { costPerUnit = it },
                        label = { Text("Cost/Unit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(value = expiryDate, onValueChange = { expiryDate = it }, label = { Text("Expiry Date (Optional)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(
                        InventoryItem(
                            name = name.trim(),
                            category = category.key,
                            quantity = qty.toDoubleOrNull() ?: 0.0,
                            unit = unit.ifBlank { "pcs" },
                            minThreshold = minThreshold.toDoubleOrNull() ?: 5.0,
                            costPerUnit = costPerUnit.toDoubleOrNull() ?: 0.0,
                            expiryDate = expiryDate.trim()
                        )
                    )
                },
                enabled = name.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PartyBookingDialog(
    existing: PartyBooking?,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (PartyBooking) -> Unit
) {
    var partyName by remember { mutableStateOf(existing?.partyName ?: "") }
    var partyPhone by remember { mutableStateOf(existing?.partyPhone ?: "") }
    var eventDate by remember { mutableStateOf(existing?.eventDate ?: "2026-11-25") }
    var guests by remember { mutableStateOf((existing?.guestCount ?: 30L).toString()) }
    var budget by remember { mutableStateOf((existing?.totalBudget ?: 25000.0).toInt().toString()) }
    var advance by remember { mutableStateOf((existing?.advancePaid ?: 5000.0).toInt().toString()) }
    var inventoryUsed by remember { mutableStateOf(existing?.inventoryUsedSummary ?: "") }
    var chefReq by remember { mutableStateOf(existing?.chefRequirements ?: "") }
    var status by remember { mutableStateOf(existing?.status ?: PartyStatus.BOOKED.key) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) strings.addPartyBooking else "Update Party Booking") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = partyName, onValueChange = { partyName = it }, label = { Text(strings.partyName) }, singleLine = true)
                OutlinedTextField(
                    value = partyPhone,
                    onValueChange = { partyPhone = it },
                    label = { Text(strings.partyPhone) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = eventDate, onValueChange = { eventDate = it }, label = { Text("Event Date") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(
                        value = guests,
                        onValueChange = { guests = it },
                        label = { Text("Guests") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = budget,
                        onValueChange = { budget = it },
                        label = { Text("Total Budget") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = advance,
                        onValueChange = { advance = it },
                        label = { Text("Advance") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(value = inventoryUsed, onValueChange = { inventoryUsed = it }, label = { Text(strings.inventoryUsed) })
                OutlinedTextField(value = chefReq, onValueChange = { chefReq = it }, label = { Text(strings.chefRequirement) })
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PartyStatus.entries.forEach { st ->
                        FilterChip(selected = status == st.key, onClick = { status = st.key }, label = { Text(st.key) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val model = (existing ?: PartyBooking()).copy(
                        partyName = partyName.trim(),
                        partyPhone = partyPhone.trim(),
                        eventDate = eventDate.trim(),
                        guestCount = (guests.toLongOrNull() ?: 20L).coerceAtLeast(1L),
                        totalBudget = budget.toDoubleOrNull() ?: 0.0,
                        advancePaid = advance.toDoubleOrNull() ?: 0.0,
                        inventoryUsedSummary = inventoryUsed.trim(),
                        chefRequirements = chefReq.trim(),
                        status = status
                    )
                    onSave(model)
                },
                enabled = partyName.isNotBlank() && eventDate.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
