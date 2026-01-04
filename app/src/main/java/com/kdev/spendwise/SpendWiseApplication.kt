package com.kdev.spendwise

import android.app.Application
import com.google.firebase.FirebaseApp

class SpendWiseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Force initialization of Firebase
        FirebaseApp.initializeApp(this)
    }
}