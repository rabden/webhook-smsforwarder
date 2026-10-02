package com.rabden.smsforwarder.ui.contacts

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.lerp
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import com.rabden.smsforwarder.ui.components.ListCard
import com.rabden.smsforwarder.ui.components.PromptCard
import com.rabden.smsforwarder.ui.components.ExpressiveFilterModeToggle
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.rabden.smsforwarder.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel,
    contentTopPadding: Dp = 0.dp,
    autoOpenAddDialog: Boolean = false,
    showSettingsSheet: Boolean = false,
    onSettingsSheetDismiss: () -> Unit = {}
) {
    val customContacts by viewModel.customContacts.collectAsState()
    val blacklistContacts by viewModel.blacklistContacts.collectAsState()
    val filterMode by viewModel.filterMode.collectAsState()
    val selectedContacts by viewModel.selectedContacts.collectAsState()
    val selectedBlacklistContacts by viewModel.selectedBlacklistContacts.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    val isCurrentTabSelection = if (selectedTab == 0) selectedContacts.isNotEmpty() else selectedBlacklistContacts.isNotEmpty()
    val currentContacts = if (selectedTab == 0) customContacts else blacklistContacts

    var showAddDialog by remember { mutableStateOf(autoOpenAddDialog) }
    var showPickContactDialog by remember { mutableStateOf(false) }

    var showVerifyDialog by remember { mutableStateOf(false) }
    var pendingNumber by remember { mutableStateOf("") }
    var suggestedNumber by remember { mutableStateOf("") }
    var selectedContact by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val density = LocalDensity.current

    val pagerState = rememberPagerState(pageCount = { 2 }, initialPage = selectedTab)

    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(
                page = selectedTab,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = 380f
                )
            )
        }
    }
    
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            selectedTab = pagerState.currentPage
        }
    }

    fun handleAddRequest(number: String) {
        val trimmed = number.trim()
        if (trimmed.isBlank()) return

        if (trimmed.any { it.isLetter() }) {
            if (selectedTab == 0) viewModel.addContact(trimmed) else viewModel.addBlacklistContact(trimmed)
            return
        }

        val cleanNumber = trimmed.replace(Regex("[^0-9+]"), "")
        if (cleanNumber.isBlank()) return

        if (!cleanNumber.startsWith("+")) {
            if (cleanNumber.length == 11 && cleanNumber.startsWith("01")) {
                pendingNumber = cleanNumber
                suggestedNumber = "+88$cleanNumber"
                showVerifyDialog = true
            } else {
                val prefix = getCountryPrefix()
                pendingNumber = cleanNumber
                suggestedNumber = "$prefix$cleanNumber"
                showVerifyDialog = true
            }
        } else {
            if (selectedTab == 0) viewModel.addContact(cleanNumber) else viewModel.addBlacklistContact(cleanNumber)
        }
    }

    val pickContactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (numberIndex >= 0) {
                            val number = cursor.getString(numberIndex)
                            handleAddRequest(number)
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !isCurrentTabSelection,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageContacts = if (page == 0) customContacts else blacklistContacts
            val isPageSelectionMode = if (page == 0) selectedContacts.isNotEmpty() else selectedBlacklistContacts.isNotEmpty()

            if (pageContacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = 10.dp,
                            end = 10.dp,
                            top = contentTopPadding + 8.dp,
                            bottom = 110.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    PromptCard(
                        icon = if (page == 0) Icons.Default.CheckCircle else Icons.Default.Block,
                        title = if (page == 0) "No whitelisted numbers" else "No blacklisted numbers",
                        message = if (page == 0)
                            "Add contacts to whitelist so messages from trusted senders are forwarded."
                        else
                            "Add numbers to blacklist to block messages from specific senders.",
                        ctaText = "Add",
                        shape = RoundedCornerShape(28.dp),
                        onCtaClick = { showAddDialog = true }
                    )
                }
            } else {
                // Contact list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 10.dp,
                        end = 10.dp,
                        top = contentTopPadding + 8.dp,
                        bottom = 110.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val contactList = pageContacts.toList()
                    itemsIndexed(contactList, key = { _, number -> number }) { index, number ->
                        val shape = when {
                            contactList.size == 1 -> RoundedCornerShape(28.dp)
                            index == 0 -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                            index == contactList.size - 1 -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
                            else -> RoundedCornerShape(8.dp)
                        }
                        val isSelected = if (page == 0) number in selectedContacts else number in selectedBlacklistContacts
                        CustomContactItem(
                            number = number,
                            shape = shape,
                            isSelected = isSelected,
                            isSelectionMode = isPageSelectionMode,
                            onClick = {
                                if (isPageSelectionMode) {
                                    if (page == 0) viewModel.toggleSelection(number) else viewModel.toggleBlacklistSelection(number)
                                } else {
                                    selectedContact = number
                                }
                            },
                            onLongPress = {
                                if (page == 0) viewModel.toggleSelection(number) else viewModel.toggleBlacklistSelection(number)
                            }
                        )
                    }
                }
            }
        }

        // Custom Floating Bottom Bar: Segmented Tabs & Round Add Button
        AnimatedVisibility(
            visible = !isCurrentTabSelection,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            var tabHeight by remember { mutableStateOf(56.dp) }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingTabs(
                    pagerState = pagerState,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        val h = with(density) { coordinates.size.height.toDp() }
                        if (h > 0.dp) tabHeight = h
                    }
                )

                FloatingAddButton(
                    size = tabHeight,
                    onClick = { showAddDialog = true }
                )
            }
        }
    }

    // Contact detail dialog
    if (selectedContact != null) {
        ContactDetailDialog(
            contactNumber = selectedContact!!,
            onDismiss = { selectedContact = null },
            onDelete = {
                if (selectedTab == 0) viewModel.removeContact(it) else viewModel.removeBlacklistContact(it)
                selectedContact = null
            },
            onUpdate = { old, new ->
                if (selectedTab == 0) {
                    viewModel.removeContact(old)
                    viewModel.addContact(new)
                } else {
                    viewModel.removeBlacklistContact(old)
                    viewModel.addBlacklistContact(new)
                }
                selectedContact = null
            }
        )
    }

    // Add contact dialog
    if (showAddDialog) {
        var phoneNumber by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }
        val listLabel = if (selectedTab == 0) "Whitelist" else "Blacklist"

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add to $listLabel") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Phone number",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                        TextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            placeholder = { Text("e.g. 123456789 or SenderID") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            shape = CircleShape,
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                errorIndicatorColor = Color.Transparent
                            )
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            showAddDialog = false
                            showPickContactDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContactPage, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose from contacts")
                    }
                }

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                    Button(onClick = {
                        if (phoneNumber.isNotBlank()) {
                            handleAddRequest(phoneNumber)
                            showAddDialog = false
                        }
                    }) {
                        Text("Add")
                    }
                }
            }
        )
    }

    // Verify number dialog
    if (showVerifyDialog) {
        AlertDialog(
            onDismissRequest = { showVerifyDialog = false },
            title = { Text("Verify Number") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Is this the correct number?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = suggestedNumber,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Adding with country code is recommended for reliability.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        if (selectedTab == 0) viewModel.addContact(pendingNumber) else viewModel.addBlacklistContact(pendingNumber)
                        showVerifyDialog = false
                    }) {
                        Text("No, add as is")
                    }
                    Button(onClick = {
                        if (selectedTab == 0) viewModel.addContact(suggestedNumber) else viewModel.addBlacklistContact(suggestedNumber)
                        showVerifyDialog = false
                    }) {
                        Text("Yes, use this")
                    }
                }
            }
        )
    }

    // Pick contact launcher
    if (showPickContactDialog) {
        LaunchedEffect(Unit) {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            pickContactLauncher.launch(intent)
            showPickContactDialog = false
        }
    }

    // Settings bottom sheet — filter mode
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = onSettingsSheetDismiss,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Filter Mode",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )

                ExpressiveFilterModeToggle(
                    selectedMode = filterMode,
                    onModeSelected = { viewModel.setFilterMode(it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomContactItem(
    number: String,
    shape: Shape,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (isSelectionMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                Text(
                    text = number,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )
            }
            if (!isSelectionMode) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ContactDetailDialog(
    contactNumber: String,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit,
    onUpdate: (String, String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editValue by remember {
        mutableStateOf(TextFieldValue(contactNumber, TextRange(contactNumber.length)))
    }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Contact" else "Contact Info") },
        text = {
            Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.CenterStart) {
                AnimatedContent(
                    targetState = isEditing,
                    transitionSpec = {
                        if (targetState) {
                            (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                        } else {
                            (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    },
                    label = "SwooshTransition"
                ) { targetEditing ->
                    if (targetEditing) {
                        TextField(
                            value = editValue,
                            onValueChange = { editValue = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true,
                            shape = CircleShape,
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                errorIndicatorColor = Color.Transparent
                            )
                        )
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                    } else {
                        Text(
                            text = contactNumber,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isEditing) {
                    TextButton(onClick = { isEditing = false }) {
                        Text("Cancel")
                    }
                    Button(onClick = {
                        if (editValue.text.isNotBlank()) {
                            onUpdate(contactNumber, editValue.text)
                        }
                    }) {
                        Text("Confirm")
                    }
                } else {
                    TextButton(
                        onClick = { onDelete(contactNumber) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete")
                    }
                    Button(onClick = { isEditing = true }) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit")
                    }
                }
            }
        }
    )
}

@Composable
fun FloatingTabs(
    pagerState: androidx.compose.foundation.pager.PagerState,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp,
        modifier = modifier
    ) {
        val tabWidths = remember { mutableStateListOf(0.dp, 0.dp) }
        
        // Calculate real-time interpolated values based on pager scroll fraction
        val fraction = (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
        
        val offset0 = 0.dp
        val offset1 = if (tabWidths[0] > 0.dp) tabWidths[0] + 4.dp else 0.dp
        val indicatorOffset = lerp(offset0, offset1, fraction)
        
        val width0 = tabWidths[0]
        val width1 = if (tabWidths.size > 1) tabWidths[1] else 0.dp
        val indicatorWidth = lerp(width0, width1, fraction)

        // Elastic squash & stretch spring physics during movement
        val stretchFactor = (kotlin.math.sin(fraction * Math.PI.toFloat())).coerceIn(0f, 1f)
        val stretch = (stretchFactor * 22f).dp
        val animatedWidth = indicatorWidth + stretch
        val animatedOffset = indicatorOffset - (stretch / 2)

        Box(
            modifier = Modifier.padding(4.dp).height(IntrinsicSize.Min)
        ) {
            if (animatedWidth > 0.dp) {
                Box(
                    modifier = Modifier
                        .offset(x = animatedOffset)
                        .width(animatedWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FloatingTabItem(
                    selected = pagerState.currentPage == 0 || (pagerState.currentPage == 1 && pagerState.currentPageOffsetFraction < -0.5f),
                    onClick = { onTabSelected(0) },
                    icon = Icons.Default.CheckCircle,
                    label = "Whitelist",
                    onWidthMeasured = { if (tabWidths[0] == 0.dp) tabWidths[0] = it }
                )
                FloatingTabItem(
                    selected = pagerState.currentPage == 1 || (pagerState.currentPage == 0 && pagerState.currentPageOffsetFraction > 0.5f),
                    onClick = { onTabSelected(1) },
                    icon = Icons.Default.Block,
                    label = "Blacklist",
                    onWidthMeasured = { if (tabWidths.size > 1 && tabWidths[1] == 0.dp) tabWidths[1] = it }
                )
            }
        }
    }
}

@Composable
fun FloatingTabItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    onWidthMeasured: (Dp) -> Unit
) {
    val view = LocalView.current
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 600f),
        label = "contentColor"
    )
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tabPressScale"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 0.92f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = 380f
        ),
        label = "tabIconScale"
    )

    Column(
        modifier = Modifier
            .onGloballyPositioned { onWidthMeasured(with(density) { it.size.width.toDp() }) }
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onClick()
                }
            )
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor
        )
    }
}

@Composable
fun FloatingAddButton(
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "addButtonPressScale"
    )

    val iconRotation by animateFloatAsState(
        targetValue = if (isPressed) 90f else 0f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = 380f
        ),
        label = "addButtonIconRotation"
    )

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onClick()
                }
            )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Contact",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        rotationZ = iconRotation
                    }
            )
        }
    }
}

private val countryPrefixMap = mapOf(
    "BD" to "+880", "US" to "+1", "CA" to "+1", "GB" to "+44", "IN" to "+91"
)

private fun getCountryPrefix(): String {
    val country = Locale.getDefault().country.uppercase()
    return countryPrefixMap[country] ?: "+1"
}
