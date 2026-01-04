package com.kdev.spendwise

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kdev.spendwise.ui.MainViewModel
import com.kdev.spendwise.ui.screens.*
import com.kdev.spendwise.ui.theme.SpendWiseTheme
import com.kdev.spendwise.util.BiometricUtils

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val systemSplash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel()

            // Splash State
            var showComposeSplash by remember { mutableStateOf(true) }

            // Biometric State: Default to authenticated if biometrics are disabled
            var isBiometricAuthenticated by remember { mutableStateOf(!viewModel.isBiometricEnabled) }
            val isCheckingAuth = viewModel.isCheckingAuthState

            // 1. Trigger Biometric Prompt (Login)
            LaunchedEffect(viewModel.isLoggedIn, viewModel.isBiometricEnabled) {
                if (viewModel.isLoggedIn && viewModel.isBiometricEnabled && !isBiometricAuthenticated) {
                    if (BiometricUtils.isBiometricAvailable(this@MainActivity)) {
                        BiometricUtils.showBiometricPrompt(
                            activity = this@MainActivity,
                            onSuccess = { isBiometricAuthenticated = true },
                            onFailure = { finish() } // Exit if failed/cancelled
                        )
                    } else {
                        isBiometricAuthenticated = true // Fallback
                    }
                } else if (!viewModel.isBiometricEnabled) {
                    isBiometricAuthenticated = true
                }
            }

            // 2. Reset Biometric State on Logout
            LaunchedEffect(viewModel.isLoggedIn) {
                if (!viewModel.isLoggedIn) {
                    isBiometricAuthenticated = !viewModel.isBiometricEnabled
                }
            }

            SpendWiseTheme(darkTheme = viewModel.isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Show Splash until Auth is checked AND Biometric (if required) is done
                    if (showComposeSplash || isCheckingAuth || (viewModel.isLoggedIn && !isBiometricAuthenticated)) {
                        SplashScreen(onSplashFinished = { showComposeSplash = false })
                    } else {

                        if (viewModel.showOnboarding) {
                            OnboardingScreen(onGetStarted = { viewModel.completeOnboarding() })
                        } else if (!viewModel.isLoggedIn) {
                            // --- AUTH FLOW ---
                            var authScreenState by rememberSaveable { mutableStateOf("login") }

                            when (authScreenState) {
                                "login" -> LoginScreen(
                                    viewModel = viewModel,
                                    onStartRegistration = { authScreenState = "registration_flow" },
                                    onLoginSuccess = { /* State change handles navigation */ }
                                )
                                "registration_flow" -> RegistrationFlow(
                                    viewModel = viewModel,
                                    onComplete = {
                                        // Do nothing. isLoggedIn = true triggers navigation automatically.
                                    }
                                )
                            }
                        } else {
                            // --- DASHBOARD FLOW ---
                            var currentScreen by rememberSaveable { mutableStateOf("dashboard") }

                            // Handle Back Press to always go to Dashboard first
                            BackHandler(enabled = currentScreen != "dashboard") {
                                currentScreen = "dashboard"
                            }

                            Box(modifier = Modifier.fillMaxSize()) {
                                when (currentScreen) {
                                    "dashboard" -> DashboardScreen(
                                        viewModel = viewModel,
                                        onShowMoreClick = { currentScreen = "all_transactions" },
                                        onShowWalletsClick = { currentScreen = "wallet_list" }, // Navigate to List
                                        onEditCardClick = { currentScreen = "edit_card" },      // Navigate to Create New
                                        onShowAnalysisClick = { currentScreen = "detailed_analysis" },
                                        onAddTransactionClick = {
                                            viewModel.clearEditState()
                                            currentScreen = "add_transaction"
                                        },
                                        onProfileClick = { currentScreen = "profile" },
                                        onRecurringClick = { currentScreen = "recurring_transactions" }
                                    )

                                    // --- NEW: Wallet Management List ---
                                    "wallet_list" -> WalletListScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = "dashboard" },
                                        onAddWallet = {
                                            viewModel.walletToEdit = null
                                            currentScreen = "edit_card"
                                        },
                                        onEditWallet = {
                                            // viewModel.walletToEdit is set inside the list item click
                                            currentScreen = "edit_card"
                                        }
                                    )

                                    "profile" -> ProfileScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = "dashboard" }
                                    )
                                    "add_transaction" -> AddTransactionScreen(
                                        viewModel = viewModel,
                                        onSaveSuccess = { currentScreen = "dashboard" },
                                        onBack = { currentScreen = "dashboard" }
                                    )
                                    "all_transactions" -> AllTransactionsScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = "dashboard" },
                                        onEditNavigate = { currentScreen = "add_transaction" }
                                    )
                                    "edit_card" -> EditCardScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = "dashboard" } // Goes back to Dashboard usually
                                    )
                                    "detailed_analysis" -> DetailedAnalysisScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = "dashboard" }
                                    )
                                    "recurring_transactions" -> RecurringTransactionsScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = "dashboard" }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}