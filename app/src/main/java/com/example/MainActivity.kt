package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.AppLanguage
import com.example.data.repository.AiManagerRepository
import com.example.data.repository.RestaurantRepository
import com.example.firebase.authStateFlow
import com.example.firebase.configureAppCheck
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InventoryPartyScreen
import com.example.ui.screens.MenuQrScreen
import com.example.ui.screens.OrdersPosScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StaffExpensesScreen
import com.example.ui.theme.MaraManagerTheme
import com.example.util.L10n
import com.example.viewmodel.MaraManagerViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory: configure App Check with launch intent BEFORE setContent or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            MaraManagerRoot()
        }
    }
}

@Composable
fun MaraManagerRoot() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        MaraManagerTheme {
            AuthScreen(onAuthSuccess = {})
        }
    } else {
        key(user.uid) {
            AuthenticatedMaraApp(currentUserId = user.uid)
        }
    }
}

private enum class SubScreen {
    NONE,
    SETTINGS,
    AI_ASSISTANT
}

@Composable
fun AuthenticatedMaraApp(currentUserId: String) {
    val viewModel: MaraManagerViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                MaraManagerViewModel(
                    repo = RestaurantRepository(db),
                    aiRepo = AiManagerRepository(),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val workspaceState by viewModel.workspaceState.collectAsStateWithLifecycle()
    val toastMsg by viewModel.toastMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMsg) {
        val msg = toastMsg
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    when (val state = workspaceState) {
        is UiState.Loading -> {
            MaraManagerTheme {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text("Loading Mara Manager...", modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }
        }
        is UiState.Error -> {
            MaraManagerTheme {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.saveProfile(com.example.data.model.RestaurantProfile(userId = currentUserId)) }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
        is UiState.Success -> {
            val data = state.data
            val profile = data.profile
            val strings = L10n.forLang(AppLanguage.fromCode(profile.language))

            MaraManagerTheme(
                themePresetKey = profile.themePreset,
                customAccentHex = profile.accentColorHex,
                customTextHex = profile.textColorHex,
                darkTheme = profile.darkMode
            ) {
                var selectedTab by remember { mutableIntStateOf(0) }
                var subScreen by remember { mutableStateOf(SubScreen.NONE) }

                val navItems = listOf(
                    strings.navDashboard to Icons.Rounded.Dashboard,
                    strings.navOrders to Icons.Rounded.ReceiptLong,
                    strings.navMenuQr to Icons.Rounded.RestaurantMenu,
                    strings.navStockParty to Icons.Rounded.Inventory2,
                    strings.navStaffCosts to Icons.Rounded.Groups
                )

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f)
                                )
                            )
                        )
                ) {
                    val isWideScreen = maxWidth >= 700.dp

                    // Optional Custom Gallery Wallpaper Overlay
                    if (profile.wallpaperPath.isNotBlank() && File(profile.wallpaperPath).exists()) {
                        Image(
                            painter = rememberAsyncImagePainter(File(profile.wallpaperPath)),
                            contentDescription = "Custom Restaurant Wallpaper",
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(profile.wallpaperOpacity.toFloat().coerceIn(0.05f, 0.75f)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Scaffold(
                        containerColor = Color.Transparent,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        contentWindowInsets = WindowInsets.safeDrawing,
                        bottomBar = {
                            if (!isWideScreen && subScreen == SubScreen.NONE) {
                                NavigationBar(
                                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                                ) {
                                    navItems.forEachIndexed { idx, (label, icon) ->
                                        NavigationBarItem(
                                            selected = selectedTab == idx,
                                            onClick = { selectedTab = idx },
                                            icon = { Icon(icon, contentDescription = label) },
                                            label = { Text(label, maxLines = 1) }
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPad ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPad)
                        ) {
                            if (isWideScreen && subScreen == SubScreen.NONE) {
                                NavigationRail {
                                    navItems.forEachIndexed { idx, (label, icon) ->
                                        NavigationRailItem(
                                            selected = selectedTab == idx,
                                            onClick = { selectedTab = idx },
                                            icon = { Icon(icon, contentDescription = label) },
                                            label = { Text(label, maxLines = 1) }
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                when (subScreen) {
                                    SubScreen.SETTINGS -> SettingsScreen(
                                        data = data,
                                        strings = strings,
                                        viewModel = viewModel,
                                        onBack = { subScreen = SubScreen.NONE }
                                    )
                                    SubScreen.AI_ASSISTANT -> AiAssistantScreen(
                                        data = data,
                                        viewModel = viewModel,
                                        onBack = { subScreen = SubScreen.NONE }
                                    )
                                    SubScreen.NONE -> when (selectedTab) {
                                        0 -> DashboardScreen(
                                            data = data,
                                            strings = strings,
                                            onNavigateOrders = { selectedTab = 1 },
                                            onNavigateMenuQr = { selectedTab = 2 },
                                            onNavigateInventory = { selectedTab = 3 },
                                            onNavigateStaffCosts = { selectedTab = 4 },
                                            onOpenAiAssistant = { subScreen = SubScreen.AI_ASSISTANT },
                                            onOpenSettings = { subScreen = SubScreen.SETTINGS }
                                        )
                                        1 -> OrdersPosScreen(data = data, strings = strings, viewModel = viewModel)
                                        2 -> MenuQrScreen(data = data, strings = strings, viewModel = viewModel)
                                        3 -> InventoryPartyScreen(data = data, strings = strings, viewModel = viewModel)
                                        else -> StaffExpensesScreen(data = data, strings = strings, viewModel = viewModel)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
