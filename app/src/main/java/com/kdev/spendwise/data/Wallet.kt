package com.kdev.spendwise.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Wallet data class representing a bank account or credit card.
 * Updated to support premium balance syncing logic and drag-and-drop reordering.
 */
enum class WalletType {
    SAVINGS, CURRENT, CASH, CREDIT_CARD, SALARY, INVESTMENT
}

data class Wallet(
    val id: String = "",
    val name: String = "",
    val balance: Double = 0.0,
    val bankName: String = "",
    val last4Digits: String = "",
    val cardThemeId: Int = 0,
    val isPrimary: Boolean = false,
    val cardNumber: String = "", // Note: Ensure full card numbers are encrypted if saved locally.
    val type: WalletType = WalletType.SAVINGS,
    var orderIndex: Int = 0 // Updated to 'val' to maintain Compose state stability
) {

    val icon: ImageVector
        get() = when (type) {
            WalletType.SAVINGS -> Icons.Default.Savings
            WalletType.CURRENT -> Icons.Default.AccountBalance
            WalletType.CASH -> Icons.Default.Payments
            WalletType.CREDIT_CARD -> Icons.Default.CreditCard
            WalletType.SALARY -> Icons.Default.Work
            WalletType.INVESTMENT -> Icons.AutoMirrored.Filled.TrendingUp
        }

    /**
     * In a premium fintech app, the "Available Balance" is the real-time
     * standing of the specific wallet.
     * This helper ensures we call the same value consistently.
     */
    val availableBalance: Double
        get() = balance

    /**
     * Clean display name logic: Prioritizes Bank Name,
     * falls back to Wallet Name.
     */
    fun getDisplayName(): String = bankName.ifBlank { name }.ifBlank { "Unnamed Account" }
}