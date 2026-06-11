package com.rabden.smsforwarder.ui.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Date

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onNavigateToOptimization: (String) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var webhookUrlInput by remember { mutableStateOf(uiState.webhookUrl) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveComplete by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var showReliabilityWarning by remember { mutableStateOf(false) }
    val isAggressive = com.rabden.smsforwarder.util.BrandHelper.isAggressiveBrand()

    LaunchedEffect(uiState.webhookUrl) {
        if (!hasUnsavedChanges) {
            webhookUrlInput = uiState.webhookUrl
        }
    }
    
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshBatteryStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Forwarding Group (switch + url + device)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Service Configuration", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ListCard(
                    onClick = { viewModel.setForwardingEnabled(!uiState.isForwardingEnabled) },
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Forwarding Service",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (uiState.isForwardingEnabled) "Active and listening" else "Service paused",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Switch(
                            checked = uiState.isForwardingEnabled,
                            onCheckedChange = { viewModel.setForwardingEnabled(it) }
                        )
                    }
                }

                ListCard(
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Webhook destination URL",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                        TextField(
                            value = webhookUrlInput,
                            onValueChange = {
                                webhookUrlInput = it
                                hasUnsavedChanges = true
                            },
                            placeholder = { Text("https://url.com") },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = CircleShape,
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                errorIndicatorColor = Color.Transparent
                            )
                        )
                    }
                }

                var deviceNameInput by remember { mutableStateOf(uiState.deviceName) }
                LaunchedEffect(uiState.deviceName) {
                    if (!hasUnsavedChanges) {
                        deviceNameInput = uiState.deviceName
                    }
                }

                val bottomCorner by animateDpAsState(
                    targetValue = if (hasUnsavedChanges) 8.dp else 28.dp,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "bottomCorner"
                )
                ListCard(
                    modifier = Modifier.zIndex(1f),
                    shape = RoundedCornerShape(
                        topStart = 8.dp, topEnd = 8.dp,
                        bottomStart = bottomCorner, bottomEnd = bottomCorner
                    )
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Device identifier",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                        TextField(
                            value = deviceNameInput,
                            onValueChange = {
                                deviceNameInput = it
                                hasUnsavedChanges = true
                            },
                            placeholder = { Text("e.g. Pixel 7 Pro") },
                            leadingIcon = { Icon(Icons.Default.Devices, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = CircleShape,
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                errorIndicatorColor = Color.Transparent
                            )
                        )
                    }
                }

                AnimatedVisibility(
                    visible = hasUnsavedChanges,
                    enter = slideInVertically(initialOffsetY = { -it }),
                    exit = slideOutVertically(targetOffsetY = { -it })
                ) {
                    ListCard(
                        onClick = {
                            if (!isSaving && !saveComplete) {
                                isSaving = true
                                viewModel.updateWebhookUrl(webhookUrlInput)
                                viewModel.updateDeviceName(deviceNameInput)
                            }
                        },
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            AnimatedContent(
                                targetState = when {
                                    isSaving -> "saving"
                                    saveComplete -> "complete"
                                    else -> "idle"
                                },
                                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                                label = "saveState"
                            ) { state ->
                                when (state) {
                                    "saving" -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    "complete" -> {
                                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Saved", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    else -> {
                                        Text("Save configuration", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                LaunchedEffect(isSaving) {
                    if (isSaving) {
                        kotlinx.coroutines.delay(800)
                        isSaving = false
                        saveComplete = true
                    }
                }

                LaunchedEffect(saveComplete) {
                    if (saveComplete) {
                        kotlinx.coroutines.delay(1000)
                        saveComplete = false
                        hasUnsavedChanges = false
                        focusManager.clearFocus()
                    }
                }

                // Detect changes in both inputs
                LaunchedEffect(webhookUrlInput, deviceNameInput) {
                    hasUnsavedChanges = webhookUrlInput != uiState.webhookUrl || deviceNameInput != uiState.deviceName
                }
            }
        }

        // 2. Custom Headers
        var showAddHeaderDialog by remember { mutableStateOf(false) }
        
        if (showAddHeaderDialog) {
            var newKey by remember { mutableStateOf("") }
            var newValue by remember { mutableStateOf("") }
            val focusRequester = remember { FocusRequester() }
            
            AlertDialog(
                onDismissRequest = { showAddHeaderDialog = false },
                title = { Text("New Custom Header") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Header name", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp))
                            TextField(value = newKey, onValueChange = { newKey = it }, placeholder = { Text("e.g. Authorization") }, singleLine = true, modifier = Modifier.fillMaxWidth().focusRequester(focusRequester), shape = CircleShape, colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent, errorIndicatorColor = Color.Transparent))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Header value", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp))
                            TextField(value = newValue, onValueChange = { newValue = it }, placeholder = { Text("e.g. Bearer token") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = CircleShape, colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent, errorIndicatorColor = Color.Transparent))
                        }
                    }
                    LaunchedEffect(Unit) { focusRequester.requestFocus() }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showAddHeaderDialog = false }) { Text("Cancel") }
                        Button(onClick = { if (newKey.isNotBlank()) { viewModel.addHeader(newKey, newValue); showAddHeaderDialog = false } }) { Text("Add") }
                    }
                }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("HTTP Headers", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
            
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val headerList = uiState.customHeaders.toList()
                
                if (headerList.isEmpty()) {
                    ListCard(
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                    ) {
                        Text("No custom headers configured", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    headerList.forEachIndexed { index, (key, value) ->
                        val shape = if (index == 0) {
                            RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                        } else {
                            RoundedCornerShape(8.dp)
                        }
                        
                        ListCard(shape = shape) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(key, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                    Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                }
                                IconButton(onClick = { viewModel.removeHeader(key) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                ListCard(
                    onClick = { showAddHeaderDialog = true },
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add custom header", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Reliability & Optimization Group
        val brand = com.rabden.smsforwarder.util.BrandHelper.getDeviceBrand()
        val showBrandOptimization = brand != com.rabden.smsforwarder.util.BrandHelper.Brand.OTHER && brand != com.rabden.smsforwarder.util.BrandHelper.Brand.PIXEL

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Reliability & Optimization", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // Battery & Last Forward - now at top
                ListCard(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatusItem(
                            icon = if (uiState.isBatteryOptimizationIgnored) Icons.Default.CheckCircle else Icons.Default.BatteryAlert,
                            label = "Battery",
                            value = if (uiState.isBatteryOptimizationIgnored) "Optimized" else "Restricted",
                            color = if (uiState.isBatteryOptimizationIgnored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f),
                            showRipple = true,
                            onClick = {
                                if (!uiState.isBatteryOptimizationIgnored) {
                                    com.rabden.smsforwarder.util.PermissionHelper.requestIgnoreBatteryOptimizations(context)
                                } else {
                                    viewModel.refreshBatteryStatus()
                                }
                            }
                        )
                        StatusItem(
                            icon = Icons.Default.History,
                            label = "Last forward",
                            value = if (uiState.lastForwardedTime == 0L) "Never" else formatTime(context, uiState.lastForwardedTime),
                            modifier = Modifier.weight(1f),
                            showRipple = false
                        )
                    }
                }

                ListCard(
                    onClick = { 
                        if (isAggressive && uiState.isHighReliabilityMode) {
                            showReliabilityWarning = true
                        } else {
                            viewModel.setHighReliabilityMode(!uiState.isHighReliabilityMode)
                        }
                    },
                    shape = if (showBrandOptimization) RoundedCornerShape(8.dp) else RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("High Reliability Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Uses a silent foreground service to prevent the OS from killing the app.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = uiState.isHighReliabilityMode, 
                            onCheckedChange = { 
                                if (isAggressive && uiState.isHighReliabilityMode) {
                                    showReliabilityWarning = true
                                } else {
                                    viewModel.setHighReliabilityMode(it)
                                }
                            }
                        )
                    }
                }

                if (showReliabilityWarning) {
                    AlertDialog(
                        onDismissRequest = { showReliabilityWarning = false },
                        title = { Text("Disable High Reliability?") },
                        text = {
                            Text("Your device brand (${com.rabden.smsforwarder.util.BrandHelper.getDeviceBrand().name}) is known for aggressively killing background apps. Turning this off will likely stop SMS forwarding from working when the app is closed.")
                        },
                        confirmButton = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { showReliabilityWarning = false }) {
                                    Text("Keep it ON")
                                }
                                Button(
                                    onClick = {
                                        viewModel.setHighReliabilityMode(false)
                                        showReliabilityWarning = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Turn OFF anyway")
                                }
                            }
                        }
                    )
                }

                if (showBrandOptimization) {
                    ListCard(
                        onClick = { onNavigateToOptimization(brand.name) },
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Setup ${brand.name} Reliability", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Background settings guide", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showRipple: Boolean = true
) {
    Row(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = if (showRipple) androidx.compose.foundation.LocalIndication.current else null, onClick = onClick) else Modifier)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun BrandOptimizationScreen(
    brandName: String,
    onBackClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val brand = try {
        com.rabden.smsforwarder.util.BrandHelper.Brand.valueOf(brandName.uppercase())
    } catch (e: Exception) {
        com.rabden.smsforwarder.util.BrandHelper.Brand.OTHER
    }
    
    val instructions = com.rabden.smsforwarder.util.BrandHelper.getBrandInstructions(brand).split("\n")
    val actions = com.rabden.smsforwarder.util.BrandHelper.getBrandActions(context, brand)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Hero Card or Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${brand.name} Background Guide",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Aggressive battery optimization on some devices can stop background SMS forwarding. Follow these steps to ensure continuous operation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        // Instructions Section
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Steps to follow:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    instructions.forEach { step ->
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Quick Actions Section
        if (actions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Quick Actions:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    actions.forEach { action ->
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(action.intent)
                                } catch (e: Exception) {
                                    val settingsIntent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = android.net.Uri.parse("package:${context.packageName}")
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(settingsIntent)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = CircleShape
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (action.label.contains("App Info")) Icons.Default.Info else Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = action.label,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    onClick: (() -> Unit)? = null,
    showRipple: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .then(if (onClick != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = if (showRipple) androidx.compose.foundation.LocalIndication.current else null, onClick = onClick) else Modifier)
                .padding(20.dp),
            content = content
        )
    }
}

@Composable
fun ListCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    shape: Shape = RoundedCornerShape(28.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        onClick = onClick ?: {}
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

private fun formatTime(context: android.content.Context, timestamp: Long): String {
    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    val pattern = if (is24Hour) "HH:mm" else "hh:mm a"
    val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
    return sdf.format(Date(timestamp))
}
