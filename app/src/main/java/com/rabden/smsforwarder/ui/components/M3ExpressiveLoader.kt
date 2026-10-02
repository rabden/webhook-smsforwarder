package com.rabden.smsforwarder.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive Loader.
 * Features an organic breathing circular arc that expands, contracts, and rotates
 * with harmonic phase-shifted spring acceleration, accompanied by a pulsing core dot.
 */
@Composable
fun M3ExpressiveLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    size: Dp = 26.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "m3ExpressiveLoader")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loaderRotation"
    )

    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 45f,
        targetValue = 280f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loaderSweep"
    )

    val corePulseScale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corePulse"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotation }
        ) {
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(
                    width = 2.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        Box(
            modifier = Modifier
                .size(size * 0.28f)
                .graphicsLayer {
                    scaleX = corePulseScale
                    scaleY = corePulseScale
                }
                .clip(CircleShape)
                .background(color)
        )
    }
}
