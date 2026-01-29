package com.kdev.spendwise.util

import com.kdev.spendwise.data.Expense
import com.kdev.spendwise.data.Wallet
import kotlin.math.abs

object BalanceUtils {

    /**
     * Calculates the "Opening Balance" for a specific date range.
     * This is the sum of all transactions that occurred BEFORE the [startDate].
     *
     * @param allTransactions The full history of transactions (must not be filtered by date yet).
     * @param startDate The start of the period (timestamp).
     * @param selectedWalletId If null, calculates for all wallets. If provided, calculates only for that wallet.
     * @param initialWalletBalances Optional: If you track initial balances separately (usually 0 for fresh apps).
     */
    fun calculateOpeningBalance(
        allTransactions: List<Expense>,
        startDate: Long,
        selectedWalletId: String? = null,
        initialWalletBalances: Double = 0.0
    ): Double {
        // Filter transactions that happened strictly BEFORE the start date
        val pastTransactions = allTransactions.filter { expense ->
            expense.date < startDate &&
                    (selectedWalletId == null || expense.walletId == selectedWalletId || expense.toWalletId == selectedWalletId)
        }

        return initialWalletBalances + pastTransactions.sumOf { tx ->
            calculateTransactionImpact(tx, selectedWalletId)
        }
    }

    /**
     * Calculates the net impact of a single transaction on the balance.
     * Handles logic for Expenses, Income, and Transfers (Source vs Destination).
     */
    fun calculateTransactionImpact(tx: Expense, selectedWalletId: String?): Double {
        val amount = abs(tx.amount)

        return when (tx.type) {
            "INCOME" -> amount
            "EXPENSE" -> -amount
            "TRANSFER" -> {
                if (selectedWalletId == null) {
                    // If viewing "All Accounts", transfers between own accounts are neutral (0 change to net worth)
                    // unless it's a transfer to an external tracked entity, but usually it's 0.
                    0.0
                } else {
                    // Viewing specific wallet
                    if (tx.toWalletId == selectedWalletId) {
                        amount // Incoming Transfer (+ money)
                    } else if (tx.walletId == selectedWalletId) {
                        -amount // Outgoing Transfer (- money)
                    } else {
                        0.0 // Irrelevant transfer
                    }
                }
            }
            else -> 0.0
        }
    }

    /**
     * Calculates the Total Available Balance across all wallets directly from the Wallet objects.
     * This is the source of truth for "Today's Closing Balance".
     */
    fun getCurrentTotalBalance(wallets: List<Wallet>): Double {
        return wallets.sumOf { it.balance }
    }

    /**
     * Helper to verify if the transaction history matches the wallet balance.
     * Useful for debugging "messed up" balances.
     */
    fun verifyBalanceIntegrity(wallets: List<Wallet>, allTransactions: List<Expense>): Boolean {
        val calculatedBalance = allTransactions.sumOf { calculateTransactionImpact(it, null) }
        val actualBalance = getCurrentTotalBalance(wallets)

        // Allow small floating point error
        return abs(calculatedBalance - actualBalance) < 0.01
    }
}