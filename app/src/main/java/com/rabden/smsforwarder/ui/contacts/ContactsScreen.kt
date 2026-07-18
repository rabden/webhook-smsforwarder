package com.rabden.smsforwarder.ui.contacts

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rabden.smsforwarder.ui.components.ListCard
import com.rabden.smsforwarder.ui.theme.*
import java.util.Locale

@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel,
    autoOpenAddDialog: Boolean = false
) {
    val customContacts by viewModel.customContacts.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showPickContactDialog by remember { mutableStateOf(false) }
    
    var showVerifyDialog by remember { mutableStateOf(false) }
    var pendingNumber by remember { mutableStateOf("") }
    var suggestedNumber by remember { mutableStateOf("") }
    var selectedContact by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current

    LaunchedEffect(autoOpenAddDialog) {
        if (autoOpenAddDialog) showAddDialog = true
    }

    fun handleAddRequest(number: String) {
        val trimmed = number.trim()
        if (trimmed.isBlank()) return

        if (trimmed.any { it.isLetter() }) {
            viewModel.addContact(trimmed)
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
            viewModel.addContact(cleanNumber)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (customContacts.isEmpty()) {
                    ListCard(
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No numbers added", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    val customList = customContacts.toList()
                    customList.forEachIndexed { index, number ->
                        val shape = when {
                            customList.size == 1 -> RoundedCornerShape(28.dp)
                            index == 0 -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                            index == customList.size - 1 -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
                            else -> RoundedCornerShape(8.dp)
                        }
                        CustomContactItem(
                            number = number,
                            shape = shape,
                            onClick = { selectedContact = number }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp))
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Contact")
        }
    }

    if (selectedContact != null) {
        ContactDetailDialog(
            contactNumber = selectedContact!!,
            onDismiss = { selectedContact = null },
            onDelete = { 
                viewModel.removeContact(it)
                selectedContact = null
            },
            onUpdate = { old, new ->
                viewModel.removeContact(old)
                viewModel.addContact(new)
                selectedContact = null
            }
        )
    }

    if (showAddDialog) {
        var phoneNumber by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Number") },
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
                        viewModel.addContact(pendingNumber)
                        showVerifyDialog = false
                    }) {
                        Text("No, add as is")
                    }
                    Button(onClick = {
                        viewModel.addContact(suggestedNumber)
                        showVerifyDialog = false
                    }) {
                        Text("Yes, use this")
                    }
                }
            }
        )
    }

    if (showPickContactDialog) {
        LaunchedEffect(Unit) {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            pickContactLauncher.launch(intent)
            showPickContactDialog = false
        }
    }
}

@Composable
fun CustomContactItem(number: String, shape: Shape, onClick: () -> Unit) {
    ListCard(
        onClick = onClick,
        shape = shape
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
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

private val countryPrefixMap = mapOf(
    "BD" to "+880", "US" to "+1", "CA" to "+1", "GB" to "+44", "IN" to "+91"
)

private fun getCountryPrefix(): String {
    val country = Locale.getDefault().country.uppercase()
    return countryPrefixMap[country] ?: "+1"
}
