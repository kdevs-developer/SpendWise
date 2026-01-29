package com.kdev.spendwise.util

import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricUtils {

    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        // UPDATED: Allow both Strong (Fingerprint/Iris) and Weak (Face Unlock)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // If user cancels or locks out, we treat it as failure
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                    onFailure()
                }
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Let the system handle the retry UI, but we can notify briefly
                // Note: Frequent toasts here can be annoying, so we keep it minimal
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("SpendWise Security")
            .setSubtitle("Verify your identity to access")
            .setNegativeButtonText("Log Out") // If they can't unlock, they should log out
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        biometricPrompt.authenticate(promptInfo)
    }
}