package com.linksi.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

data class CoachMarkTarget(
    val coords: LayoutCoordinates?,
    val title: String,
    val description: String,
    val tooltipBelow: Boolean = true  // show tooltip below or above
)

@Composable
fun SpotlightOverlay(
    target: CoachMarkTarget,
    onNext: () -> Unit,
    isLastStep: Boolean,
    stepNumber: Int,
    totalSteps: Int
) {
    val coords = target.coords ?: return
    val bounds = coords.boundsInRoot()
    val padding = 12.dp
    val density = LocalDensity.current
    val paddingPx = with(density) { padding.toPx() }

    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300),
        label = "overlay_alpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim with cutout
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { /* consume taps outside */ }
                }
        ) {
            val scrimColor = Color.Black.copy(alpha = 0.75f * animatedAlpha)

            // Full scrim
            drawRect(color = scrimColor)

            // Cut out the target area
            val cutoutPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(
                            left = bounds.left - paddingPx,
                            top = bounds.top - paddingPx,
                            right = bounds.right + paddingPx,
                            bottom = bounds.bottom + paddingPx
                        ),
                        cornerRadius = CornerRadius(16.dp.toPx())
                    )
                )
            }
            clipPath(cutoutPath, clipOp = ClipOp.Difference) {
                drawRect(color = scrimColor)
            }

            // Highlight ring around target
            drawRoundRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = Offset(bounds.left - paddingPx, bounds.top - paddingPx),
                size = Size(
                    bounds.width + paddingPx * 2,
                    bounds.height + paddingPx * 2
                ),
                cornerRadius = CornerRadius(16.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }

        // Tooltip card
        val tooltipOffset = with(density) {
            if (target.tooltipBelow) {
                IntOffset(
                    x = 32.dp.roundToPx(),
                    y = (bounds.bottom + paddingPx + 8.dp.toPx()).toInt()
                )
            } else {
                IntOffset(
                    x = 32.dp.roundToPx(),
                    y = (bounds.top - paddingPx - 160.dp.toPx()).toInt()
                )
            }
        }

        Box(modifier = Modifier.offset { tooltipOffset }.padding(end = 32.dp)) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically()
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Step counter
                        Text(
                            "$stepNumber of $totalSteps",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            target.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            target.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = onNext,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isLastStep) "Done 🎉" else "Next")
                                if (!isLastStep) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Outlined.ArrowForward, null, Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}