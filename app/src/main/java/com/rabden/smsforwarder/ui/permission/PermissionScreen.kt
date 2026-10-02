package com.rabden.smsforwarder.ui.permission

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import com.rabden.smsforwarder.ui.components.ListCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PermissionRequestScreen(onGrantClick: () -> Unit) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val continueInteractionSource = remember { MutableInteractionSource() }
    val isContinuePressed by continueInteractionSource.collectIsPressedAsState()
    var isClickActive by remember { mutableStateOf(false) }

    // Active morph state applies during press or on click
    val isMorphed = isContinuePressed || isClickActive

    // M3 Expressive corner morphing:
    // Both Continue button top corners and the Notifications item bottom corners morph synchronously to 28.dp
    val continueTopCorner by animateDpAsState(
        targetValue = if (isMorphed) 28.dp else 8.dp,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 380f),
        label = "continueTopCorner"
    )
    val notificationsBottomCorner by animateDpAsState(
        targetValue = if (isMorphed) 28.dp else 8.dp,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 380f),
        label = "notificationsBottomCorner"
    )

    // Expressive physical separation gap when morphed
    val separationOffset by animateDpAsState(
        targetValue = if (isMorphed) 6.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 420f),
        label = "separationOffset"
    )

    // M3 Expressive press squash and arrow bounce
    val continuePressScale by animateFloatAsState(
        targetValue = if (isMorphed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 480f),
        label = "continuePressScale"
    )
    val arrowOffset by animateDpAsState(
        targetValue = if (isMorphed) 8.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.50f, stiffness = 420f),
        label = "arrowOffset"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Hero Section with smaller, refined icon
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Setup Forwarding",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "To securely forward your messages to your webhook, we need a few essential permissions.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp, bottom = 28.dp, start = 8.dp, end = 8.dp)
        )

        // Permission Items Grouped with Connected Continue Button
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            PermissionItem(
                icon = Icons.Default.Sms,
                title = "SMS Access",
                description = "To capture incoming SMS and identify the receiving SIM card number.",
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
            )

            PermissionItem(
                icon = Icons.Default.Contacts,
                title = "Contacts",
                description = "To allow you to select trusted senders from your whitelist.",
                shape = RoundedCornerShape(8.dp)
            )

            PermissionItem(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                description = "To keep you informed about the forwarding service status.",
                shape = RoundedCornerShape(
                    topStart = 8.dp,
                    topEnd = 8.dp,
                    bottomStart = notificationsBottomCorner,
                    bottomEnd = notificationsBottomCorner
                )
            )

            // Connected Continue Button with M3 Expressive corner morphing & spring physics
            ListCard(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    if (!isClickActive) {
                        coroutineScope.launch {
                            isClickActive = true
                            delay(220)
                            onGrantClick()
                            delay(300)
                            isClickActive = false
                        }
                    }
                },
                interactionSource = continueInteractionSource,
                shape = RoundedCornerShape(
                    topStart = continueTopCorner,
                    topEnd = continueTopCorner,
                    bottomStart = 28.dp,
                    bottomEnd = 28.dp
                ),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .offset(y = separationOffset)
                    .graphicsLayer {
                        scaleX = continuePressScale
                        scaleY = continuePressScale
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .size(20.dp)
                                .offset(x = arrowOffset)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String,
    shape: RoundedCornerShape
) {
    ListCard(shape = shape) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }

            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
