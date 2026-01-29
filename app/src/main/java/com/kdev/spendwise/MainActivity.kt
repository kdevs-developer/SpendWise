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

            // --- STATE MANAGEMENT ---
            var showComposeSplash by remember { mutableStateOf(true) }

            // Biometrics: Default to authenticated if disabled, otherwise false until proven
            var isBiometricAuthenticated by remember { mutableStateOf(!viewModel.isBiometricEnabled) }
            val isCheckingAuth = viewModel.isCheckingAuthState

            // --- 1. BIOMETRIC LOGIC ---
            // Trigger prompt only when: Logged In + Bio Enabled + Not yet Authenticated
            LaunchedEffect(viewModel.isLoggedIn, viewModel.isBiometricEnabled) {
                if (viewModel.isLoggedIn && viewModel.isBiometricEnabled && !isBiometricAuthenticated) {
                    if (BiometricUtils.isBiometricAvailable(this@MainActivity)) {
                        BiometricUtils.showBiometricPrompt(
                            activity = this@MainActivity,
                            onSuccess = { isBiometricAuthenticated = true },
                            onFailure = {
                                // If they cancel/fail, we close the app to secure data
                                finish()
                            }
                        )
                    } else {
                        // Fallback if hardware unavailable despite setting being true
                        isBiometricAuthenticated = true
                    }
                } else if (!viewModel.isLoggedIn) {
                    // Reset if logged out
                    isBiometricAuthenticated = !viewModel.isBiometricEnabled
                }
            }

            // --- 2. UI THEME & CONTENT ---
            SpendWiseTheme(darkTheme = viewModel.isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // LOADING / SPLASH STATE
                    // We show the splash if:
                    // 1. The animation is still running
                    // 2. Firebase is still checking auth
                    // 3. User is logged in but hasn't passed Biometric check yet
                    if (showComposeSplash || isCheckingAuth || (viewModel.isLoggedIn && !isBiometricAuthenticated)) {
                        SplashScreen(onSplashFinished = { showComposeSplash = false })
                    } else {

                        // --- MAIN CONTENT ---
                        if (viewModel.showOnboarding) {
                            OnboardingScreen(onGetStarted = { viewModel.completeOnboarding() })
                        } else if (!viewModel.isLoggedIn) {
                            // --- AUTH FLOW ---
                            var authScreenState by rememberSaveable { mutableStateOf("login") }

                            when (authScreenState) {
                                "login" -> LoginScreen(
                                    viewModel = viewModel,
                                    onStartRegistration = { authScreenState = "registration_flow" },
                                    onLoginSuccess = { /* ViewModel state change triggers recomposition to Dashboard */ }
                                )
                                "registration_flow" -> RegistrationFlow(
                                    viewModel = viewModel,
                                    onComplete = { /* ViewModel state change triggers recomposition */ }
                                )
                            }
                        } else {
                            // --- DASHBOARD FLOW ---
                            var currentScreen by rememberSaveable { mutableStateOf("dashboard") }
                            // Tracks where to go back to (e.g., Edit Card -> Wallet List)
                            var returnScreen by rememberSaveable { mutableStateOf("dashboard") }

                            // Global Back Handler
                            BackHandler(enabled = currentScreen != "dashboard") {
                                if (currentScreen == "edit_card" && returnScreen == "wallet_list") {
                                    currentScreen = "wallet_list"
                                } else {
                                    currentScreen = "dashboard"
                                }
                            }

                            Box(modifier = Modifier.fillMaxSize()) {
                                when (currentScreen) {
                                    "dashboard" -> DashboardScreen(
                                        viewModel = viewModel,
                                        onShowMoreClick = { currentScreen = "all_transactions" },
                                        onShowWalletsClick = {
                                            currentScreen = "wallet_list"
                                        },
                                        onEditCardClick = {
                                            // Direct add from dashboard
                                            returnScreen = "dashboard"
                                            currentScreen = "edit_card"
                                        },
                                        onShowAnalysisClick = { currentScreen = "detailed_analysis" },
                                        onAddTransactionClick = {
                                            viewModel.clearEditState()
                                            currentScreen = "add_transaction"
                                        },
                                        onProfileClick = { currentScreen = "profile" },
                                        onRecurringClick = { currentScreen = "recurring_transactions" }
                                    )

                                    "wallet_list" -> WalletListScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = "dashboard" },
                                        onAddWallet = {
                                            viewModel.walletToEdit = null
                                            returnScreen = "wallet_list" // Return here after adding
                                            currentScreen = "edit_card"
                                        },
                                        onEditWallet = {
                                            returnScreen = "wallet_list" // Return here after editing
                                            currentScreen = "edit_card"
                                        }
                                    )

                                    "edit_card" -> EditCardScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = returnScreen }
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

                                    "recurring_transactions" -> RecurringTransactionsScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = "dashboard" },
                                        onNavigateToAdd = {
                                            viewModel.clearRecurringEdit() // Clear any old state
                                            currentScreen = "add_recurring_rule"
                                        },
                                        onNavigateToEdit = { rule ->
                                            viewModel.recurringRuleToEdit = rule // Set state in ViewModel
                                            currentScreen = "add_recurring_rule"
                                        }
                                    )

                                    // [NEW ROUTE]
                                    "add_recurring_rule" -> AddRecurringRuleScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = "recurring_transactions" },
                                        onSaveSuccess = {
                                            viewModel.clearRecurringEdit()
                                            currentScreen = "recurring_transactions"
                                        }
                                    )
                                    "detailed_analysis" -> DetailedAnalysisScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = "dashboard" },
                                        onNavigateToAccountSelect = { currentScreen = "analysis_account_select" } // [NEW]
                                    )

                                    // [NEW ROUTE]
                                    "analysis_account_select" -> AnalysisWalletSelectionScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = "detailed_analysis" }
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