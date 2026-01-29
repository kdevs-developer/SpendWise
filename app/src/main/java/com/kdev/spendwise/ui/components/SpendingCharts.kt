package com.kdev.spendwise.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun AnalyticalPieChart(
    data: Map<String, Double>, // Changed to accept pre-calculated Map
    modifier: Modifier = Modifier
) {
    // Sort big to small for better visualization
    val categoryTotals = remember(data) {
        data.toList().sortedByDescending { it.second }
    }

    val totalAmount = remember(categoryTotals) { categoryTotals.sumOf { it.second } }

    // Aesthetic Color Palette
    val aestheticColors = listOf(
        Color(0xFFEF5350), // Red
        Color(0xFFFFA726), // Orange
        Color(0xFFFFEE58), // Yellow
        Color(0xFF66BB6A), // Green
        Color(0xFF42A5F5), // Blue
        Color(0xFFAB47BC), // Purple
        Color(0xFFEC407A), // Pink
        Color(0xFF26C6DA), // Cyan
        Color(0xFF8D6E63), // Brown
        Color(0xFF78909C), // Blue Grey
        Color(0xFF5C6BC0), // Indigo
        Color(0xFF26A69A), // Teal
        Color(0xFFD4E157), // Lime
        Color(0xFFFF7043), // Deep Orange
        Color(0xFF8D6E63), // Brown
        Color(0xFFBDBDBD), // Grey
        Color(0xFF7E57C2), // Deep Purple
        Color(0xFF29B6F6)  // Light Blue
    )

    // Animation State
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 32.dp.toPx() // Increased slightly for visibility
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            var startAngle = -90f // Start from top

            categoryTotals.forEachIndexed { index, (_, amount) ->
                val sweepAngle = if (totalAmount > 0) {
                    (amount.toFloat() / totalAmount.toFloat()) * 360f
                } else 0f

                // Pick color cyclically
                val sliceColor = aestheticColors[index % aestheticColors.size]

                // Draw Arc
                drawArc(
                    color = sliceColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * animationProgress.value,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )

                startAngle += sweepAngle * animationProgress.value
            }
        }
    }
}