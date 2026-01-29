package com.kdev.spendwise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kdev.spendwise.data.Expense
import com.kdev.spendwise.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Composable
fun TransactionItem(
    transaction: Expense,
    walletName: String,
    categoryIcon: ImageVector,
    onClick: () -> Unit
) {
    val isExpense = transaction.type == "EXPENSE"
    val isTransfer = transaction.type == "TRANSFER"

    // Amount Colors
    val amountColor = when {
        isTransfer -> Color.Blue
        isExpense -> Color.Red
        else -> Color(0xFF2E7D32) // Green
    }

    // Icon Background Colors
    val iconBg = when {
        isTransfer -> Color(0xFFE3F2FD) // Light Blue
        isExpense -> Color(0xFFFFEBEE) // Light Red
        else -> Color(0xFFE8F5E9) // Light Green
    }

    // Icon Tint
    val iconTint = when {
        isTransfer -> Color.Blue
        isExpense -> Color(0xFFD32F2F)
        else -> Color(0xFF388E3C)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Title & Bank Name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = walletName, // Showing Bank Name / Wallet Name
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }

            // Amount & Date/Time
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyUtils.formatINR(abs(transaction.amount)),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )

                // Date & Time Format (e.g., "06 Jan, 10:30 AM")
                val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                Text(
                    text = sdf.format(Date(transaction.date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}