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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
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

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
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

    // M3 Expressive Add to Whitelist / Blacklist Drawer
    if (showAddDialog) {
        var phoneNumber by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }
        val listLabel = if (selectedTab == 0) "Whitelist" else "Blacklist"
        val view = LocalView.current
        val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val hasText = phoneNumber.isNotBlank()

        val buttonInteractionSource = remember { MutableInteractionSource() }
        val isButtonPressed by buttonInteractionSource.collectIsPressedAsState()
        val buttonPressScale by animateFloatAsState(
            targetValue = if (isButtonPressed) 0.88f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "actionButtonPressScale"
        )
        val buttonContainerColor by animateColorAsState(
            targetValue = if (hasText) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
            label = "buttonContainerColor"
        )
        val buttonContentColor by animateColorAsState(
            targetValue = if (hasText) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
            label = "buttonContentColor"
        )

        ModalBottomSheet(
            onDismissRequest = { showAddDialog = false },
            sheetState = addSheetState,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Centered Title in Header
                Text(
                    text = "Add to $listLabel",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Input and Contacts/Add button side by side in same row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        placeholder = { Text("e.g. 123456789 or SenderID") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .focusRequester(focusRequester),
                        shape = CircleShape,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            errorIndicatorColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = if (hasText) ImeAction.Done else ImeAction.Default
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (hasText) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    handleAddRequest(phoneNumber)
                                    showAddDialog = false
                                }
                            }
                        )
                    )

                    Surface(
                        shape = CircleShape,
                        color = buttonContainerColor,
                        shadowElevation = if (hasText) 4.dp else 0.dp,
                        modifier = Modifier
                            .height(56.dp)
                            .graphicsLayer {
                                scaleX = buttonPressScale
                                scaleY = buttonPressScale
                            }
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = buttonInteractionSource,
                                indication = null,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    if (hasText) {
                                        handleAddRequest(phoneNumber)
                                        showAddDialog = false
                                    } else {
                                        showAddDialog = false
                                        showPickContactDialog = true
                                    }
                                }
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .animateContentSize(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = hasText,
                                transitionSpec = {
                                    if (targetState) {
                                        (fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.5f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 400f)))
                                            .togetherWith(fadeOut(animationSpec = tween(100)) + scaleOut(targetScale = 0.5f))
                                    } else {
                                        (fadeIn(animationSpec = tween(200, delayMillis = 40)) + scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 400f)))
                                            .togetherWith(fadeOut(animationSpec = tween(100)) + scaleOut(targetScale = 0.8f))
                                    }
                                },
                                label = "actionButtonMorph"
                            ) { isAdd ->
                                if (isAdd) {
                                    Box(
                                        modifier = Modifier.size(56.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Number",
                                            tint = buttonContentColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContactPage,
                                            contentDescription = null,
                                            tint = buttonContentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Contacts",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = buttonContentColor,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
    }

    // M3 Expressive Verify Number Drawer
    if (showVerifyDialog) {
        val view = LocalView.current
        val verifySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val rejectInteractionSource = remember { MutableInteractionSource() }
        val acceptInteractionSource = remember { MutableInteractionSource() }

        val isRejectPressed by rejectInteractionSource.collectIsPressedAsState()
        val rejectEndCorner by animateDpAsState(
            targetValue = if (isRejectPressed) 26.dp else 8.dp,
            animationSpec = spring(dampingRatio = 0.58f, stiffness = 400f),
            label = "rejectCorner"
        )

        val isAcceptPressed by acceptInteractionSource.collectIsPressedAsState()
        val acceptStartCorner by animateDpAsState(
            targetValue = if (isAcceptPressed) 26.dp else 8.dp,
            animationSpec = spring(dampingRatio = 0.58f, stiffness = 400f),
            label = "acceptCorner"
        )

        ModalBottomSheet(
            onDismissRequest = { showVerifyDialog = false },
            sheetState = verifySheetState,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Centered Title
                Text(
                    text = "Verify Number",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Info card with suggested formatted number
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Is this the correct number?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = suggestedNumber,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Adding with country code is recommended for reliability.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // M3 Expressive Button Group
                ButtonGroup(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    FilledTonalButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            if (selectedTab == 0) viewModel.addContact(pendingNumber) else viewModel.addBlacklistContact(pendingNumber)
                            showVerifyDialog = false
                        },
                        interactionSource = rejectInteractionSource,
                        shape = RoundedCornerShape(
                            topStart = 26.dp,
                            bottomStart = 26.dp,
                            topEnd = rejectEndCorner,
                            bottomEnd = rejectEndCorner
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .animateWidth(interactionSource = rejectInteractionSource)
                    ) {
                        Text("No, add as is", fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }

                    Button(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            if (selectedTab == 0) viewModel.addContact(suggestedNumber) else viewModel.addBlacklistContact(suggestedNumber)
                            showVerifyDialog = false
                        },
                        interactionSource = acceptInteractionSource,
                        shape = RoundedCornerShape(
                            topStart = acceptStartCorner,
                            bottomStart = acceptStartCorner,
                            topEnd = 26.dp,
                            bottomEnd = 26.dp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .animateWidth(interactionSource = acceptInteractionSource)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Yes, use this", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
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

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
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
    val view = LocalView.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val cancelInteractionSource = remember { MutableInteractionSource() }
    val confirmInteractionSource = remember { MutableInteractionSource() }
    val deleteInteractionSource = remember { MutableInteractionSource() }
    val editInteractionSource = remember { MutableInteractionSource() }

    val isCancelPressed by cancelInteractionSource.collectIsPressedAsState()
    val cancelEndCorner by animateDpAsState(
        targetValue = if (isCancelPressed) 28.dp else 6.dp,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 400f),
        label = "cancelEndCorner"
    )

    val isConfirmPressed by confirmInteractionSource.collectIsPressedAsState()
    val confirmStartCorner by animateDpAsState(
        targetValue = if (isConfirmPressed) 28.dp else 6.dp,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 400f),
        label = "confirmStartCorner"
    )

    val isDeletePressed by deleteInteractionSource.collectIsPressedAsState()
    val deleteEndCorner by animateDpAsState(
        targetValue = if (isDeletePressed) 28.dp else 6.dp,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 400f),
        label = "deleteEndCorner"
    )

    val isEditPressed by editInteractionSource.collectIsPressedAsState()
    val editStartCorner by animateDpAsState(
        targetValue = if (isEditPressed) 28.dp else 6.dp,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 400f),
        label = "editStartCorner"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Centered Title
            Text(
                text = if (isEditing) "Edit Contact" else "Contact Info",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Input box prefilled + action icon buttons beside it
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    readOnly = !isEditing,
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .focusRequester(focusRequester),
                    shape = CircleShape,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = if (isEditing) ImeAction.Done else ImeAction.Default
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isEditing && editValue.text.isNotBlank()) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onUpdate(contactNumber, editValue.text)
                            }
                        }
                    )
                )

                AnimatedContent(
                    targetState = isEditing,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.7f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 400f)))
                            .togetherWith(fadeOut(animationSpec = tween(100)) + scaleOut(targetScale = 0.7f))
                    },
                    label = "actionButtonsTransition"
                ) { editing ->
                    ButtonGroup(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                        if (editing) {
                            FilledTonalIconButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    editValue = TextFieldValue(contactNumber, TextRange(contactNumber.length))
                                    isEditing = false
                                },
                                interactionSource = cancelInteractionSource,
                                shape = RoundedCornerShape(
                                    topStart = 28.dp,
                                    bottomStart = 28.dp,
                                    topEnd = cancelEndCorner,
                                    bottomEnd = cancelEndCorner
                                ),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .size(56.dp)
                                    .animateWidth(interactionSource = cancelInteractionSource)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel edit",
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            FilledIconButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    if (editValue.text.isNotBlank()) {
                                        onUpdate(contactNumber, editValue.text)
                                    }
                                },
                                interactionSource = confirmInteractionSource,
                                shape = RoundedCornerShape(
                                    topStart = confirmStartCorner,
                                    bottomStart = confirmStartCorner,
                                    topEnd = 28.dp,
                                    bottomEnd = 28.dp
                                ),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .size(56.dp)
                                    .animateWidth(interactionSource = confirmInteractionSource)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Confirm edit",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            FilledTonalIconButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onDelete(contactNumber)
                                },
                                interactionSource = deleteInteractionSource,
                                shape = RoundedCornerShape(
                                    topStart = 28.dp,
                                    bottomStart = 28.dp,
                                    topEnd = deleteEndCorner,
                                    bottomEnd = deleteEndCorner
                                ),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier
                                    .size(56.dp)
                                    .animateWidth(interactionSource = deleteInteractionSource)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete contact",
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            FilledTonalIconButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    editValue = TextFieldValue(contactNumber, TextRange(contactNumber.length))
                                    isEditing = true
                                },
                                interactionSource = editInteractionSource,
                                shape = RoundedCornerShape(
                                    topStart = editStartCorner,
                                    bottomStart = editStartCorner,
                                    topEnd = 28.dp,
                                    bottomEnd = 28.dp
                                ),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier
                                    .size(56.dp)
                                    .animateWidth(interactionSource = editInteractionSource)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit contact",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            LaunchedEffect(isEditing) {
                if (isEditing) {
                    focusRequester.requestFocus()
                }
            }
        }
    }
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
