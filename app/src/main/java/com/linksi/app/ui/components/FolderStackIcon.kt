package com.linksi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.linksi.app.domain.model.Folder

@Composable
fun FolderStackIcon(
    folder: Folder,
    folderLockEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val isLocked = folderLockEnabled && folder.isLocked
    val images = if (isLocked) emptyList() else folder.latestImages

    Box(
        modifier = modifier
            // Use Offscreen compositing to allow BlendMode.Clear to work against the background of the Box
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
        contentAlignment = Alignment.CenterStart
    ) {
        val slots = 3

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart
        ) {
            // Draw from back to front (reverse z-order)
            for (i in (slots - 1) downTo 0) {
                val offset = (i * 14).dp
                val imageUrl = images.getOrNull(i)

                FaviconItem(
                    url = imageUrl,
                    modifier = Modifier
                        .size(34.dp)
                        .offset(x = offset)
                        .zIndex((slots - i).toFloat()),
                    showGap = i < slots - 1
                )
            }
        }
    }
}

@Composable
private fun FaviconItem(
    url: String?,
    modifier: Modifier = Modifier,
    showGap: Boolean = false
) {
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .then(
                if (showGap) {
                    Modifier.drawBehind {
                        // Draw a slightly larger stroke with BlendMode.Clear to create the transparent gap
                        val gapWidth = 6f * density
                        val outline = shape.createOutline(
                            Size(size.width + gapWidth, size.height + gapWidth),
                            layoutDirection,
                            this
                        )
                        val path = Path()
                        when (outline) {
                            is Outline.Rectangle -> path.addRect(outline.rect)
                            is Outline.Rounded -> path.addRoundRect(outline.roundRect)
                            is Outline.Generic -> path.addPath(outline.path)
                        }
                        translate(-gapWidth / 2f, -gapWidth / 2f) {
                            drawPath(
                                path = path,
                                color = Color.Black,
                                blendMode = BlendMode.Clear
                            )
                        }
                    }
                } else Modifier
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)) // Light gray placeholder
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), shape),
        contentAlignment = Alignment.Center
    ) {
        if (url != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.2f),
                contentScale = ContentScale.Crop
            )
        }
    }
}
