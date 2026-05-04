package com.flipverse.chat.components
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.*
import kotlin.random.Random

// WhatsApp-like colors for light theme
interface ChatColors {
    val backgroundBase: Color
    val patternPrimary: Color
    val patternSecondary: Color
    val patternAccent: Color
}

object WhatsAppColors : ChatColors {
    override val backgroundBase = Color(0xFFE5DDD5)  // WhatsApp light background
    override val patternPrimary = Color(0xFFD9D0C7)  // Slightly darker for pattern
    override val patternSecondary = Color(0xFFEFEAE2) // Lighter for contrast
    override val patternAccent = Color(0xFFCEC5BC)    // Even darker for depth
}

// WhatsApp-like colors for dark theme
object WhatsAppDarkColors : ChatColors {
    override val backgroundBase = Color(0xFF0B141A)   // WhatsApp dark background
    override val patternPrimary = Color(0xFF1F2C34)   // Lighter for pattern visibility
    override val patternSecondary = Color(0xFF182229) // Medium tone
    override val patternAccent = Color(0xFF253842)    // Accent for depth
}

@Composable
fun ChatBackground(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false
) {
    val density = LocalDensity.current
    val colors = if (isDarkTheme) WhatsAppDarkColors else WhatsAppColors

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundBase)
    ) {
        // Draw the WhatsApp-like pattern
        drawWhatsAppPattern(
            size = size,
            density = density.density,
            colors = colors
        )
    }
}

private fun DrawScope.drawWhatsAppPattern(
    size: Size,
    density: Float,
    colors: ChatColors
) {
    val patternSize = (40 * density).toInt()
    val spacing = (60 * density).toInt()

    // Create a subtle geometric pattern similar to WhatsApp
    for (y in 0 until size.height.toInt() step spacing) {
        for (x in 0 until size.width.toInt() step spacing) {
            val offsetX = x + (y / spacing % 2) * (spacing / 2) // Offset every other row

            // Draw subtle geometric shapes
            drawGeometricShape(
                center = Offset(offsetX.toFloat(), y.toFloat()),
                size = patternSize.toFloat(),
                shapeType = (x + y) % 3,
                colors = colors
            )
        }
    }

    // Add some random subtle dots for texture
    val random = Random(42) // Fixed seed for consistent pattern
    repeat(100) {
        val x = random.nextFloat() * size.width
        val y = random.nextFloat() * size.height
        val alpha = random.nextFloat() * 0.1f + 0.02f
        val accentColor = colors.patternAccent

        drawCircle(
            color = accentColor.copy(alpha = alpha),
            radius = random.nextFloat() * 3f + 1f,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawGeometricShape(
    center: Offset,
    size: Float,
    shapeType: Int,
    colors: ChatColors
) {
    val alpha = 0.08f
    val halfSize = size / 2f

    when (shapeType) {
        0 -> {
            // Draw subtle hexagon
            val hexPath = Path().apply {
                val angles = (0..5).map { it * 60f * PI / 180f }
                angles.forEachIndexed { index, angle ->
                    val x = center.x + cos(angle) * halfSize * 0.6f
                    val y = center.y + sin(angle) * halfSize * 0.6f
                    if (index == 0) moveTo(x.toFloat(), y.toFloat())
                    else lineTo(x.toFloat(), y.toFloat())
                }
                close()
            }
            val primaryColor = colors.patternPrimary
            drawPath(
                path = hexPath,
                color = primaryColor.copy(alpha = alpha)
            )
        }
        1 -> {
            // Draw subtle circle
            val secondaryColor = colors.patternSecondary
            drawCircle(
                color = secondaryColor.copy(alpha = alpha),
                radius = halfSize * 0.4f,
                center = center
            )
        }
        2 -> {
            // Draw subtle diamond
            val diamondPath = Path().apply {
                moveTo(center.x, center.y - halfSize * 0.5f)
                lineTo(center.x + halfSize * 0.5f, center.y)
                lineTo(center.x, center.y + halfSize * 0.5f)
                lineTo(center.x - halfSize * 0.5f, center.y)
                close()
            }
            val accentColor = colors.patternAccent
            drawPath(
                path = diamondPath,
                color = accentColor.copy(alpha = alpha)
            )
        }
    }
}

// Alternative more complex pattern
@Composable
fun ChatBackgroundComplex(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false
) {
    val density = LocalDensity.current
    val colors = if (isDarkTheme) WhatsAppDarkColors else WhatsAppColors

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundBase)
    ) {
        drawComplexWhatsAppPattern(
            size = size,
            density = density.density,
            colors = colors
        )
    }
}

private fun DrawScope.drawComplexWhatsAppPattern(
    size: Size,
    density: Float,
    colors: ChatColors
) {
    // Base layer - subtle gradient overlay
    val gradientColors = listOf(
        colors.patternSecondary.copy(alpha = 0.3f),
        Color.Transparent
    )

    drawRect(
        brush = Brush.radialGradient(
            colors = gradientColors,
            center = Offset(size.width * 0.3f, size.height * 0.2f),
            radius = size.width * 0.8f
        ),
        size = size
    )

    // Pattern layer
    val cellSize = (80 * density).toInt()
    val random = Random(123)

    for (row in 0 until (size.height / cellSize).toInt() + 1) {
        for (col in 0 until (size.width / cellSize).toInt() + 1) {
            val x = col * cellSize.toFloat() + (row % 2) * cellSize * 0.5f
            val y = row * cellSize.toFloat()

            // Vary the pattern based on position
            val variation = (row + col) % 4
            val alpha = (random.nextFloat() * 0.05f + 0.02f)

            when (variation) {
                0 -> drawFloralElement(Offset(x, y), cellSize * 0.6f, alpha, colors)
                1 -> drawGeometricLines(Offset(x, y), cellSize * 0.4f, alpha, colors)
                2 -> drawDottedCircle(Offset(x, y), cellSize * 0.3f, alpha, colors)
                3 -> drawSubtleWave(Offset(x, y), cellSize * 0.5f, alpha, colors)
            }
        }
    }
}

private fun DrawScope.drawFloralElement(
    center: Offset,
    size: Float,
    alpha: Float,
    colors: ChatColors
) {
    val petalCount = 6
    val primaryColor = colors.patternPrimary

    repeat(petalCount) { i ->
        val angle = i * 2f * PI / petalCount
        val petalCenter = Offset(
            (center.x + cos(angle) * size * 0.3f).toFloat(),
            (center.y + sin(angle) * size * 0.3f).toFloat()
        )
        drawCircle(
            color = primaryColor.copy(alpha = alpha),
            radius = size * 0.15f,
            center = petalCenter.toOffset()
        )
    }
}

private fun DrawScope.drawGeometricLines(
    center: Offset,
    size: Float,
    alpha: Float,
    colors: ChatColors
) {
    val accentColor = colors.patternAccent

    repeat(3) { i ->
        val angle = i * PI / 3
        val start = Offset(
            (center.x - cos(angle) * size * 0.5f).toFloat(),
            (center.y - sin(angle) * size * 0.5f).toFloat()
        )
        val end = Offset(
            (center.x + cos(angle) * size * 0.5f).toFloat(),
            (center.y + sin(angle) * size * 0.5f).toFloat()
        )

        drawLine(
            color = accentColor.copy(alpha = alpha),
            start = start.toOffset(),
            end = end.toOffset(),
            strokeWidth = 1.dp.toPx()
        )
    }
}

private fun DrawScope.drawDottedCircle(
    center: Offset,
    radius: Float,
    alpha: Float,
    colors: ChatColors
) {
    val secondaryColor = colors.patternSecondary

    val dotCount = 8
    repeat(dotCount) { i ->
        val angle = i * 2f * PI / dotCount
        val dotCenter = Offset(
            (center.x + cos(angle) * radius).toFloat(),
            (center.y + sin(angle) * radius).toFloat()
        )
        drawCircle(
            color = secondaryColor.copy(alpha = alpha),
            radius = 2.dp.toPx(),
            center = dotCenter.toOffset()
        )
    }
}

private fun DrawScope.drawSubtleWave(
    center: Offset,
    width: Float,
    alpha: Float,
    colors: ChatColors
) {
    val primaryColor = colors.patternPrimary

    val wavePath = Path().apply {
        val startX = center.x - width
        val endX = center.x + width
        val waveHeight = width * 0.2f

        moveTo(startX, center.y)

        val steps = 20
        for (i in 0..steps) {
            val x = startX + (endX - startX) * i / steps
            val y = center.y + sin(i * PI * 2 / steps) * waveHeight
            lineTo(x, y.toFloat())
        }
    }

    drawPath(
        path = wavePath,
        color = primaryColor.copy(alpha = alpha),
        style = Stroke(width = 1.dp.toPx())
    )
}

// Extension function to convert Offset to Offset (for type consistency)
private fun Offset.toOffset(): Offset = this
