package com.kdev.spendwise

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kdev.spendwise.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardUITest {

    /**
     * This rule launches your MainActivity before every test.
     * It allows us to interact with the UI elements.
     */
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun dashboard_displaysBasicElements() {
        // 1. Verify the FAB (Floating Action Button) exists
        // We look for the ContentDescription we set: "Add Transaction"
        composeTestRule.onNodeWithContentDescription("Add Transaction")
            .assertExists()
            .assertIsDisplayed()

        // 2. Verify the Greeting text exists (Good Morning/Afternoon/Evening)
        // We use substring=true because the text changes based on time
        composeTestRule.onNodeWithText("Good", substring = true)
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun dashboard_showsAddNewWallet_whenEmpty() {
        // Assuming the app starts with 0 wallets (fresh install state)
        // We verify that the "Add New Wallet" text is visible on the dashboard card

        // Note: If you are logged in with an account that HAS wallets,
        // this test might fail. Ideally, run this with a fresh test user.
        composeTestRule.onNodeWithText("Add New Wallet")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun fabClick_opensAddTransactionScreen() {
        // 1. Find the FAB and Click it
        composeTestRule.onNodeWithContentDescription("Add Transaction")
            .performClick()

        // 2. Verify that we navigated to the Add Transaction Screen
        // We check for the title "New Entry" in the TopAppBar
        composeTestRule.onNodeWithText("New Entry")
            .assertIsDisplayed()

        // 3. Verify the "Enter Amount" text exists on the new screen
        composeTestRule.onNodeWithText("Enter Amount")
            .assertIsDisplayed()
    }
}