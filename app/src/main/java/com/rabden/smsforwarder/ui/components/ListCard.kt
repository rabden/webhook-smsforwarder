package com.rabden.smsforwarder.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun ListCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    shape: Shape = RoundedCornerShape(28.dp),
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            onClick = onClick,
            interactionSource = interactionSource ?: remember { MutableInteractionSource() }
        ) {
            Column(modifier = Modifier.padding(20.dp), content = content)
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor)
        ) {
            Column(modifier = Modifier.padding(20.dp), content = content)
        }
    }
}
