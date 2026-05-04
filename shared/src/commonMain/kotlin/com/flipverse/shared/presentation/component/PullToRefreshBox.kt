package com.flipverse.shared.presentation.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
fun PullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    refreshThresholdPx: Float = with(LocalDensity.current) { 50.dp.toPx() },
    content: @Composable BoxScope.() -> Unit
) {
    var pullOffset by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            pullOffset = 0f
            isDragging = false
            println("✅ RESET: isRefreshing became false - reset pullOffset and isDragging")
        }
    }

    // Animation for smooth pullOffset transitions
    val animatedPullOffset by animateFloatAsState(
        targetValue = when {
            isRefreshing -> refreshThresholdPx
            isDragging -> pullOffset
            else -> 0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pullOffset",
        finishedListener = {
            // 🔥 CRITICAL: Ensure pullOffset is truly 0 when animation finishes
            if (!isRefreshing && !isDragging) {
                pullOffset = 0f
                println("🎯 Animation finished - pullOffset reset to 0")
            }
        }
    )


    Box(
        modifier = modifier
            .pointerInput(isRefreshing) { // 🔥 CRITICAL: Include isRefreshing as key
                detectDragGestures(
                    onDragStart = { offset ->
                        // Only start dragging if not currently refreshing
                        if (!isRefreshing) {
                            isDragging = true
                            println("🟢 DRAG START - not refreshing, isDragging = true")
                        } else {
                            println("🚫 DRAG START blocked - currently refreshing")
                        }
                    },
                    onDragEnd = {
                        println("🔴 DRAG END - pullOffset: $pullOffset, threshold: $refreshThresholdPx, isRefreshing: $isRefreshing")

                        if (pullOffset >= refreshThresholdPx && !isRefreshing) {
                            println("✅ REFRESH TRIGGERED!")
                            coroutineScope.launch {
                                onRefresh()
                            }
                        } else {
                            println("❌ REFRESH NOT TRIGGERED")
                        }

                        // Always reset dragging state
                        isDragging = false

                        // If not refreshing, reset pull offset immediately
                        if (!isRefreshing) {
                            pullOffset = 0f
                        }
                    }
                ) { _, dragAmount ->
                    // 🔥 CRITICAL: Only process drag if not refreshing
                    if (!isRefreshing && dragAmount.y > 0) {
                        val resistance = 1f - min(pullOffset / (refreshThresholdPx * 2), 0.8f)
                        val newOffset = max(0f, pullOffset + dragAmount.y * resistance * 0.5f)
                        pullOffset = newOffset
                        println("📏 DRAG UPDATE - pullOffset: $pullOffset")
                    } else if (isRefreshing) {
                        println("🚫 DRAG ignored - currently refreshing")
                    }
                }
            }
    ) {
        // Content with offset
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = with(density) { animatedPullOffset.toDp() })
        ) {
            content()
        }

        // Refresh indicator
        if (animatedPullOffset > 0) {
            RefreshIndicator(
                isRefreshing = isRefreshing,
                pullOffset = animatedPullOffset,
                refreshThreshold = refreshThresholdPx,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun RefreshIndicator(
    isRefreshing: Boolean,
    pullOffset: Float,
    refreshThreshold: Float,
    modifier: Modifier = Modifier
) {
    val indicatorSize = 40.dp
    val progress = min(pullOffset / refreshThreshold, 1f)

    Box(
        modifier = modifier
            .size(indicatorSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        if (isRefreshing) {
            AdaptiveCircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            CustomRefreshIndicator(
                progress = progress,
                modifier = Modifier.size(24.dp)
            )
//            Text(
//                text = if (progress >= 1f) "Release!" else "${(progress * 100).toInt()}%",
//                style = MaterialTheme.typography.bodySmall,
//                fontWeight = FontWeight.Medium
//            )
        }
    }
}

@Composable
private fun CustomRefreshIndicator(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val color = MaterialTheme.colorScheme.primary
    val rotation by animateFloatAsState(
        targetValue = progress * 360f,
        animationSpec = tween(300),
        label = "rotation"
    )

    Canvas(
        modifier = modifier.rotate(rotation)
    ) {
        val strokeWidth = 3.dp.toPx()
        val radius = size.minDimension / 2 - strokeWidth / 2
        val center = Offset(size.width / 2, size.height / 2)

        // Draw arc based on progress
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = progress * 270f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round
            )
        )

        // Draw arrow at the end if progress > 0.8
        if (progress > 0.8f) {
            val arrowAngle = (progress * 270f - 90f) * PI / 180f
            val arrowStartX = center.x + cos(arrowAngle).toFloat() * radius
            val arrowStartY = center.y + sin(arrowAngle).toFloat() * radius

            val arrowLength = 8.dp.toPx()
            val arrowAngle1 = arrowAngle + PI - PI/6
            val arrowAngle2 = arrowAngle + PI + PI/6

            // Arrow line 1
            drawLine(
                color = color,
                start = Offset(arrowStartX, arrowStartY),
                end = Offset(
                    arrowStartX + cos(arrowAngle1).toFloat() * arrowLength,
                    arrowStartY + sin(arrowAngle1).toFloat() * arrowLength
                ),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Arrow line 2
            drawLine(
                color = color,
                start = Offset(arrowStartX, arrowStartY),
                end = Offset(
                    arrowStartX + cos(arrowAngle2).toFloat() * arrowLength,
                    arrowStartY + sin(arrowAngle2).toFloat() * arrowLength
                ),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
