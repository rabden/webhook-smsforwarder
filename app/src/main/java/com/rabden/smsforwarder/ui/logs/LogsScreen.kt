package com.rabden.smsforwarder.ui.logs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rabden.smsforwarder.data.MessageLog
import com.rabden.smsforwarder.util.formatTimestamp

fun groupShape(index: Int, total: Int): Shape = when {
    total == 1 -> RoundedCornerShape(28.dp)
    index == 0 -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
    index == total - 1 -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
    else -> RoundedCornerShape(8.dp)
}

@Composable
fun LogsScreen(
    viewModel: LogsViewModel,
    hasWebhookUrl: Boolean,
    hasContacts: Boolean,
    onConfigureWebhook: () -> Unit,
    onAddContact: () -> Unit,
    contentTopPadding: Dp = 0.dp,
    listState: LazyListState = rememberLazyListState()
) {
    val logs by viewModel.logs.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val isSelectionMode = selectedIds.isNotEmpty()
    var selectedLog by remember { mutableStateOf<MessageLog?>(null) }

    val showWebhookPrompt = !hasWebhookUrl && !isSelectionMode
    val showContactsPrompt = !hasContacts && !isSelectionMode
    val promptCount = (if (showWebhookPrompt) 1 else 0) + (if (showContactsPrompt) 1 else 0)
    val totalCount = promptCount + logs.size

    Box(modifier = Modifier.fillMaxSize()) {
        if (logs.isEmpty() && promptCount == 0) {
            NoMessagesState(contentTopPadding)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(
                    start = 10.dp,
                    end = 10.dp,
                    top = contentTopPadding + 8.dp,
                    bottom = 88.dp
                ),
                verticalArrangement = if (logs.isEmpty())
                    Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
                else
                    Arrangement.spacedBy(2.dp)
            ) {
                var pos = 0
                if (showWebhookPrompt) {
                    val shape = groupShape(pos, totalCount)
                    item(key = "prompt_webhook") {
                        PromptCard(
                            icon = Icons.Default.Link,
                            title = "No webhook URL configured",
                            message = "Add a webhook destination URL in settings to start forwarding messages.",
                            ctaText = "Configure",
                            shape = shape,
                            onCtaClick = onConfigureWebhook
                        )
                    }
                    pos++
                }
                if (showContactsPrompt) {
                    val shape = groupShape(pos, totalCount)
                    item(key = "prompt_contacts") {
                        PromptCard(
                            icon = Icons.Default.Contacts,
                            title = "No contacts configured",
                            message = "Add contacts to filter which messages are forwarded.",
                            ctaText = "Add",
                            shape = shape,
                            onCtaClick = onAddContact
                        )
                    }
                    pos++
                }

                itemsIndexed(logs, key = { _, log -> log.id }) { index, log ->
                    val shape = groupShape(promptCount + index, totalCount)
                    val isSelected = log.id in selectedIds
                    LogItem(
                        log = log,
                        shape = shape,
                        isSelected = isSelected,
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
            onDismiss = { selectedLog = null },
            onRetry = {
                viewModel.retry(selectedLog!!)
                selectedLog = null
            }
        )
    }
}

@Composable
fun NoMessagesState(contentTopPadding: Dp) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 10.dp, end = 10.dp, top = contentTopPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
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

@Composable
fun PromptCard(
    icon: ImageVector,
    title: String,
    message: String,
    ctaText: String,
    shape: Shape,
    onCtaClick: () -> Unit
) {
    Card(
        onClick = onCtaClick,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = onCtaClick) {
                    Text(ctaText)
                }
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
            .clip(shape)
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
fun LogDetailDialog(
    log: MessageLog,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (log.status == "FAILED") {
                    OutlinedButton(onClick = onRetry) {
                        Text("Retry")
                    }
                }
                Button(onClick = onDismiss) {
                    Text("Close")
                }
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
