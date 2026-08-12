package com.linksi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun SimpleProgressBar(
    progress: Float?,
    modifier: Modifier = Modifier
) {
    if (progress != null) {
        // Custom solid progress bar using Box to avoid any "wavy" effects
        Box(
            modifier = modifier
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    } else {
        // Indeterminate state
        LinearProgressIndicator(
            modifier = modifier
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Butt
        )
    }
}
