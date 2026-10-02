package com.rabden.smsforwarder.ui.settings

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import com.rabden.smsforwarder.ui.components.ListCard
import com.rabden.smsforwarder.ui.components.ExpressiveFilterModeToggle
import com.rabden.smsforwarder.ui.components.M3ExpressiveLoader
import com.rabden.smsforwarder.util.formatTimestamp

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, focusWebhookUrl: Boolean = false, contentTopPadding: Dp = 0.dp, onNavigateToOptimization: (String) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var webhookUrlInput by remember { mutableStateOf(uiState.webhookUrl) }
    var deviceNameInput by remember { mutableStateOf(uiState.deviceName) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveComplete by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var showReliabilityWarning by remember { mutableStateOf(false) }
    val isAggressive = com.rabden.smsforwarder.util.BrandHelper.isAggressiveBrand()
    val webhookFocusRequester = remember { FocusRequester() }

    val testState by viewModel.testState.collectAsState()
    var showErrorDialog by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    var showTestButton by remember { mutableStateOf(false) }
    var showTestText by remember { mutableStateOf(false) }
    val revealThresholdPx = with(density) { 32.dp.toPx() }
    val maxPullPx = with(density) { 60.dp.toPx() }
    val revealWidth = 56.dp
    val revealWidthPx = with(density) { revealWidth.toPx() }
    val maxRubberBandPx = with(density) { 36.dp.toPx() }
    val dragOffset = remember { Animatable(0f) }
    var isRevealed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    LaunchedEffect(uiState.webhookUrl) {
        if (!hasUnsavedChanges) {
            webhookUrlInput = uiState.webhookUrl
        }
    }

    LaunchedEffect(focusWebhookUrl) {
        if (focusWebhookUrl) {
            webhookFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(showTestText) {
        if (showTestText) {
            kotlinx.coroutines.delay(2500)
            showTestText = false
        }
    }

    LaunchedEffect(testState) {
        if (testState is WebhookTestState.Success) {
            kotlinx.coroutines.delay(2500)
            isRevealed = false
            showTestButton = false
            showTestText = false
            coroutineScope.launch {
                dragOffset.animateTo(
                    0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            viewModel.clearTestState()
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
            viewModel.clearTestState()
            isRevealed = false
            showTestButton = false
            showTestText = false
            coroutineScope.launch { dragOffset.snapTo(0f) }
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
            .padding(start = 10.dp, end = 10.dp, top = contentTopPadding + 20.dp, bottom = 20.dp),
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
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .pointerInput(showTestButton) {
                            detectHorizontalDragGestures(
                                onDragStart = {},
                                onDragEnd = {
                                    coroutineScope.launch {
                                        if (!showTestButton) {
                                            if (dragOffset.value < -revealThresholdPx) {
                                                showTestButton = true
                                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                            }
                                        } else {
                                            if (dragOffset.value > revealThresholdPx) {
                                                showTestButton = false
                                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                            }
                                        }
                                        dragOffset.animateTo(
                                            0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        dragOffset.animateTo(
                                            0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                    }
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    coroutineScope.launch {
                                        val current = dragOffset.value
                                        val target = if (!showTestButton) {
                                            if (dragAmount < 0) {
                                                if (current < -maxPullPx) {
                                                    current + dragAmount * 0.2f
                                                } else {
                                                    current + dragAmount * 0.85f
                                                }
                                            } else {
                                                current + dragAmount * 0.2f
                                            }
                                        } else {
                                            if (dragAmount > 0) {
                                                if (current > maxPullPx) {
                                                    current + dragAmount * 0.2f
                                                } else {
                                                    current + dragAmount * 0.85f
                                                }
                                            } else {
                                                current + dragAmount * 0.2f
                                            }
                                        }
                                        dragOffset.snapTo(target)
                                    }
                                }
                            )
                        },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(dragOffset.value.roundToInt(), 0) },
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Webhook destination URL",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextField(
                                value = webhookUrlInput,
                                onValueChange = {
                                    webhookUrlInput = it
                                    hasUnsavedChanges = true
                                },
                                placeholder = { Text("https://url.com") },
                                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(webhookFocusRequester),
                                shape = CircleShape,
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    errorIndicatorColor = Color.Transparent
                                )
                            )

                            AnimatedVisibility(
                                visible = showTestButton,
                                enter = expandHorizontally(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) + fadeIn(),
                                exit = shrinkHorizontally(
                                    animationSpec = tween(250)
                                ) + fadeOut()
                            ) {
                                WebhookTestButton(
                                    testState = testState,
                                    showLabel = showTestText,
                                    onClick = {
                                        if (testState is WebhookTestState.Failed) {
                                            showErrorDialog = true
                                        } else if (testState !is WebhookTestState.Running) {
                                            viewModel.testWebhook(
                                                url = webhookUrlInput.ifBlank { uiState.webhookUrl },
                                                device = deviceNameInput.ifBlank { uiState.deviceName },
                                                headers = uiState.customHeaders
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                LaunchedEffect(uiState.deviceName) {
                    if (!hasUnsavedChanges) {
                        deviceNameInput = uiState.deviceName
                    }
                }

                val saveInteractionSource = remember { MutableInteractionSource() }
                val isSavePressed by saveInteractionSource.collectIsPressedAsState()
                val saveTopCorner by animateDpAsState(
                    targetValue = if (isSavePressed || isSaving || saveComplete) 28.dp else 8.dp,
                    animationSpec = spring(dampingRatio = 0.58f, stiffness = 400f),
                    label = "saveTopCorner"
                )
                val deviceBottomCorner by animateDpAsState(
                    targetValue = if (!hasUnsavedChanges) 28.dp else saveTopCorner,
                    animationSpec = spring(dampingRatio = 0.58f, stiffness = 400f),
                    label = "deviceBottomCorner"
                )

                ListCard(
                    modifier = Modifier.zIndex(1f),
                    shape = RoundedCornerShape(
                        topStart = 8.dp, topEnd = 8.dp,
                        bottomStart = deviceBottomCorner, bottomEnd = deviceBottomCorner
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
                    val savePressScale by animateFloatAsState(
                        targetValue = if (isSavePressed) 0.95f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.52f, stiffness = 500f),
                        label = "savePressScale"
                    )

                    ListCard(
                        onClick = {
                            if (!isSaving && !saveComplete) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                isSaving = true
                                viewModel.updateWebhookUrl(webhookUrlInput)
                                viewModel.updateDeviceName(deviceNameInput)
                            }
                        },
                        interactionSource = saveInteractionSource,
                        shape = RoundedCornerShape(
                            topStart = saveTopCorner,
                            topEnd = saveTopCorner,
                            bottomStart = 28.dp,
                            bottomEnd = 28.dp
                        ),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.graphicsLayer {
                            scaleX = savePressScale
                            scaleY = savePressScale
                        }
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            AnimatedContent(
                                targetState = when {
                                    isSaving -> "saving"
                                    saveComplete -> "complete"
                                    else -> "idle"
                                },
                                transitionSpec = {
                                    (fadeIn(animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f)) +
                                            scaleIn(initialScale = 0.85f, animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f)))
                                        .togetherWith(
                                            fadeOut(animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f)) +
                                                    scaleOut(targetScale = 0.85f, animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f))
                                        )
                                },
                                label = "saveState"
                            ) { state ->
                                when (state) {
                                    "saving" -> {
                                        M3ExpressiveLoader(
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
                        kotlinx.coroutines.delay(800)
                        saveComplete = false
                        hasUnsavedChanges = false
                        focusManager.clearFocus()
                        if (webhookUrlInput.isNotBlank()) {
                            showTestButton = true
                            showTestText = true
                        }
                    }
                }

                // Detect changes in both inputs
                LaunchedEffect(webhookUrlInput, deviceNameInput) {
                    val urlChanged = webhookUrlInput != uiState.webhookUrl
                    hasUnsavedChanges = urlChanged || deviceNameInput != uiState.deviceName
                }
            }
        }

        // 2. Filter Mode
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Filter Mode",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp),
                fontWeight = FontWeight.Bold
            )
            ExpressiveFilterModeToggle(
                selectedMode = uiState.filterMode,
                onModeSelected = { viewModel.setFilterMode(it) },
                showDescription = false
            )
        }

        // 3. Custom Headers
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
                val addHeaderInteractionSource = remember { MutableInteractionSource() }
                val isAddHeaderPressed by addHeaderInteractionSource.collectIsPressedAsState()
                val addHeaderTopCorner by animateDpAsState(
                    targetValue = if (isAddHeaderPressed) 28.dp else 8.dp,
                    animationSpec = spring(dampingRatio = 0.58f, stiffness = 400f),
                    label = "addHeaderTopCorner"
                )
                val addHeaderPressScale by animateFloatAsState(
                    targetValue = if (isAddHeaderPressed) 0.95f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.52f, stiffness = 500f),
                    label = "addHeaderPressScale"
                )
                val addHeaderIconRotation by animateFloatAsState(
                    targetValue = if (isAddHeaderPressed) 90f else 0f,
                    animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
                    label = "addHeaderIconRotation"
                )
                
                if (headerList.isEmpty()) {
                    ListCard(
                        shape = RoundedCornerShape(
                            topStart = 28.dp,
                            topEnd = 28.dp,
                            bottomStart = addHeaderTopCorner,
                            bottomEnd = addHeaderTopCorner
                        )
                    ) {
                        Text("No custom headers configured", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    headerList.forEachIndexed { index, (key, value) ->
                        val isFirst = index == 0
                        val isLast = index == headerList.size - 1
                        val shape = RoundedCornerShape(
                            topStart = if (isFirst) 28.dp else 8.dp,
                            topEnd = if (isFirst) 28.dp else 8.dp,
                            bottomStart = if (isLast) addHeaderTopCorner else 8.dp,
                            bottomEnd = if (isLast) addHeaderTopCorner else 8.dp
                        )
                        
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
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        showAddHeaderDialog = true
                    },
                    interactionSource = addHeaderInteractionSource,
                    shape = RoundedCornerShape(
                        topStart = addHeaderTopCorner,
                        topEnd = addHeaderTopCorner,
                        bottomStart = 28.dp,
                        bottomEnd = 28.dp
                    ),
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.graphicsLayer {
                        scaleX = addHeaderPressScale
                        scaleY = addHeaderPressScale
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer { rotationZ = addHeaderIconRotation },
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Add custom header",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. Reliability & Optimization Group
        val brand = com.rabden.smsforwarder.util.BrandHelper.getDeviceBrand()
        val showBrandOptimization = brand != com.rabden.smsforwarder.util.BrandHelper.Brand.OTHER && brand != com.rabden.smsforwarder.util.BrandHelper.Brand.PIXEL

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Reliability & Optimization", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // Battery Optimization
                ListCard(
                    onClick = {
                        if (!uiState.isBatteryOptimizationIgnored) {
                            com.rabden.smsforwarder.util.PermissionHelper.requestIgnoreBatteryOptimizations(context)
                        } else {
                            viewModel.refreshBatteryStatus()
                        }
                    },
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isBatteryOptimizationIgnored) Icons.Default.CheckCircle else Icons.Default.BatteryAlert,
                                contentDescription = null,
                                tint = if (uiState.isBatteryOptimizationIgnored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Battery Optimization",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (uiState.isBatteryOptimizationIgnored) "Unrestricted (Optimized)" else "Restricted (Tap to optimize)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (uiState.isBatteryOptimizationIgnored) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                                )
                            }
                        }
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

    if (showErrorDialog && testState is WebhookTestState.Failed) {
        WebhookErrorDialog(
            errorState = testState as WebhookTestState.Failed,
            onDismiss = { showErrorDialog = false },
            onRetry = {
                showErrorDialog = false
                viewModel.testWebhook(
                    url = webhookUrlInput.ifBlank { uiState.webhookUrl },
                    device = deviceNameInput.ifBlank { uiState.deviceName },
                    headers = uiState.customHeaders
                )
            }
        )
    }
}

@Composable
fun WebhookTestButton(
    testState: WebhookTestState,
    showLabel: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = when (testState) {
            is WebhookTestState.Running -> MaterialTheme.colorScheme.primaryContainer
            is WebhookTestState.Success -> Color(0xFF2E7D32).copy(alpha = 0.22f)
            is WebhookTestState.Failed -> MaterialTheme.colorScheme.errorContainer
            is WebhookTestState.Idle -> MaterialTheme.colorScheme.secondaryContainer
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "testBtnContainer"
    )

    val contentColor by animateColorAsState(
        targetValue = when (testState) {
            is WebhookTestState.Running -> MaterialTheme.colorScheme.primary
            is WebhookTestState.Success -> Color(0xFF2E7D32)
            is WebhookTestState.Failed -> MaterialTheme.colorScheme.error
            is WebhookTestState.Idle -> MaterialTheme.colorScheme.onSecondaryContainer
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "testBtnContent"
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
            .height(52.dp)
            .defaultMinSize(minWidth = 52.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (showLabel) 16.dp else 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
                when (testState) {
                    is WebhookTestState.Running -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = contentColor
                        )
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Running test...",
                            tint = contentColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    is WebhookTestState.Success -> {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Test successful",
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    is WebhookTestState.Failed -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Test failed. Tap to see error details.",
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    is WebhookTestState.Idle -> {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Test Webhook",
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showLabel,
                enter = expandHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(),
                exit = shrinkHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeOut()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Test",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
fun WebhookErrorDialog(
    errorState: WebhookTestState.Failed,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "Webhook Test Failed",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errorState.statusCode != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "HTTP Status Code: ${errorState.statusCode}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Text(
                    text = errorState.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Target Webhook URL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorState.testedUrl.ifBlank { "None" },
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (!errorState.responseBody.isNullOrBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Server Response Body",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorState.responseBody,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .heightIn(max = 140.dp)
                                    .verticalScroll(rememberScrollState())
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Troubleshooting Tips:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val tip = when {
                        errorState.statusCode == 404 -> "• The requested URL route does not exist. Verify the endpoint path."
                        errorState.statusCode in listOf(401, 403) -> "• Authentication failed. Check your custom HTTP headers and API tokens."
                        errorState.statusCode != null && errorState.statusCode >= 500 -> "• The webhook destination server encountered an internal error."
                        errorState.errorMessage.contains("resolve host", ignoreCase = true) -> "• Domain name couldn't be resolved. Check for typos in the hostname."
                        errorState.errorMessage.contains("Connection refused", ignoreCase = true) -> "• Server refused connection. Ensure your server is running on the specified port."
                        errorState.errorMessage.contains("timed out", ignoreCase = true) -> "• Request timed out. Ensure the server firewall allows external HTTP POST traffic."
                        else -> "• Ensure device has network connectivity and the destination accepts raw JSON POST requests."
                    }
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Close")
                }
                Button(
                    onClick = onRetry
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry")
                }
            }
        }
    )
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
    contentTopPadding: Dp = 0.dp,
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
            .padding(start = 24.dp, end = 24.dp, top = contentTopPadding + 24.dp, bottom = 24.dp),
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


