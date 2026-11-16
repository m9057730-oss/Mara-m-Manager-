package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeliveryDining
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.OrderChannel
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.ui.theme.HappyAmber
import com.example.ui.theme.LossCoral
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.SwiggyOrange
import com.example.ui.theme.ZomatoRed
import com.example.util.AppStrings
import com.example.viewmodel.MaraWorkspaceData
import java.util.Locale

@Composable
fun DashboardScreen(
    data: MaraWorkspaceData,
    strings: AppStrings,
    onNavigateOrders: () -> Unit,
    onNavigateMenuQr: () -> Unit,
    onNavigateInventory: () -> Unit,
    onNavigateStaffCosts: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val metrics = data.metrics
    val isProfit = metrics.todayNetPnl >= 0.0
    val liveOrders = data.orders.filter { it.status == OrderStatus.RUNNING.key }
    val pendingOrders = data.orders.filter { it.status == OrderStatus.PENDING.key }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Restaurant Header Banner (Dynamic Restaurant Name + Settings + AI)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.appTitle,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                            Text(
                                text = data.profile.restaurantName.ifBlank { "Mara Spice Kitchen" },
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${data.profile.address} • ${metrics.todayOrdersCount} Orders Today",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = onOpenAiAssistant,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.22f))
                                    .testTag("dashboard_ai_button")
                            ) {
                                Icon(
                                    Icons.Rounded.AutoAwesome,
                                    contentDescription = strings.quickAiManager,
                                    tint = Color.White
                                )
                            }
                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.22f))
                                    .testTag("dashboard_settings_button")
                            ) {
                                Icon(
                                    Icons.Rounded.Settings,
                                    contentDescription = strings.settingsTitle,
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Low-Stock Alert Banner (if any inventory item is low/out)
        if (data.lowStockItems.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEB)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateInventory() }
                        .testTag("low_stock_alert_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(LossCoral.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.NotificationsActive,
                                contentDescription = strings.lowStockAlertTitle,
                                tint = LossCoral
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${strings.lowStockAlertTitle} (${data.lowStockItems.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = LossCoral
                            )
                            Text(
                                text = data.lowStockItems.take(3).joinToString(", ") {
                                    "${it.name}: ${formatQty(it.quantity)} ${it.unit}"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF7F1D1D),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 3. Today's Profit & Loss + Sales + Cost + Target Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = strings.todayProfitLoss,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = formatCurrency(metrics.todayNetPnl),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = if (isProfit) ProfitEmerald else LossCoral
                                )
                                Surface(
                                    color = (if (isProfit) ProfitEmerald else LossCoral).copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isProfit) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (isProfit) ProfitEmerald else LossCoral
                                        )
                                        Text(
                                            text = if (isProfit) strings.netProfit else strings.netLoss,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isProfit) ProfitEmerald else LossCoral,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MiniMetricTile(
                            title = strings.todaySales,
                            value = formatCurrency(metrics.todaySales),
                            accent = ProfitEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        MiniMetricTile(
                            title = strings.todayCost,
                            value = formatCurrency(metrics.todayTotalCost),
                            accent = LossCoral,
                            modifier = Modifier.weight(1f)
                        )
                        MiniMetricTile(
                            title = strings.laborCost,
                            value = formatCurrency(metrics.dailyLaborCost),
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Today Target Progress
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${strings.todayTarget}: ${formatCurrency(data.profile.dailyTarget)}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "${(metrics.targetProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { metrics.targetProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(50)),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        // 4. Today Profit/Loss & Cost Bar Chart (Compose Canvas)
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.revenueVsCostChart,
                        style = MaterialTheme.typography.titleMedium
                    )
                    ProfitLossBarChart(
                        sales = metrics.todaySales,
                        labor = metrics.dailyLaborCost,
                        overhead = metrics.dailyRentCost + metrics.dailyElectricityCost + metrics.dailyStaffFoodCost,
                        materialsAndLoss = metrics.todayGasAndMaterialCost + metrics.todayExpiryLoss + metrics.todayOtherExpenses,
                        netPnl = metrics.todayNetPnl
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ChartLegend("Sales", ProfitEmerald)
                        ChartLegend("Labor", MaterialTheme.colorScheme.primary)
                        ChartLegend("Rent+Elec", HappyAmber)
                        ChartLegend("Gas+Exp", LossCoral)
                    }
                }
            }
        }

        // 5. Quick Actions Row
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    QuickActionPill(strings.quickNewOrder, Icons.Rounded.AddShoppingCart, MaterialTheme.colorScheme.primary, onNavigateOrders)
                }
                item {
                    QuickActionPill(strings.quickScanMenu, Icons.Rounded.DocumentScanner, MaterialTheme.colorScheme.secondary, onNavigateMenuQr)
                }
                item {
                    QuickActionPill(strings.quickQrMenu, Icons.Rounded.QrCode2, HappyAmber, onNavigateMenuQr)
                }
                item {
                    QuickActionPill(strings.quickAiManager, Icons.Rounded.AutoAwesome, Color(0xFF8B5CF6), onOpenAiAssistant)
                }
            }
        }

        // 6. Live Running Orders & Pending Orders Strip (with live elapsed timer)
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "${strings.liveOrders} (${liveOrders.size}) • ${strings.pendingOrders} (${pendingOrders.size})",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        FilledTonalButton(onClick = onNavigateOrders) {
                            Text("POS")
                        }
                    }

                    if (liveOrders.isEmpty() && pendingOrders.isEmpty()) {
                        Text(
                            text = "No live or pending orders right now. Tap POS to punch a Table, Delivery, or 3rd-Party order.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        (liveOrders + pendingOrders).take(4).forEach { order ->
                            LiveOrderTickerRow(
                                order = order,
                                nowMillis = data.nowMillis,
                                strings = strings,
                                onClick = onNavigateOrders
                            )
                        }
                    }
                }
            }
        }

        // 7. Separate 3rd Party Orders (Zomato, Swiggy, Other Apps) vs Restaurant Own Delivery / Table / Pickup / Online
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.thirdPartyOrders,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChannelCountCard(strings.zomatoLabel, metrics.zomatoCount, ZomatoRed, Modifier.weight(1f))
                        ChannelCountCard(strings.swiggyLabel, metrics.swiggyCount, SwiggyOrange, Modifier.weight(1f))
                        ChannelCountCard(strings.otherAppLabel, metrics.otherAppCount, Color(0xFF7C3AED), Modifier.weight(1f))
                    }
                    Text(
                        text = "${strings.ownDeliveryLabel} & Direct Channels",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChannelCountCard(strings.ownDeliveryLabel, metrics.ownDeliveryCount, ProfitEmerald, Modifier.weight(1f))
                        ChannelCountCard(strings.tableOrderLabel, metrics.tableCount, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        ChannelCountCard(strings.pickupOrderLabel, metrics.pickupCount, HappyAmber, Modifier.weight(1f))
                        ChannelCountCard(strings.onlineOrderLabel, metrics.onlineCount, Color(0xFF0EA5E9), Modifier.weight(1f))
                    }
                }
            }
        }

        // 8. Tips (Today & Month) + Staff Attendance Snapshot
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateStaffCosts() }
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.VolunteerActivism, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text(strings.tipsToday, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(metrics.tipsToday), style = MaterialTheme.typography.titleLarge)
                        Text("${strings.tipsMonth}: ${formatCurrency(metrics.tipsMonth)}", style = MaterialTheme.typography.labelSmall, color = ProfitEmerald)
                    }
                }
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateStaffCosts() }
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Staff & Attendance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${data.staff.size} Staff", style = MaterialTheme.typography.titleLarge)
                        Text("Rent+Elec/Day: ${formatCurrency(metrics.dailyRentCost + metrics.dailyElectricityCost)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniMetricTile(title: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.1f)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ProfitLossBarChart(
    sales: Double,
    labor: Double,
    overhead: Double,
    materialsAndLoss: Double,
    netPnl: Double
) {
    val maxVal = maxOf(sales, labor + overhead + materialsAndLoss, 100.0).toFloat()
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
    ) {
        val barWidth = size.width / 6.5f
        val gap = (size.width - barWidth * 4) / 5f
        val values = listOf(
            sales.toFloat() to ProfitEmerald,
            labor.toFloat() to primaryColor,
            overhead.toFloat() to HappyAmber,
            materialsAndLoss.toFloat() to LossCoral
        )
        values.forEachIndexed { index, (value, color) ->
            val x = gap + index * (barWidth + gap)
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(x, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = CornerRadius(14f, 14f)
            )
            val barH = ((value / maxVal) * size.height).coerceIn(8f, size.height)
            drawRoundRect(
                color = color,
                topLeft = Offset(x, size.height - barH),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(14f, 14f)
            )
        }
    }
}

@Composable
private fun ChartLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun QuickActionPill(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = color)
        }
    }
}

@Composable
private fun LiveOrderTickerRow(
    order: OrderRecord,
    nowMillis: Long,
    strings: AppStrings,
    onClick: () -> Unit
) {
    val elapsedSec = ((nowMillis - order.createdAtMillis) / 1000L).coerceAtLeast(0L)
    val mins = elapsedSec / 60L
    val secs = elapsedSec % 60L
    val timerText = String.format(Locale.US, "%02d:%02d", mins, secs)
    val channel = OrderChannel.fromKey(order.channel)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${order.orderNumber} • ${strings.channelLabel(channel)} ${order.tableOrRef}",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = order.itemsSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = if (order.status == OrderStatus.RUNNING.key) MaterialTheme.colorScheme.primary else HappyAmber,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "⏱ $timerText",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun ChannelCountCard(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.11f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(count.toString(), style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

fun formatCurrency(amount: Double): String {
    val sign = if (amount < 0) "-" else ""
    val absVal = kotlin.math.abs(amount)
    return if (absVal % 1.0 == 0.0) {
        "${sign}₹${absVal.toLong()}"
    } else {
        "${sign}₹${String.format(Locale.US, "%.0f", absVal)}"
    }
}

private fun formatQty(q: Double): String =
    if (q % 1.0 == 0.0) q.toInt().toString() else String.format(Locale.US, "%.1f", q)
