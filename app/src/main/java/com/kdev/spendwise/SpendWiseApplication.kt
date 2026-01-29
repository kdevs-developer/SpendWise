package com.kdev.spendwise

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.kdev.spendwise.worker.RecurringTransactionWorker
import java.util.concurrent.TimeUnit

class SpendWiseApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Firebase
        FirebaseApp.initializeApp(this)

        // 2. Schedule Background Worker
        // Checks every 12 hours to catch up on any missed recurring transactions
        val recurringWorkRequest = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(12, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "RecurringTransactionCheck",
            ExistingPeriodicWorkPolicy.KEEP, // KEEP ensures we don't reset the timer on every app launch
            recurringWorkRequest
        )
    }
}