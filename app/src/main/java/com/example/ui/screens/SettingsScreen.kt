package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.model.AppLanguage
import com.example.data.model.ThemePreset
import com.example.firebase.signOutUser
import com.example.ui.theme.LossCoral
import com.example.ui.theme.parseHexColorOrNull
import com.example.util.AppStrings
import com.example.util.PrintAndShareHelper
import com.example.viewmodel.MaraManagerViewModel
import com.example.viewmodel.MaraWorkspaceData

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    data: MaraWorkspaceData,
    strings: AppStrings,
    viewModel: MaraManagerViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val p = data.profile
    var restaurantName by remember(p.restaurantName) { mutableStateOf(p.restaurantName) }
    var address by remember(p.address) { mutableStateOf(p.address) }
    var phone by remember(p.phone) { mutableStateOf(p.phone) }
    var gstin by remember(p.gstin) { mutableStateOf(p.gstin) }
    var gstPercent by remember(p.gstPercent) { mutableStateOf(p.gstPercent.toString()) }
    var dailyTarget by remember(p.dailyTarget) { mutableStateOf(p.dailyTarget.toInt().toString()) }
    var monthlyRent by remember(p.monthlyRent) { mutableStateOf(p.monthlyRent.toInt().toString()) }
    var monthlyElectricity by remember(p.monthlyElectricity) { mutableStateOf(p.monthlyElectricity.toInt().toString()) }
    var dailyStaffFood by remember(p.dailyStaffFoodBudget) { mutableStateOf(p.dailyStaffFoodBudget.toInt().toString()) }
    var upiId by remember(p.upiId) { mutableStateOf(p.upiId) }
    var autoKot by remember(p.autoKotEnabled) { mutableStateOf(p.autoKotEnabled) }
    var zomatoId by remember(p.zomatoStoreId) { mutableStateOf(p.zomatoStoreId) }
    var swiggyId by remember(p.swiggyStoreId) { mutableStateOf(p.swiggyStoreId) }
    var otherWebhook by remember(p.otherAppWebhook) { mutableStateOf(p.otherAppWebhook) }
    var language by remember(p.language) { mutableStateOf(p.language) }
    var themePreset by remember(p.themePreset) { mutableStateOf(p.themePreset) }
    var accentHex by remember(p.accentColorHex) { mutableStateOf(p.accentColorHex) }
    var textHex by remember(p.textColorHex) { mutableStateOf(p.textColorHex) }
    var wallpaperPath by remember(p.wallpaperPath) { mutableStateOf(p.wallpaperPath) }
    var wallpaperOpacity by remember(p.wallpaperOpacity) { mutableFloatStateOf(p.wallpaperOpacity.toFloat()) }
    var darkMode by remember(p.darkMode) { mutableStateOf(p.darkMode) }

    val wallpaperPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val savedPath = PrintAndShareHelper.copyUriToInternalWallpaper(context, uri)
            if (savedPath != null) {
                wallpaperPath = savedPath
                viewModel.saveProfile(p.copy(wallpaperPath = savedPath))
            }
        }
    }

    val accentSwatches = listOf("", "#FF6B35", "#00A878", "#E84393", "#2563EB", "#D97706", "#7C3AED")
    val textSwatches = listOf("", "#1E1916", "#0F172A", "#1E3A8A", "#065F46", "#7C2D12", "#FFF8F0")

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Language Switcher (English, Hindi, Punjabi)
            item {
                SettingsSectionCard(strings.languageLabel) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = language == lang.code,
                                onClick = {
                                    language = lang.code
                                    viewModel.updateLanguage(p, lang.code)
                                },
                                label = { Text(lang.nativeLabel) },
                                modifier = Modifier.testTag("lang_chip_${lang.code}")
                            )
                        }
                    }
                }
            }

            // 2. Happy Colors, Word Color & Gallery Wallpaper
            item {
                SettingsSectionCard("${strings.themeColorsLabel} & Wallpaper") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemePreset.entries.forEach { preset ->
                            FilterChip(
                                selected = themePreset == preset.key,
                                onClick = {
                                    themePreset = preset.key
                                    accentHex = ""
                                },
                                label = { Text(preset.title) }
                            )
                        }
                    }

                    Text(strings.accentColorLabel, style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        accentSwatches.forEach { hex ->
                            val swatchColor = parseHexColorOrNull(hex) ?: MaterialTheme.colorScheme.primary
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(
                                        width = if (accentHex == hex) 3.dp else 1.dp,
                                        color = if (accentHex == hex) MaterialTheme.colorScheme.onSurface else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable { accentHex = hex }
                            )
                        }
                    }

                    Text(strings.textColorLabel, style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        textSwatches.forEach { hex ->
                            val swatchColor = parseHexColorOrNull(hex) ?: Color(0xFF1E1916)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(
                                        width = if (textHex == hex) 3.dp else 1.dp,
                                        color = if (textHex == hex) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable { textHex = hex }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dark Mode")
                        Switch(checked = darkMode, onCheckedChange = { darkMode = it })
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                wallpaperPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("pick_wallpaper_btn")
                        ) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.pickWallpaper)
                        }
                        if (wallpaperPath.isNotBlank()) {
                            OutlinedButton(onClick = { wallpaperPath = "" }) {
                                Text(strings.clearWallpaper)
                            }
                        }
                    }
                    if (wallpaperPath.isNotBlank()) {
                        Text("Wallpaper Visibility: ${(wallpaperOpacity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = wallpaperOpacity,
                            onValueChange = { wallpaperOpacity = it },
                            valueRange = 0.05f..0.75f
                        )
                    }
                }
            }

            // 3. Restaurant Profile, Targets & Daily Costs
            item {
                SettingsSectionCard("Restaurant Profile & Daily Cost Config") {
                    OutlinedTextField(value = restaurantName, onValueChange = { restaurantName = it }, label = { Text("Restaurant Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = gstin, onValueChange = { gstin = it }, label = { Text("GSTIN") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = gstPercent,
                            onValueChange = { gstPercent = it },
                            label = { Text("Default GST %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = dailyTarget,
                            onValueChange = { dailyTarget = it },
                            label = { Text("Daily Target (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = monthlyRent,
                            onValueChange = { monthlyRent = it },
                            label = { Text("Monthly Rent (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = monthlyElectricity,
                            onValueChange = { monthlyElectricity = it },
                            label = { Text("Monthly Elec (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = dailyStaffFood,
                        onValueChange = { dailyStaffFood = it },
                        label = { Text("Daily Staff Food Budget (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it },
                        label = { Text("Merchant UPI ID (for QR Payment)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // 4. Auto-KOT & Zomato / Swiggy / Ordering App Connectors
            item {
                SettingsSectionCard("POS, Auto-KOT & Zomato/Swiggy Connectors") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.autoKot)
                        Switch(checked = autoKot, onCheckedChange = { autoKot = it })
                    }
                    OutlinedTextField(value = zomatoId, onValueChange = { zomatoId = it }, label = { Text("Zomato Partner Store ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = swiggyId, onValueChange = { swiggyId = it }, label = { Text("Swiggy Partner Store ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = otherWebhook, onValueChange = { otherWebhook = it }, label = { Text("Other Ordering App Webhook / API URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedButton(onClick = { viewModel.testThirdPartyWebhook(otherWebhook) }) {
                        Text("Test Ordering App Connection")
                    }
                }
            }

            // 5. Save, Restart App, Sign Out
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            viewModel.saveProfile(
                                p.copy(
                                    restaurantName = restaurantName.trim().ifBlank { "Mara Spice Kitchen" },
                                    address = address.trim(),
                                    phone = phone.trim(),
                                    gstin = gstin.trim(),
                                    gstPercent = gstPercent.toDoubleOrNull() ?: 5.0,
                                    dailyTarget = dailyTarget.toDoubleOrNull() ?: 25000.0,
                                    monthlyRent = monthlyRent.toDoubleOrNull() ?: 30000.0,
                                    monthlyElectricity = monthlyElectricity.toDoubleOrNull() ?: 9000.0,
                                    dailyStaffFoodBudget = dailyStaffFood.toDoubleOrNull() ?: 600.0,
                                    upiId = upiId.trim(),
                                    autoKotEnabled = autoKot,
                                    zomatoStoreId = zomatoId.trim(),
                                    swiggyStoreId = swiggyId.trim(),
                                    otherAppWebhook = otherWebhook.trim(),
                                    language = language,
                                    themePreset = themePreset,
                                    accentColorHex = accentHex,
                                    textColorHex = textHex,
                                    wallpaperPath = wallpaperPath,
                                    wallpaperOpacity = wallpaperOpacity.toDouble(),
                                    darkMode = darkMode
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_settings_btn")
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.saveSettings)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { PrintAndShareHelper.restartApp(context) },
                            modifier = Modifier.weight(1f).testTag("restart_app_btn")
                        ) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.restartApp)
                        }
                        Button(
                            onClick = { signOutUser(credentialManager, scope) },
                            colors = ButtonDefaults.buttonColors(containerColor = LossCoral),
                            modifier = Modifier.weight(1f).testTag("sign_out_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.signOut)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
