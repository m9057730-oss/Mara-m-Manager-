package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MenuItem
import com.example.ui.theme.LossCoral
import com.example.ui.theme.ProfitEmerald
import com.example.util.AppStrings
import com.example.util.PrintAndShareHelper
import com.example.viewmodel.MaraManagerViewModel
import com.example.viewmodel.MaraWorkspaceData
import com.example.viewmodel.UiState
import java.io.File

@Composable
fun MenuQrScreen(
    data: MaraWorkspaceData,
    strings: AppStrings,
    viewModel: MaraManagerViewModel
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showAddDishDialog by remember { mutableStateOf(false) }
    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }

    val scannedState by viewModel.scannedMenuState.collectAsStateWithLifecycle()

    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)
                if (bmp != null) {
                    viewModel.scanMenuPhoto(bmp, data.profile.language)
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraTempUri
        if (success && uri != null) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)
                if (bmp != null) {
                    viewModel.scanMenuPhoto(bmp, data.profile.language)
                }
            }
        }
    }

    val tabs = listOf(strings.fineDineMenu, strings.restaurantQrMenu)
    val categories = listOf("Starters", "Main Course", "Tandoor & Breads", "Beverages", "Desserts")

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddDishDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("add_dish_fab")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = strings.addDish, tint = Color.White)
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

            if (selectedTab == 0) {
                // Fine-Dine Menu + AI Photo-to-Menu Scanner Bar
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(strings.scanMenuPhoto, style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "Snap or pick a photo of any paper menu — Gemini AI converts it into a structured Fine-Dine Menu automatically.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    val file = File(context.cacheDir, "menu_scan_${System.currentTimeMillis()}.jpg")
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    cameraTempUri = uri
                                    cameraLauncher.launch(uri)
                                },
                                modifier = Modifier.weight(1f).testTag("scan_menu_camera_btn")
                            ) {
                                Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Camera")
                            }
                            OutlinedButton(
                                onClick = {
                                    galleryPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f).testTag("scan_menu_gallery_btn")
                            ) {
                                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Gallery")
                            }
                        }
                    }
                }

                // Category Chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All (${data.menuItems.size})") }
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(cat) }
                        )
                    }
                }

                val filtered = data.menuItems.filter {
                    selectedCategory == null || it.category == selectedCategory
                }

                if (filtered.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Rounded.RestaurantMenu,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Your Fine-Dine Menu is ready to build", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Scan a menu photo with Camera/Gallery or tap + to add signature dishes.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.id }) { dish ->
                            MenuDishCard(
                                dish = dish,
                                onToggleAvailable = { viewModel.toggleMenuAvailability(dish) },
                                onDelete = { viewModel.deleteMenuItem(dish.id) }
                            )
                        }
                        item { Spacer(Modifier.height(72.dp)) }
                    }
                }
            } else {
                // Restaurant Branded QR Order Menu
                QrMenuShowcase(data = data, strings = strings)
            }
        }
    }

    // Scanned Menu Draft Review Dialog
    when (val state = scannedState) {
        is UiState.Loading -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Scanning Menu with Gemini AI...") },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Extracting dishes, prices, categories & fine-dine descriptions...")
                    }
                },
                confirmButton = {}
            )
        }
        is UiState.Success -> {
            val drafts = state.data
            AlertDialog(
                onDismissRequest = { viewModel.clearScannedMenu() },
                title = { Text("Scanned ${drafts.size} Dishes") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        drafts.forEach { d ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("${d.name} — ₹${d.price.toInt()}", fontWeight = FontWeight.Bold)
                                    Text("${d.category} • ${if (d.veg) "Veg" else "Non-Veg"} • ${d.prepMinutes}m", style = MaterialTheme.typography.labelSmall)
                                    if (d.description.isNotBlank()) {
                                        Text(d.description, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.saveScannedMenuDrafts(drafts) }) {
                        Text("Save All to Menu")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.clearScannedMenu() }) { Text("Cancel") }
                }
            )
        }
        is UiState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearScannedMenu() },
                title = { Text("Scan Issue") },
                text = { Text(state.message) },
                confirmButton = {
                    Button(onClick = { viewModel.clearScannedMenu() }) { Text("OK") }
                }
            )
        }
        null -> Unit
    }

    if (showAddDishDialog) {
        AddDishDialog(
            categories = categories,
            onDismiss = { showAddDishDialog = false },
            onAdd = { item ->
                showAddDishDialog = false
                viewModel.addMenuItem(item)
            }
        )
    }
}

@Composable
private fun MenuDishCard(
    dish: MenuItem,
    onToggleAvailable: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (dish.veg) ProfitEmerald else LossCoral)
                    )
                    Text(dish.name, style = MaterialTheme.typography.titleMedium)
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = dish.courseType.ifBlank { dish.category },
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                if (dish.description.isNotBlank()) {
                    Text(dish.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = "${dish.category} • ${dish.prepMinutes} mins • Price: ${formatCurrency(dish.price)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Switch(checked = dish.available, onCheckedChange = { onToggleAvailable() })
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Dish", tint = LossCoral)
                }
            }
        }
    }
}

@Composable
private fun QrMenuShowcase(data: MaraWorkspaceData, strings: AppStrings) {
    val context = LocalContext.current
    val restName = data.profile.restaurantName.ifBlank { "Mara Spice Kitchen" }
    val menuPreviewList = data.menuItems.filter { it.available }.take(15).joinToString("\n") {
        "${it.name} - ₹${it.price.toInt()}"
    }
    val qrPayload = "MARA_MENU|restaurant=$restName|phone=${data.profile.phone}|upi=${data.profile.upiId}\n$menuPreviewList"
    val qrBitmap = remember(qrPayload) { PrintAndShareHelper.generateQrBitmap(qrPayload, 560) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Rounded.QrCode2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
                Text(
                    text = restName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Scan to Browse Fine-Dine Menu & Order",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (qrBitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Restaurant QR Order Menu",
                            modifier = Modifier
                                .padding(14.dp)
                                .size(230.dp)
                        )
                    }
                }
                Text(
                    text = "${data.menuItems.count { it.available }} Active Fine-Dine Dishes",
                    style = MaterialTheme.typography.labelLarge,
                    color = ProfitEmerald
                )
                Button(
                    onClick = {
                        val shareText = "*$restName — Digital Menu*\n\n" +
                            data.menuItems.filter { it.available }.joinToString("\n") {
                                "• ${it.name} (${it.category}) — ₹${it.price.toInt()}"
                            }
                        PrintAndShareHelper.shareToWhatsAppOrChooser(context, shareText, "Share Menu")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share Digital Menu to WhatsApp")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddDishDialog(
    categories: List<String>,
    onDismiss: () -> Unit,
    onAdd: (MenuItem) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(categories.first()) }
    var price by remember { mutableStateOf("") }
    var costPrice by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var veg by remember { mutableStateOf(true) }
    var prepMins by remember { mutableStateOf("15") }
    var courseType by remember { mutableStateOf("Fine Dine") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Fine-Dine Dish") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Dish Name") }, singleLine = true)
                Text("Category", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it },
                        label = { Text("Cost (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = prepMins,
                    onValueChange = { prepMins = it },
                    label = { Text("Prep Time (mins)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Fine-Dine Description") }
                )
                OutlinedTextField(
                    value = courseType,
                    onValueChange = { courseType = it },
                    label = { Text("Style (Fine Dine / Chef Special)") },
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (veg) "Vegetarian" else "Non-Vegetarian")
                    Switch(checked = veg, onCheckedChange = { veg = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(
                        MenuItem(
                            name = name.trim(),
                            category = category,
                            price = price.toDoubleOrNull() ?: 0.0,
                            costPrice = costPrice.toDoubleOrNull() ?: 0.0,
                            description = description.trim(),
                            veg = veg,
                            available = true,
                            prepMinutes = prepMins.toLongOrNull() ?: 15L,
                            courseType = courseType.ifBlank { "Fine Dine" }
                        )
                    )
                },
                enabled = name.isNotBlank() && (price.toDoubleOrNull() ?: 0.0) >= 0.0
            ) {
                Text("Save Dish")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
