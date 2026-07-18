package com.rabden.smsforwarder.ui.navigation

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rabden.smsforwarder.ui.contacts.ContactsScreen
import com.rabden.smsforwarder.ui.contacts.ContactsViewModel
import com.rabden.smsforwarder.ui.logs.LogsScreen
import com.rabden.smsforwarder.ui.logs.LogsViewModel
import com.rabden.smsforwarder.ui.settings.SettingsScreen
import com.rabden.smsforwarder.ui.settings.SettingsViewModel

object Routes {
    const val LOGS = "logs"
    const val SETTINGS = "settings?focusWebhook={focusWebhook}"
    const val BRAND_OPTIMIZATION = "brand_optimization/{brandName}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val haptic = LocalHapticFeedback.current

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route ?: Routes.LOGS

    // Activity-scoped ViewModels: Pre-instantiated to ensure "Warm State"
    // even when the UI screens are dismounted.
    val logsViewModel: LogsViewModel = viewModel()
    val contactsViewModel: ContactsViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val logs by logsViewModel.logs.collectAsState()
    val selectedIds by logsViewModel.selectedIds.collectAsState()
    val isSelectionMode = selectedIds.isNotEmpty()

    val settingsUiState by settingsViewModel.uiState.collectAsState()
    val hasWebhookUrl = settingsUiState.webhookUrl.isNotBlank()
    val contacts by contactsViewModel.customContacts.collectAsState()

    var showWhitelistSheet by remember { mutableStateOf(false) }
    var autoOpenAddDialog by remember { mutableStateOf(false) }

    // First run: auto-open whitelist so the user adds numbers.
    LaunchedEffect(Unit) {
        if (!sharedPrefs.getBoolean("first_run_completed", false)) {
            sharedPrefs.edit().putBoolean("first_run_completed", true).apply()
            showWhitelistSheet = true
        }
    }

    val isMainScreen = currentDestination == Routes.LOGS
    val isSettingsScreen = currentDestination.startsWith("settings")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            val title = when {
                isMainScreen && isSelectionMode -> "${selectedIds.size} selected"
                isMainScreen -> "Messages"
                currentDestination.startsWith("brand_optimization/") -> {
                    val brandName = navBackStackEntry?.arguments?.getString("brandName") ?: "Device"
                    "$brandName Optimization"
                }
                isSettingsScreen -> "Settings"
                else -> "Forwarder"
            }

            TopAppBar(
                title = {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    when {
                        isMainScreen && isSelectionMode -> {
                            IconButton(onClick = { logsViewModel.clearSelection() }) {
                                Icon(Icons.Default.Close, contentDescription = "Exit selection")
                            }
                        }
                        !isMainScreen -> {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    when {
                        isMainScreen && isSelectionMode -> {
                            val allSelected = logs.isNotEmpty() && selectedIds.size == logs.size
                            IconButton(onClick = {
                                if (allSelected) logsViewModel.clearSelection()
                                else logsViewModel.selectAll()
                            }) {
                                Icon(
                                    if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                    contentDescription = if (allSelected) "Deselect all" else "Select all"
                                )
                            }
                            IconButton(onClick = { logsViewModel.deleteSelected() }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete selected", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        isMainScreen -> {
                            IconButton(onClick = { navController.navigate("settings?focusWebhook=false") }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (isMainScreen && !isSelectionMode) {
                ExtendedFloatingActionButton(
                    onClick = {
                        autoOpenAddDialog = false
                        showWhitelistSheet = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    icon = { Icon(Icons.Default.Contacts, contentDescription = null) },
                    text = { Text("Whitelist") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOGS,
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            composable(Routes.LOGS) {
                LogsScreen(
                    viewModel = logsViewModel,
                    hasWebhookUrl = hasWebhookUrl,
                    hasContacts = contacts.isNotEmpty(),
                    onConfigureWebhook = { navController.navigate("settings?focusWebhook=true") },
                    onAddContact = {
                        autoOpenAddDialog = true
                        showWhitelistSheet = true
                    }
                )
            }
            composable(
                route = Routes.SETTINGS,
                arguments = listOf(navArgument("focusWebhook") { type = NavType.StringType; defaultValue = "false" })
            ) { backStackEntry ->
                val focusWebhook = backStackEntry.arguments?.getString("focusWebhook")?.toBoolean() ?: false
                SettingsScreen(
                    viewModel = settingsViewModel,
                    focusWebhookUrl = focusWebhook,
                    onNavigateToOptimization = { brandName ->
                        navController.navigate("brand_optimization/$brandName")
                    }
                )
            }
            composable(Routes.BRAND_OPTIMIZATION) { backStackEntry ->
                val brandName = backStackEntry.arguments?.getString("brandName") ?: "OTHER"
                com.rabden.smsforwarder.ui.settings.BrandOptimizationScreen(
                    brandName = brandName,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }

    if (showWhitelistSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                showWhitelistSheet = false
                autoOpenAddDialog = false
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            ContactsScreen(
                viewModel = contactsViewModel,
                autoOpenAddDialog = autoOpenAddDialog
            )
        }
    }
}
