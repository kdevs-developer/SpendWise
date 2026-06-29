package com.kdev.spendwise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kdev.spendwise.data.Expense
import com.kdev.spendwise.data.Wallet
import com.kdev.spendwise.ui.MainViewModel
import com.kdev.spendwise.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue
import androidx.compose.ui.platform.LocalLocale

@Composable
fun TransactionItem(
    expense: Expense,
    viewModel: MainViewModel,
    wallets: List<Wallet>,
    modifier: Modifier = Modifier
) {
    val isTransfer = expense.type == "TRANSFER"
    val isExpense = expense.amount < 0 && !isTransfer

    // Premium Color Logic: Blue for Transfer, Red for Expense, Green for Income
    val amountColor = when {
        isTransfer -> Color(0xFF2196F3)
        isExpense -> Color(0xFFD32F2F)
        else -> Color(0xFF388E3C)
    }

    // Sign Logic: Absolute value is safest for transfers viewing from multiple sides
    val sign = when {
        isTransfer -> ""
        isExpense -> "-"
        else -> "+"
    }

    // Dynamic Wallet Name Resolution
    val sourceWallet = wallets.find { it.id == expense.walletId }
    val destWallet = wallets.find { it.id == expense.toWalletId }
    val sourceName = sourceWallet?.let { it.bankName.ifBlank { it.name } } ?: "Wallet"
    val destName = destWallet?.let { it.bankName.ifBlank { it.name } } ?: "Wallet"

    val bankDisplay = if (isTransfer && destWallet != null) {
        "$sourceName → $destName"
    } else {
        sourceName
    }

    // Category & Icon Resolution
    val displayCategory = if (isTransfer) "Transfer" else expense.category.substringAfter(" -> ")
    val icon = if (isTransfer) Icons.Default.SwapHoriz else viewModel.getIconForCategory(displayCategory)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- 1. PREMIUM ICON ---
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(amountColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = amountColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        // --- 2. HIERARCHICAL DETAILS ---
        Column(modifier = Modifier.weight(1f)) {
            // Row 1: Category / Title
            Text(
                text = displayCategory,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Row 2: Bank Name (With mini icon)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = Color.Gray
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = bankDisplay,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Row 3: Notes (If available & different from category)
            if (expense.title.isNotBlank() && expense.title != displayCategory) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Icon(
                        imageVector = Icons.Default.Notes,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.Gray
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // --- 3. AMOUNT & TIME ---
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$sign ${CurrencyUtils.formatINR(expense.amount.absoluteValue)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = amountColor
            )
            Text(
                text = SimpleDateFormat("hh:mm a", LocalLocale.current.platformLocale).format(Date(expense.date)),
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray
            )
        }
    }
}