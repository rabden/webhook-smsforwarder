package com.rabden.smsforwarder.ui.navigation

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.rabden.smsforwarder.ui.components.ExpressiveExtendedFab

object Routes {
    const val LOGS = "logs"
    const val CONTACTS = "contacts"
    const val SETTINGS = "settings?focusWebhook={focusWebhook}"
    const val BRAND_OPTIMIZATION = "brand_optimization/{brandName}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val view = LocalView.current

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route ?: Routes.LOGS

    val logsViewModel: LogsViewModel = viewModel()
    val contactsViewModel: ContactsViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val logs by logsViewModel.logs.collectAsState()
    val selectedIds by logsViewModel.selectedIds.collectAsState()
    val isSelectionMode = selectedIds.isNotEmpty()

    val settingsUiState by settingsViewModel.uiState.collectAsState()
    val hasWebhookUrl = settingsUiState.webhookUrl.isNotBlank()
    val contacts by contactsViewModel.customContacts.collectAsState()
    val isContactsScreen = currentDestination == Routes.CONTACTS
    val isContactsSelecting by contactsViewModel.isSelectionMode.collectAsState()
    val isBlacklistSelecting by contactsViewModel.isBlacklistSelectionMode.collectAsState()
    val selectedContactsList by contactsViewModel.selectedContacts.collectAsState()
    val selectedBlacklistList by contactsViewModel.selectedBlacklistContacts.collectAsState()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showContactsDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showBlacklistDeleteConfirmDialog by remember { mutableStateOf(false) }
    var autoOpenAddDialog by remember { mutableStateOf(false) }
    var showContactSettingsSheet by remember { mutableStateOf(false) }

    val logsListState = rememberLazyListState()
    val fabExpanded by remember {
        derivedStateOf {
            logsListState.firstVisibleItemIndex == 0 && logsListState.firstVisibleItemScrollOffset < 50
        }
    }

    val isMainScreen = currentDestination == Routes.LOGS

    BackHandler(enabled = isMainScreen && isSelectionMode) {
        logsViewModel.clearSelection()
    }

    var topPad by remember { mutableStateOf(0.dp) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            val title = when {
                isMainScreen && isSelectionMode -> "${selectedIds.size} selected"
                isMainScreen -> "Messages"
                isContactsScreen && isContactsSelecting -> "${selectedContactsList.size} selected"
                isContactsScreen && isBlacklistSelecting -> "${selectedBlacklistList.size} selected"
                isContactsScreen -> "Contacts"
                currentDestination.startsWith("brand_optimization/") -> {
                    val brandName = navBackStackEntry?.arguments?.getString("brandName") ?: "Device"
                    "$brandName Optimization"
                }
                currentDestination.startsWith("settings") -> "Settings"
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
                        isContactsScreen && (isContactsSelecting || isBlacklistSelecting) -> {
                            IconButton(onClick = {
                                contactsViewModel.clearSelection()
                                contactsViewModel.clearBlacklistSelection()
                            }) {
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
                    containerColor = Color.Transparent,
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
                            IconButton(onClick = { showDeleteConfirmDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete selected", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        isContactsScreen && isContactsSelecting -> {
                            val allContactsSelected = contacts.isNotEmpty() && selectedContactsList.size == contacts.size
                            IconButton(onClick = {
                                if (allContactsSelected) contactsViewModel.clearSelection()
                                else contactsViewModel.selectAll()
                            }) {
                                Icon(
                                    if (allContactsSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                    contentDescription = if (allContactsSelected) "Deselect all" else "Select all"
                                )
                            }
                            IconButton(onClick = { showContactsDeleteConfirmDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete selected", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        isContactsScreen && isBlacklistSelecting -> {
                            val allBlacklistSelected = selectedBlacklistList.isNotEmpty() && selectedBlacklistList.size == contacts.size
                            IconButton(onClick = {
                                if (allBlacklistSelected) contactsViewModel.clearBlacklistSelection()
                                else contactsViewModel.selectAllBlacklist()
                            }) {
                                Icon(
                                    if (allBlacklistSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                    contentDescription = if (allBlacklistSelected) "Deselect all" else "Select all"
                                )
                            }
                            IconButton(onClick = { showBlacklistDeleteConfirmDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete selected", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        isContactsScreen -> {
                            IconButton(onClick = { showContactSettingsSheet = true }) {
                                Icon(Icons.Default.Tune, contentDescription = "Filter Settings")
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
                ExpressiveExtendedFab(
                    expanded = fabExpanded,
                    onClick = {
                        navController.navigate(Routes.CONTACTS)
                    },
                    icon = Icons.Default.Contacts,
                    text = "Contacts"
                )
            }
        }
    ) { innerPadding ->
        topPad = innerPadding.calculateTopPadding()
        val bottomPad = innerPadding.calculateBottomPadding()
        val background = MaterialTheme.colorScheme.background

        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Routes.LOGS,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomPad),
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(300)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(300)
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(300)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(300)
                    )
                }
            ) {
                composable(Routes.LOGS) {
                    LogsScreen(
                        viewModel = logsViewModel,
                        hasWebhookUrl = hasWebhookUrl,
                        hasContacts = contacts.isNotEmpty(),
                        onConfigureWebhook = { navController.navigate("settings?focusWebhook=true") },
                        onAddContact = {
                            autoOpenAddDialog = true
                            navController.navigate(Routes.CONTACTS)
                        },
                        contentTopPadding = topPad,
                        listState = logsListState
                    )
                }
                composable(Routes.CONTACTS) {
                    BackHandler(enabled = isContactsSelecting || isBlacklistSelecting) {
                        contactsViewModel.clearSelection()
                        contactsViewModel.clearBlacklistSelection()
                    }
                    ContactsScreen(
                        viewModel = contactsViewModel,
                        contentTopPadding = topPad,
                        autoOpenAddDialog = autoOpenAddDialog,
                        showSettingsSheet = showContactSettingsSheet,
                        onSettingsSheetDismiss = { showContactSettingsSheet = false }
                    )
                    LaunchedEffect(Unit) { autoOpenAddDialog = false }
                }
                composable(
                    route = Routes.SETTINGS,
                    arguments = listOf(navArgument("focusWebhook") { type = NavType.StringType; defaultValue = "false" })
                ) { backStackEntry ->
                    val focusWebhook = backStackEntry.arguments?.getString("focusWebhook")?.toBoolean() ?: false
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        focusWebhookUrl = focusWebhook,
                        contentTopPadding = topPad,
                        onNavigateToOptimization = { brandName ->
                            navController.navigate("brand_optimization/$brandName")
                        }
                    )
                }
                composable(Routes.BRAND_OPTIMIZATION) { backStackEntry ->
                    val brandName = backStackEntry.arguments?.getString("brandName") ?: "OTHER"
                    com.rabden.smsforwarder.ui.settings.BrandOptimizationScreen(
                        brandName = brandName,
                        contentTopPadding = topPad,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(topPad)
                    .background(
                        Brush.verticalGradient(
                            0.0f to background,
                            0.4f to background.copy(alpha = 0.9f),
                            0.7f to background.copy(alpha = 0.5f),
                            0.9f to background.copy(alpha = 0.1f),
                            1.0f to Color.Transparent
                        )
                    )
            )
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Messages") },
            text = { Text("Are you sure you want to permanently delete ${selectedIds.size} message(s)? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        logsViewModel.deleteSelected()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showContactsDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showContactsDeleteConfirmDialog = false },
            title = { Text("Delete Contacts") },
            text = { Text("Are you sure you want to delete ${selectedContactsList.size} contact(s)?") },
            confirmButton = {
                Button(
                    onClick = {
                        contactsViewModel.deleteSelected()
                        showContactsDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showContactsDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBlacklistDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBlacklistDeleteConfirmDialog = false },
            title = { Text("Delete Blacklisted Contacts") },
            text = { Text("Are you sure you want to delete ${selectedBlacklistList.size} contact(s)?") },
            confirmButton = {
                Button(
                    onClick = {
                        contactsViewModel.deleteSelectedBlacklist()
                        showBlacklistDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlacklistDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
