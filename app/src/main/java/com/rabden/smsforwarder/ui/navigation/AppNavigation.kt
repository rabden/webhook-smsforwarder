package com.rabden.smsforwarder.ui.navigation

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Textsms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rabden.smsforwarder.ui.contacts.ContactsScreen
import com.rabden.smsforwarder.ui.contacts.ContactsViewModel
import com.rabden.smsforwarder.ui.logs.LogsScreen
import com.rabden.smsforwarder.ui.logs.LogsViewModel
import com.rabden.smsforwarder.ui.settings.SettingsScreen
import com.rabden.smsforwarder.ui.settings.SettingsViewModel
import com.rabden.smsforwarder.ui.theme.*

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Contacts : Screen("contacts", "Whitelist", Icons.Default.Contacts)
    data object Logs : Screen("logs", "Messages", Icons.Default.Textsms)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Contacts,
    Screen.Logs,
    Screen.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    
    // Check if we should show Contacts (Whitelist) first. 
    // Default is true for the very first run.
    val showContactsFirst = remember { 
        val isFirstRun = sharedPrefs.getBoolean("first_run_completed", false).not()
        if (isFirstRun) {
            sharedPrefs.edit().putBoolean("first_run_completed", true).apply()
        }
        isFirstRun
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = navBackStackEntry?.destination?.route ?: (if (showContactsFirst) Screen.Contacts.route else Screen.Logs.route)

    // Activity-scoped ViewModels: Pre-instantiated to ensure "Warm State"
    // even when the UI screens are dismounted.
    val logsViewModel: LogsViewModel = viewModel()
    val contactsViewModel: ContactsViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    var showClearLogsDialog by remember { mutableStateOf(false) }

    if (showClearLogsDialog) {
        AlertDialog(
            onDismissRequest = { showClearLogsDialog = false },
            title = { Text("Clear All Logs") },
            text = { Text("Are you sure you want to permanently delete all message logs? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        logsViewModel.clearAllLogs()
                        showClearLogsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearLogsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            val isBottomNavScreen = bottomNavItems.any { it.route == currentRoute }
            val title = if (isBottomNavScreen) {
                bottomNavItems.find { it.route == currentRoute }?.title ?: "Forwarder"
            } else if (currentRoute.startsWith("brand_optimization/")) {
                val brandName = navBackStackEntry?.arguments?.getString("brandName") ?: "Device"
                "$brandName Optimization"
            } else {
                "Forwarder"
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
                    if (!isBottomNavScreen) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    if (currentRoute == Screen.Logs.route) {
                        IconButton(onClick = { showClearLogsDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear All")
                        }
                    }
                }
            )
        },
        bottomBar = {
            val isBottomNavScreen = bottomNavItems.any { it.route == currentRoute }
            if (isBottomNavScreen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Standard NavHost: Dismounts off-screen views for better resource management.
        // Because the ViewModels are activity-scoped and data is "Eager", 
        // the screens will still render almost instantly when navigated back to.
        NavHost(
            navController = navController,
            startDestination = if (showContactsFirst) Screen.Contacts.route else Screen.Logs.route,
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            composable(Screen.Contacts.route) {
                ContactsScreen(viewModel = contactsViewModel)
            }
            composable(Screen.Logs.route) {
                LogsScreen(viewModel = logsViewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateToOptimization = { brandName ->
                        navController.navigate("brand_optimization/$brandName")
                    }
                )
            }
            composable("brand_optimization/{brandName}") { backStackEntry ->
                val brandName = backStackEntry.arguments?.getString("brandName") ?: "OTHER"
                com.rabden.smsforwarder.ui.settings.BrandOptimizationScreen(
                    brandName = brandName,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
