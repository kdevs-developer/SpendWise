package com.kdev.spendwise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- PREMIUM WALLET SELECTION CARD ---
@Composable
fun PremiumWalletSelectionCard(
    bankName: String,
    cardName: String,
    last4: String,
    colorStart: Color,
    colorEnd: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val brush = Brush.linearGradient(colors = listOf(colorStart, colorEnd))
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderWidth = if (isSelected) 3.dp else 0.dp

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
        ) {
            // Subtle Pattern
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.05f)))

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = bankName,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = cardName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (last4.isNotEmpty()) {
                        Text(
                            text = "**** $last4",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

// --- ANALYSIS OPTION TAB (Expense/Income Switcher) ---
@Composable
fun AnalysisOptionTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.background else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Gray
        )
    }
}

// --- VERTICAL DIVIDER ---
@Composable
fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight(0.6f)
            .background(Color.Gray.copy(0.3f))
    )
}

// --- PROGRESS BAR COLOR LOGIC ---
fun getProgressColor(percentage: Float, isExpense: Boolean): Color {
    if (!isExpense) return Color(0xFF388E3C) // Green for Income

    return when {
        percentage > 0.5f -> Color(0xFFE53935) // Red
        percentage > 0.20f -> Color(0xFFFB8C00) // Orange
        else -> Color(0xFF43A047) // Green
    }
}