package com.rabden.smsforwarder.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive Filter Mode Toggle with spring physics.
 * Features:
 * - Fluid spring-driven weight and width morphing with physical overshoot
 * - Elastic corner radius transitions
 * - Interactive press-squash and release-rebound dynamics
 * - Springy icon scale bounce on selection
 * - Smooth color interpolation
 * - Tactile haptic feedback
 */
@Composable
fun ExpressiveFilterModeToggle(
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    showDescription: Boolean = true
) {
    val view = LocalView.current
    val isWhitelist = selectedMode.equals("whitelist", ignoreCase = true)

    // Expressive Spring Specs
    val bouncySpringFloat = spring<Float>(
        dampingRatio = 0.58f,
        stiffness = 380f
    )
    val bouncySpringDp = spring<Dp>(
        dampingRatio = 0.58f,
        stiffness = 380f
    )
    val pressSpringFloat = spring<Float>(
        dampingRatio = 0.52f,
        stiffness = 500f
    )

    // Dynamic weights with springy overshoot
    val whitelistWeight by animateFloatAsState(
        targetValue = if (isWhitelist) 1.35f else 1.0f,
        animationSpec = bouncySpringFloat,
        label = "whitelistWeight"
    )
    val blacklistWeight by animateFloatAsState(
        targetValue = if (!isWhitelist) 1.35f else 1.0f,
        animationSpec = bouncySpringFloat,
        label = "blacklistWeight"
    )

    // Morphing inner corner radii
    val whitelistInnerCorner by animateDpAsState(
        targetValue = if (isWhitelist) 28.dp else 10.dp,
        animationSpec = bouncySpringDp,
        label = "whitelistInnerCorner"
    )
    val blacklistInnerCorner by animateDpAsState(
        targetValue = if (!isWhitelist) 28.dp else 10.dp,
        animationSpec = bouncySpringDp,
        label = "blacklistInnerCorner"
    )

    // Active scale pop
    val whitelistActiveScale by animateFloatAsState(
        targetValue = if (isWhitelist) 1.02f else 0.98f,
        animationSpec = bouncySpringFloat,
        label = "whitelistActiveScale"
    )
    val blacklistActiveScale by animateFloatAsState(
        targetValue = if (!isWhitelist) 1.02f else 0.98f,
        animationSpec = bouncySpringFloat,
        label = "blacklistActiveScale"
    )

    // Icon scale bounce
    val whitelistIconScale by animateFloatAsState(
        targetValue = if (isWhitelist) 1.15f else 0.88f,
        animationSpec = bouncySpringFloat,
        label = "whitelistIconScale"
    )
    val blacklistIconScale by animateFloatAsState(
        targetValue = if (!isWhitelist) 1.15f else 0.88f,
        animationSpec = bouncySpringFloat,
        label = "blacklistIconScale"
    )

    // Colors
    val whitelistContainerColor by animateColorAsState(
        targetValue = if (isWhitelist) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = spring(stiffness = 600f),
        label = "whitelistContainerColor"
    )
    val blacklistContainerColor by animateColorAsState(
        targetValue = if (!isWhitelist) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = spring(stiffness = 600f),
        label = "blacklistContainerColor"
    )

    val whitelistContentColor by animateColorAsState(
        targetValue = if (isWhitelist) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = 600f),
        label = "whitelistContentColor"
    )
    val blacklistContentColor by animateColorAsState(
        targetValue = if (!isWhitelist) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = 600f),
        label = "blacklistContentColor"
    )

    // Interaction sources for touch-press squash physics
    val whitelistInteractionSource = remember { MutableInteractionSource() }
    val blacklistInteractionSource = remember { MutableInteractionSource() }
    val isWhitelistPressed by whitelistInteractionSource.collectIsPressedAsState()
    val isBlacklistPressed by blacklistInteractionSource.collectIsPressedAsState()

    val whitelistPressScale by animateFloatAsState(
        targetValue = if (isWhitelistPressed) 0.93f else 1.0f,
        animationSpec = pressSpringFloat,
        label = "whitelistPressScale"
    )
    val blacklistPressScale by animateFloatAsState(
        targetValue = if (isBlacklistPressed) 0.93f else 1.0f,
        animationSpec = pressSpringFloat,
        label = "blacklistPressScale"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Connected springy tabs row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Whitelist Tab
            ExpressiveTabCard(
                title = "Whitelist",
                icon = Icons.Default.CheckCircle,
                weight = whitelistWeight,
                shape = RoundedCornerShape(
                    topStart = 28.dp,
                    topEnd = whitelistInnerCorner,
                    bottomStart = 28.dp,
                    bottomEnd = whitelistInnerCorner
                ),
                containerColor = whitelistContainerColor,
                contentColor = whitelistContentColor,
                scale = whitelistActiveScale * whitelistPressScale,
                iconScale = whitelistIconScale,
                isSelected = isWhitelist,
                interactionSource = whitelistInteractionSource,
                onClick = {
                    if (!isWhitelist) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onModeSelected("whitelist")
                    }
                }
            )

            // Blacklist Tab
            ExpressiveTabCard(
                title = "Blacklist",
                icon = Icons.Default.Block,
                weight = blacklistWeight,
                shape = RoundedCornerShape(
                    topStart = blacklistInnerCorner,
                    topEnd = 28.dp,
                    bottomStart = blacklistInnerCorner,
                    bottomEnd = 28.dp
                ),
                containerColor = blacklistContainerColor,
                contentColor = blacklistContentColor,
                scale = blacklistActiveScale * blacklistPressScale,
                iconScale = blacklistIconScale,
                isSelected = !isWhitelist,
                interactionSource = blacklistInteractionSource,
                onClick = {
                    if (isWhitelist) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onModeSelected("blacklist")
                    }
                }
            )
        }

        // Optional Expressive animated description card
        if (showDescription) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                AnimatedContent(
                    targetState = isWhitelist,
                    transitionSpec = {
                        (fadeIn(animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f)) +
                                slideInVertically(animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f)) { it / 3 })
                            .togetherWith(
                                fadeOut(animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f)) +
                                        slideOutVertically(animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f)) { -it / 3 }
                            )
                    },
                    label = "filterModeDesc"
                ) { whitelistActive ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (whitelistActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (whitelistActive) Icons.Default.FilterAlt else Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (whitelistActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = if (whitelistActive)
                                "Only incoming SMS from contacts in the whitelist will be forwarded."
                            else
                                "All incoming SMS will be forwarded, except from numbers in the blacklist.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.ExpressiveTabCard(
    title: String,
    icon: ImageVector,
    weight: Float,
    shape: androidx.compose.ui.graphics.Shape,
    containerColor: Color,
    contentColor: Color,
    scale: Float,
    iconScale: Float,
    isSelected: Boolean,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .weight(weight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = shape,
        color = containerColor,
        tonalElevation = if (isSelected) 3.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
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
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}
