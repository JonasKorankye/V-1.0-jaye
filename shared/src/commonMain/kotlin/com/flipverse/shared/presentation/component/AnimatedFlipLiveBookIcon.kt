package com.flipverse.shared.presentation.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * An animated icon component that creates a continuous pulsing/glowing effect
 * for the FlipLiveBook icon in the bottom navigation bar.
 * 
 * Features a vibrant PickledPineapple color badge that surrounds the icon
 * with breathing/glowing animations for maximum attention.
 */
@Composable
fun AnimatedFlipLiveBookIcon(
    icon: DrawableResource,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color.White
) {
    // Vibrant PickledPineapple color - bright and beautiful for both themes
    val badgeColor = Color(0xFFD6E538)
    val accentColor = Color(0xFFFFE55C) // Brighter yellow for highlights
    val deepColor = Color(0xFFB8C630) // Deeper green-yellow for depth
    
    // Create an infinite transition that will run continuously
    val infiniteTransition = rememberInfiniteTransition(label = "flipLiveBookAnimation")
    
    // Pulsing scale animation for the badge
    val badgeScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badgeScaleAnimation"
    )
    
    // Breathing glow effect
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAnimation"
    )
    
    // Outer glow ring scale (expanding ripple effect)
    val outerGlowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = LinearOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outerGlowScale"
    )
    
    // Outer glow alpha (fades in/out)
    val outerGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = LinearOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outerGlowAlpha"
    )
    
    // Rotating shimmer effect
    val shimmerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerRotation"
    )
    
    // Secondary pulse for double-pulse effect
    val secondaryPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "secondaryPulse"
    )
    
    // Color shift for dynamic appearance
    val colorShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2500,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "colorShift"
    )
    
    // Dynamic color based on animation
    val dynamicBadgeColor = androidx.compose.ui.graphics.lerp(badgeColor, accentColor, colorShift * 0.3f)
    
    // Fixed-size container that matches other icons - prevents layout shifts
    Box(
        modifier = modifier
            .size(size)
            .layout { measurable, constraints ->
                // Measure with original constraints
                val placeable = measurable.measure(constraints)
                // Report the same size to parent layout (prevents shifting other items)
                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Outermost expanding ripple (largest)
        Box(
            modifier = Modifier
                .size(size * 2.2f * outerGlowScale)
                .scale(secondaryPulse * 0.9f)
                .background(
                    color = accentColor.copy(alpha = outerGlowAlpha * 0.4f),
                    shape = CircleShape
                )
                .blur(10.dp)
        )
        
        // Outer expanding glow ring (ripple effect)
        Box(
            modifier = Modifier
                .size(size * 2f * outerGlowScale)
                .scale(badgeScale * 0.95f)
                .background(
                    color = dynamicBadgeColor.copy(alpha = outerGlowAlpha * 0.6f),
                    shape = CircleShape
                )
                .blur(8.dp)
        )
        
        // Rotating shimmer ring with gradient
        Box(
            modifier = Modifier
                .size(size * 1.8f)
                .scale(badgeScale)
                .rotate(shimmerRotation)
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0f),
                            accentColor.copy(alpha = 0.8f),
                            Color.White.copy(alpha = 0.9f),
                            accentColor.copy(alpha = 0.8f),
                            accentColor.copy(alpha = 0f)
                        )
                    ),
                    shape = CircleShape
                )
                .blur(2.dp)
        )
        
        // Inner pulsing glow halo
        Box(
            modifier = Modifier
                .size(size * 1.7f)
                .scale(badgeScale)
                .background(
                    color = dynamicBadgeColor.copy(alpha = glowAlpha * 0.7f),
                    shape = CircleShape
                )
                .blur(6.dp)
        )
        
        // Main badge background with gradient
        Box(
            modifier = Modifier
                .size(size * 1.3f)
                .scale(badgeScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.95f),
                            dynamicBadgeColor.copy(alpha = 0.9f)
                        )
                    ),
                    shape = CircleShape
                )
                .blur(1.dp)
        )
        
        // Solid badge core that fully covers the icon with gradient
        Box(
            modifier = Modifier
                .size(size)
                .scale(badgeScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            accentColor,
                            dynamicBadgeColor,
                            deepColor
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // Inner highlight for depth
        Box(
            modifier = Modifier
                .size(size * 0.5f)
                .scale(badgeScale * 1.05f)
                .background(
                    color = Color.White.copy(alpha = glowAlpha * 0.4f),
                    shape = CircleShape
                )
                .blur(4.dp)
        )
        
        // The actual icon at center - drawn on top of badge
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            modifier = Modifier
                .size(size * 0.65f)
                .scale(badgeScale),
            tint = Color.Black.copy(alpha = 0.85f) // Dark icon for contrast
        )
    }
}
