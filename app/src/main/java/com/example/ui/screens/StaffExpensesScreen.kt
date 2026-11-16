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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppLanguage
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.DailyExpense
import com.example.data.model.ExpenseCategory
import com.example.data.model.StaffMember
import com.example.ui.theme.LossCoral
import com.example.ui.theme.ProfitEmerald
import com.example.util.AppStrings
import com.example.viewmodel.MaraManagerViewModel
import com.example.viewmodel.MaraWorkspaceData
import com.example.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StaffExpensesScreen(
    data: MaraWorkspaceData,
    strings: AppStrings,
    viewModel: MaraManagerViewModel
) {
    val lang = AppLanguage.fromCode(data.profile.language)
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddStaffDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var attendanceTarget by remember { mutableStateOf<StaffMember?>(null) }
    var showTipPoolDialog by remember { mutableStateOf(false) }

    val aiSalaryState by viewModel.aiSalaryState.collectAsStateWithLifecycle()

    val tabs = listOf(strings.staffAttendanceTab, strings.laborMenuTab, strings.dailyCostsPnlTab)

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 2) {
                FloatingActionButton(
                    onClick = {
                        if (selectedTab == 0) showAddStaffDialog = true
                        else showAddExpenseDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("staff_cost_fab")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add", tint = Color.White)
                }
            }
        }
    ) { innerPad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPad.calculateBottomPadding())
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(label, maxLines = 1) }
                    )
                }
            }

            when (selectedTab) {
                0 -> StaffAttendanceTab(
                    data = data,
                    strings = strings,
                    onMarkAttendance = { attendanceTarget = it },
                    onCheckOut = { attId, tip -> viewModel.checkOutAttendance(attId, tip) },
                    onDeleteStaff = { viewModel.deleteStaff(it) },
                    onRunAiSalary = { showTipPoolDialog = true }
                )
                1 -> LaborCostAndMenuTab(data = data, strings = strings)
                else -> DailyCostsAndPnlTab(
                    data = data,
                    lang = lang,
                    strings = strings,
                    onDeleteExpense = { viewModel.deleteExpense(it) }
                )
            }
        }
    }

    if (showAddStaffDialog) {
        AddStaffDialog(
            strings = strings,
            onDismiss = { showAddStaffDialog = false },
            onAdd = { staff ->
                showAddStaffDialog = false
                viewModel.addStaff(staff)
            }
        )
    }

    if (attendanceTarget != null) {
        MarkAttendanceDialog(
            staff = attendanceTarget!!,
            onDismiss = { attendanceTarget = null },
            onMark = { status, tip, notes ->
                val s = attendanceTarget!!
                attendanceTarget = null
                viewModel.markAttendance(s, status, tip, notes)
            }
        )
    }

    if (showTipPoolDialog) {
        var tipPoolText by remember { mutableStateOf(data.metrics.tipsToday.toInt().toString()) }
        AlertDialog(
            onDismissRequest = { showTipPoolDialog = false },
            title = { Text(strings.aiCalculateSalary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter total Tip Pool to distribute fairly along with attendance-based salary calculation:")
                    OutlinedTextField(
                        value = tipPoolText,
                        onValueChange = { tipPoolText = it },
                        label = { Text("Tip Pool (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val pool = tipPoolText.toDoubleOrNull() ?: 0.0
                    showTipPoolDialog = false
                    viewModel.runAiSalaryCalculation(data.staff, data.attendance, pool, data.profile.language)
                }) { Text("Calculate with AI") }
            },
            dismissButton = { TextButton(onClick = { showTipPoolDialog = false }) { Text("Cancel") } }
        )
    }

    when (val salaryState = aiSalaryState) {
        is UiState.Loading -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Calculating Staff Salary & Tips...") },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text("Gemini AI is analyzing attendance logs & tip splits...")
                    }
                },
                confirmButton = {}
            )
        }
        is UiState.Success -> {
            val report = salaryState.data
            AlertDialog(
                onDismissRequest = { viewModel.clearAiSalary() },
                title = { Text("AI Payroll & Tip Report") },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(report.summary, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Total Payroll: ${formatCurrency(report.totalPayroll)} • Tips: ${formatCurrency(report.totalTipsDistributed)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = ProfitEmerald
                        )
                        report.items.forEach { item ->
                            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                                Column(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
                                    Text("${item.staffName} (${item.role})", fontWeight = FontWeight.Bold)
                                    Text("Days: ${item.daysEquivalent} • Salary: ${formatCurrency(item.earnedSalary)} • Tip: ${formatCurrency(item.tipShare)}")
                                    Text("Net Payout: ${formatCurrency(item.netPayout)}", color = ProfitEmerald, fontWeight = FontWeight.Bold)
                                    if (item.breakdownNote.isNotBlank()) {
                                        Text(item.breakdownNote, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = { Button(onClick = { viewModel.clearAiSalary() }) { Text("Done") } }
            )
        }
        is UiState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearAiSalary() },
                title = { Text("AI Calculation Error") },
                text = { Text(salaryState.message) },
                confirmButton = { Button(onClick = { viewModel.clearAiSalary() }) { Text("OK") } }
            )
        }
        null -> Unit
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            lang = lang,
            strings = strings,
            onDismiss = { showAddExpenseDialog = false },
            onAdd = { exp ->
                showAddExpenseDialog = false
                viewModel.addExpense(exp)
            }
        )
    }
}

@Composable
private fun StaffAttendanceTab(
    data: MaraWorkspaceData,
    strings: AppStrings,
    onMarkAttendance: (StaffMember) -> Unit,
    onCheckOut: (String, Double) -> Unit,
    onDeleteStaff: (String) -> Unit,
    onRunAiSalary: () -> Unit
) {
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                onClick = onRunAiSalary,
                modifier = Modifier.fillMaxWidth().testTag("ai_salary_calc_btn")
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.aiCalculateSalary)
            }
        }

        if (data.staff.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Groups, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("No staff members yet", style = MaterialTheme.typography.titleMedium)
                        Text("Tap + to add Chefs, Tandoor Masters, Waiters, or Delivery Riders.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            items(data.staff, key = { it.id }) { member ->
                val todayAtt = data.attendance.firstOrNull { it.staffId == member.id && it.dateStr == todayStr }
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${member.name} • ${member.role}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Salary: ${formatCurrency(member.monthlySalary)}/mo • Food: ${formatCurrency(member.dailyFoodAllowance)}/day • ${member.shiftHours}h",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (member.phone.isNotBlank()) {
                                    Text("📞 ${member.phone}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            IconButton(onClick = { onDeleteStaff(member.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Staff", tint = LossCoral)
                            }
                        }

                        if (todayAtt != null) {
                            Surface(color = ProfitEmerald.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("${todayAtt.status} • ${todayAtt.dateStr}", fontWeight = FontWeight.Bold, color = ProfitEmerald)
                                        Text(
                                            text = "In: ${todayAtt.checkInTimeStr} | Out: ${todayAtt.checkOutTimeStr.ifBlank { "Active" }} | Tip: ${formatCurrency(todayAtt.tipEarned)}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    if (todayAtt.checkOutTimeStr.isBlank() && todayAtt.status != AttendanceStatus.ABSENT.key) {
                                        OutlinedButton(onClick = { onCheckOut(todayAtt.id, todayAtt.tipEarned) }) {
                                            Text("Clock Out")
                                        }
                                    }
                                }
                            }
                        } else {
                            Button(onClick = { onMarkAttendance(member) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.markAttendance)
                            }
                        }
                    }
                }
            }
        }

        if (data.attendance.isNotEmpty()) {
            item {
                Text("Recent Attendance Logs (Date & Time)", style = MaterialTheme.typography.titleMedium)
            }
            items(data.attendance.take(15), key = { it.id }) { att ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${att.staffName} (${att.role})", fontWeight = FontWeight.SemiBold)
                            Text("${att.dateStr} • In: ${att.checkInTimeStr} ${if (att.checkOutTimeStr.isNotBlank()) "→ Out: ${att.checkOutTimeStr}" else ""}", style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(att.status, fontWeight = FontWeight.Bold, color = if (att.status == "ABSENT") LossCoral else ProfitEmerald)
                            if (att.tipEarned > 0) Text("Tip: ${formatCurrency(att.tipEarned)}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun LaborCostAndMenuTab(data: MaraWorkspaceData, strings: AppStrings) {
    val metrics = data.metrics
    val laborPct = if (metrics.todaySales > 0) (metrics.dailyLaborCost / metrics.todaySales) * 100.0 else 0.0

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.laborCost, style = MaterialTheme.typography.titleLarge)
                    Text(formatCurrency(metrics.dailyLaborCost), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Labor-to-Sales Ratio: ${String.format(Locale.US, "%.1f", laborPct)}% • Staff Food Budget: ${formatCurrency(metrics.dailyStaffFoodCost)}/day",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tips Today: ${formatCurrency(metrics.tipsToday)} • Tips This Month: ${formatCurrency(metrics.tipsMonth)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = ProfitEmerald
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Labor Menu (Staff Meals & Allowances)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Standard Staff Meal Plan • Daily Budget: ${formatCurrency(data.profile.dailyStaffFoodBudget)}", style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider()
                    Text("• Morning Tea & Parantha / Poha (09:30 AM)")
                    Text("• Staff Lunch: Dal, Sabzi, Roti & Rice (02:30 PM)")
                    Text("• Evening Chai & Snacks (06:00 PM)")
                    Text("• Staff Dinner: Dal Makhani / Curry & Tandoori Roti (11:00 PM)")
                    HorizontalDivider()
                    data.staff.forEach { s ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${s.name} (${s.role})")
                            Text("Wage: ${formatCurrency(s.monthlySalary / 30.0)}/d • Food: ${formatCurrency(s.dailyFoodAllowance)}/d", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyCostsAndPnlTab(
    data: MaraWorkspaceData,
    lang: AppLanguage,
    strings: AppStrings,
    onDeleteExpense: (String) -> Unit
) {
    val m = data.metrics
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.dailyCostBreakdown, style = MaterialTheme.typography.titleLarge)
                    CostRow(strings.staffSalaryToday, m.dailyLaborCost)
                    CostRow(strings.rentPerDay, m.dailyRentCost)
                    CostRow(strings.electricityPerDay, m.dailyElectricityCost)
                    CostRow(strings.staffFoodToday, m.dailyStaffFoodCost)
                    CostRow(strings.gasAndOtherToday, m.todayGasAndMaterialCost)
                    CostRow(strings.expiryLossToday, m.todayExpiryLoss)
                    if (m.todayOtherExpenses > 0) CostRow("Other Today", m.todayOtherExpenses)
                    HorizontalDivider()
                    CostRow(strings.todayCost, m.todayTotalCost, bold = true, color = LossCoral)
                    CostRow(strings.todaySales, m.todaySales, bold = true, color = ProfitEmerald)
                    CostRow(
                        if (m.todayNetPnl >= 0) strings.netProfit else strings.netLoss,
                        m.todayNetPnl,
                        bold = true,
                        color = if (m.todayNetPnl >= 0) ProfitEmerald else LossCoral
                    )
                }
            }
        }

        item {
            Text("Logged Expenses", style = MaterialTheme.typography.titleMedium)
        }
        items(data.expenses, key = { it.id }) { exp ->
            val cat = ExpenseCategory.fromKey(exp.category)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(exp.title, style = MaterialTheme.typography.titleMedium)
                        Text("${strings.expenseCatLabel(cat, lang)} • ${exp.dateStr}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(formatCurrency(exp.amount), style = MaterialTheme.typography.titleMedium, color = LossCoral)
                    IconButton(onClick = { onDeleteExpense(exp.id) }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Expense", tint = LossCoral)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun CostRow(label: String, value: Double, bold: Boolean = false, color: Color = Color.Unspecified) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(
            formatCurrency(value),
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color
        )
    }
}

@Composable
private fun AddStaffDialog(
    strings: AppStrings,
    onDismiss: () -> Unit,
    onAdd: (StaffMember) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Chef") }
    var phone by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("18000") }
    var foodAllowance by remember { mutableStateOf("120") }
    var shiftHours by remember { mutableStateOf("9") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addStaff) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Staff Name") }, singleLine = true)
                OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role (Chef / Tandoor / Waiter / Rider)") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = salary, onValueChange = { salary = it }, label = { Text("Monthly Salary") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = foodAllowance, onValueChange = { foodAllowance = it }, label = { Text("Food/Day") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = shiftHours, onValueChange = { shiftHours = it }, label = { Text("Shift Hours") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(
                        StaffMember(
                            name = name.trim(),
                            role = role.ifBlank { "Staff" },
                            phone = phone.trim(),
                            monthlySalary = salary.toDoubleOrNull() ?: 15000.0,
                            dailyFoodAllowance = foodAllowance.toDoubleOrNull() ?: 100.0,
                            shiftHours = (shiftHours.toLongOrNull() ?: 9L).coerceIn(1L, 24L)
                        )
                    )
                },
                enabled = name.isNotBlank()
            ) { Text("Save Staff") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun MarkAttendanceDialog(
    staff: StaffMember,
    onDismiss: () -> Unit,
    onMark: (AttendanceStatus, Double, String) -> Unit
) {
    var status by remember { mutableStateOf(AttendanceStatus.PRESENT) }
    var tipText by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Attendance: ${staff.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Date & time are stamped automatically.", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AttendanceStatus.entries.forEach { st ->
                        FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st.key) })
                    }
                }
                OutlinedTextField(
                    value = tipText,
                    onValueChange = { tipText = it },
                    label = { Text("Tip Earned Today (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (Optional)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = { onMark(status, tipText.toDoubleOrNull() ?: 0.0, notes.trim()) }) {
                Text("Stamp Now")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddExpenseDialog(
    lang: AppLanguage,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onAdd: (DailyExpense) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExpenseCategory.MATERIAL_GAS) }
    var amount by remember { mutableStateOf("") }
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addExpense) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExpenseCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(strings.expenseCatLabel(cat, lang)) }
                        )
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Expense Title (e.g. Gas Cylinder / Expired Paneer)") }, singleLine = true)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(
                        DailyExpense(
                            title = title.trim(),
                            category = category.key,
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            dateStr = todayStr
                        )
                    )
                },
                enabled = title.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0.0
            ) { Text("Log Expense") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
