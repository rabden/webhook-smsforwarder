package com.rabden.smsforwarder.ui.logs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Textsms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rabden.smsforwarder.data.MessageLog
import com.rabden.smsforwarder.ui.components.ListCard
import com.rabden.smsforwarder.util.formatTimestamp

@Composable
fun LogsScreen(
    viewModel: LogsViewModel,
    hasWebhookUrl: Boolean,
    hasContacts: Boolean,
    onConfigureWebhook: () -> Unit,
    onAddContact: () -> Unit
) {
    val logs by viewModel.logs.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val isSelectionMode = selectedIds.isNotEmpty()
    var selectedLog by remember { mutableStateOf<MessageLog?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (logs.isEmpty()) {
            EmptyLogsState(
                hasWebhookUrl = hasWebhookUrl,
                hasContacts = hasContacts,
                onConfigureWebhook = onConfigureWebhook,
                onAddContact = onAddContact
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(logs, key = { _, log -> log.id }) { index, log ->
                    val shape = when {
                        logs.size == 1 -> RoundedCornerShape(28.dp)
                        index == 0 -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                        index == logs.size - 1 -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
                        else -> RoundedCornerShape(8.dp)
                    }
                    LogItem(
                        log = log,
                        shape = shape,
                        isSelected = log.id in selectedIds,
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) viewModel.toggleSelection(log.id)
                            else selectedLog = log
                        },
                        onLongPress = { viewModel.toggleSelection(log.id) }
                    )
                }
            }
        }
    }

    if (selectedLog != null) {
        LogDetailDialog(
            log = selectedLog!!,
            onDismiss = { selectedLog = null }
        )
    }
}

@Composable
fun EmptyLogsState(
    hasWebhookUrl: Boolean,
    hasContacts: Boolean,
    onConfigureWebhook: () -> Unit,
    onAddContact: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        if (!hasWebhookUrl) {
            SetupCard(
                icon = Icons.Default.Link,
                title = "No webhook URL configured",
                message = "Add a webhook destination URL in settings to start forwarding messages.",
                ctaText = "Configure Webhook",
                onCtaClick = onConfigureWebhook
            )
        }
        if (!hasContacts) {
            SetupCard(
                icon = Icons.Default.Contacts,
                title = "No whitelisted contacts",
                message = "Add contacts whose messages should be forwarded.",
                ctaText = "Add Contacts",
                onCtaClick = onAddContact
            )
        }
        if (hasWebhookUrl && hasContacts) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Textsms,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "No messages forwarded yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Forwarded messages will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SetupCard(
    icon: ImageVector,
    title: String,
    message: String,
    ctaText: String,
    onCtaClick: () -> Unit
) {
    ListCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onCtaClick) {
                Text(ctaText)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogItem(
    log: MessageLog,
    shape: Shape,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = log.sender,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formatTimestamp(context, log.timestamp, "MMM dd, yyyy HH:mm:ss", "MMM dd, yyyy hh:mm:ss a"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (isSelectionMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                } else {
                    StatusBadge(status = log.status)
                }
            }

            Text(
                text = log.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 12.dp),
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    Surface(
        color = when (status) {
            "SUCCESS" -> MaterialTheme.colorScheme.primaryContainer
            "PENDING" -> MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.errorContainer
        },
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = when (status) {
                "SUCCESS" -> MaterialTheme.colorScheme.onPrimaryContainer
                "PENDING" -> MaterialTheme.colorScheme.onTertiaryContainer
                else -> MaterialTheme.colorScheme.onErrorContainer
            }
        )
    }
}

@Composable
fun LogDetailDialog(log: MessageLog, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Message Details")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DetailField(label = "Sender", value = log.sender)
                DetailField(label = "Timestamp", value = formatTimestamp(context, log.timestamp, "MMM dd, yyyy HH:mm:ss", "MMM dd, yyyy hh:mm:ss a"))
                DetailField(label = "Status", value = log.status, isStatus = true)

                Column {
                    Text(
                        text = "Full Message",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = log.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DetailField(label: String, value: String, isStatus: Boolean = false) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        if (isStatus) {
            Surface(
                color = when (value) {
                    "SUCCESS" -> MaterialTheme.colorScheme.primaryContainer
                    "PENDING" -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.errorContainer
                },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = value,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = when (value) {
                        "SUCCESS" -> MaterialTheme.colorScheme.onPrimaryContainer
                        "PENDING" -> MaterialTheme.colorScheme.onTertiaryContainer
                        else -> MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
