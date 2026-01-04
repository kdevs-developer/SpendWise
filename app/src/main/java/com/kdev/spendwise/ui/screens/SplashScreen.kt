package com.kdev.spendwise.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val scale = remember { Animatable(0.6f) } // Start slightly larger for a "pop" effect
    val alpha = remember { Animatable(0f) }
    val premiumGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF4F5B93), // Start color (Offset 0)
            Color(0xFFE0E4FF)  // End color (Offset 1)
        )
    )

    LaunchedEffect(Unit) {
        // Animate scale and alpha
        scale.animateTo(1f, tween(800))
        alpha.animateTo(1f, tween(800))

        delay(1000) // Brief hold
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer(alpha = 0.99f) // Required for blending
                    .drawWithCache {
                        onDrawWithContent {
                            drawContent()
                            drawRect(
                                brush = premiumGradient,
                                blendMode = BlendMode.SrcAtop
                            )
                        }
                    }
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "SpendWise",
                style = androidx.compose.material3.MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF4F5B93),
                modifier = Modifier.alpha(alpha.value)
            )
        }
    }
}