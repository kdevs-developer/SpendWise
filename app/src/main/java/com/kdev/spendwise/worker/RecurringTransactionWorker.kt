package com.kdev.spendwise.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.kdev.spendwise.data.Expense
import com.kdev.spendwise.data.RecurringRule
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import kotlin.math.abs

class RecurringTransactionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("RecurringWorker", "Starting background check for recurring transactions.")

        // 1. Ensure Firebase is initialized (Critical for background workers)
        if (FirebaseApp.getApps(applicationContext).isEmpty()) {
            FirebaseApp.initializeApp(applicationContext)
        }

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser ?: return Result.success()

        val uid = currentUser.uid
        val now = System.currentTimeMillis()

        try {
            // 2. Fetch all recurring rules
            val rulesSnapshot = db.collection("users").document(uid)
                .collection("recurring_rules")
                .get()
                .await()

            val batch = db.batch()
            var changesMade = false

            for (doc in rulesSnapshot.documents) {
                val rule = doc.toObject(RecurringRule::class.java) ?: continue

                // 3. Check if the rule is due
                // (Assuming 'isActive' defaults to true in data class if not present in Firestore)
                if (rule.nextRunDate <= now) {
                    changesMade = true
                    var currentDateToProcess = rule.nextRunDate
                    val walletRef = db.collection("users").document(uid).collection("wallets").document(rule.walletId)

                    // 4. CATCH-UP LOGIC
                    // Loop to generate ALL missed transactions since the last run
                    while (currentDateToProcess <= now) {
                        Log.d("RecurringWorker", "Processing due transaction: ${rule.title} for date $currentDateToProcess")

                        val newTxRef = db.collection("users").document(uid).collection("transactions").document()
                        val amount = abs(rule.amount)

                        // Determine final amount (Income adds, Expense subtracts)
                        val finalAmount = if (rule.type == "INCOME") amount else -amount

                        val tx = Expense(
                            id = newTxRef.id,
                            amount = finalAmount,
                            title = "Auto: ${rule.title}",
                            category = rule.category,
                            type = rule.type,
                            date = currentDateToProcess,
                            walletId = rule.walletId
                        )

                        // A. Add Transaction
                        batch.set(newTxRef, tx)

                        // B. Update Balance Atomically
                        // FieldValue.increment is safe for multiple updates to the same wallet in one batch
                        batch.update(walletRef, "balance", FieldValue.increment(finalAmount))

                        // C. Calculate next interval
                        currentDateToProcess = calculateNextDate(currentDateToProcess, rule.frequency)
                    }

                    // 5. Update the rule with the new future date
                    batch.update(doc.reference, "nextRunDate", currentDateToProcess)
                }
            }

            // 6. Commit all changes at once
            if (changesMade) {
                batch.commit().await()
                Log.d("RecurringWorker", "Batch committed successfully.")
            }

            return Result.success()

        } catch (e: Exception) {
            Log.e("RecurringWorker", "Error processing rules", e)
            return Result.retry()
        }
    }

    private fun calculateNextDate(current: Long, freq: String): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = current

        // 1. Normalize the string (lowercase, remove extra spaces)
        val cleanFreq = freq.lowercase().trim()

        // 2. Find the Number (Count)
        // Look for any digit sequence. If not found, default to 1.
        val numberRegex = "\\d+".toRegex()
        val match = numberRegex.find(cleanFreq)
        val count = match?.value?.toIntOrNull() ?: 1

        // 3. Determine Unit based on Keywords
        // We check for keywords regardless of where they are in the string
        when {
            "day" in cleanFreq || "daily" in cleanFreq -> {
                cal.add(Calendar.DAY_OF_YEAR, count)
            }
            "week" in cleanFreq || "weekly" in cleanFreq -> {
                cal.add(Calendar.WEEK_OF_YEAR, count)
            }
            "month" in cleanFreq || "monthly" in cleanFreq -> {
                cal.add(Calendar.MONTH, count)
            }
            "year" in cleanFreq || "yearly" in cleanFreq -> {
                cal.add(Calendar.YEAR, count)
            }
            else -> {
                // Only if NO keywords match, fallback to 1 Month
                // Log this error in production if needed
                cal.add(Calendar.MONTH, 1)
            }
        }

        return cal.timeInMillis
    }
}