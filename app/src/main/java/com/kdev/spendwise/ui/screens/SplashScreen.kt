package com.kdev.spendwise.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    // --- STATE & ANIMATABLES ---
    var startAnimation by remember { mutableStateOf(false) }

    // Logo Animations (Spring Physics for "Bouncy" feel)
    val logoScale = remember { Animatable(0f) }
    val logoRotation = remember { Animatable(-45f) }

    // Text Animations (Slide Up + Fade)
    val textAlpha = remember { Animatable(0f) }
    val textOffset = remember { Animatable(50f) } // Start 50px lower

    // Tagline Animation (Delayed)
    val taglineAlpha = remember { Animatable(0f) }

    // Background Pulse
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val circleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    // Premium Colors
    val primaryColor = Color(0xFF4F5B93) // Deep Blue
    val secondaryColor = Color(0xFF8E99F3) // Light Lavender
    val gradientBrush = Brush.linearGradient(
        colors = listOf(primaryColor, secondaryColor),
        start = Offset(0f, 0f),
        end = Offset(100f, 100f)
    )

    // --- ORCHESTRATION ---
    LaunchedEffect(Unit) {
        startAnimation = true

        // 1. Logo Entrance (Parallel Scale + Rotate)
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            logoRotation.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
        }

        delay(300) // Stagger

        // 2. Title Slide Up
        launch {
            textAlpha.animateTo(1f, tween(500))
        }
        launch {
            textOffset.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
        }

        delay(200) // Stagger

        // 3. Tagline Fade In
        launch {
            taglineAlpha.animateTo(1f, tween(500))
        }

        delay(1500) // Hold to read
        onSplashFinished()
    }

    // --- UI CONTENT ---
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // [LAYER 1] Animated Background Decorations
            // Faint circle top-left
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = primaryColor.copy(alpha = 0.03f),
                    radius = size.minDimension / 1.5f * circleScale,
                    center = Offset(0f, 0f)
                )
            }
            // Faint circle bottom-right
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = secondaryColor.copy(alpha = 0.05f),
                    radius = size.minDimension / 2f * circleScale,
                    center = Offset(size.width, size.height)
                )
            }

            // [LAYER 2] Main Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 1. ANIMATED LOGO
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(logoScale.value)
                        .graphicsLayer { rotationZ = logoRotation.value }
                        // Soft Shadow
                        .graphicsLayer {
                            shadowElevation = 20.dp.toPx()
                            shape = CircleShape
                            clip = false
                        }
                        .background(Color.White, CircleShape)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(alpha = 0.99f)
                            .drawWithCache {
                                onDrawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = gradientBrush,
                                        blendMode = BlendMode.SrcIn //
                                    )
                                }
                            }
                    )
                }

                Spacer(Modifier.height(32.dp))

                // 2. TITLE (Slide + Fade)
                Text(
                    text = "SpendWise",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryColor,
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = textAlpha.value
                            translationY = textOffset.value
                        }
                )

                Spacer(Modifier.height(8.dp))

                // 3. TAGLINE (Fade only)
                Text(
                    text = "Master your money.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.alpha(taglineAlpha.value)
                )
            }

            // [LAYER 3] Footer / Version (Optional)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .alpha(taglineAlpha.value) // Appears with tagline
            ) {
                Text(
                    text = "v1.0.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )
            }
        }
    }
}