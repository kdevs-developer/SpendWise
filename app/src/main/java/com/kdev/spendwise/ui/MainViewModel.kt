package com.kdev.spendwise.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.* import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kdev.spendwise.R
import com.kdev.spendwise.data.Expense
import com.kdev.spendwise.data.RecurringRule
import com.kdev.spendwise.data.Wallet
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import kotlin.math.abs

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val prefs = application.getSharedPreferences("spendwise_prefs", Context.MODE_PRIVATE)

    // --- APP STATES ---
    var isLoggedIn by mutableStateOf(false)
    var isCheckingAuthState by mutableStateOf(true)
    var isDarkMode by mutableStateOf(prefs.getBoolean("is_dark_mode", false))
    var isBiometricEnabled by mutableStateOf(prefs.getBoolean("biometric_enabled", false))
    var showOnboarding by mutableStateOf(prefs.getBoolean("is_first_launch", true))

    // User Profile
    var userName by mutableStateOf("")
    var userEmail by mutableStateOf("")
    var userDob by mutableStateOf("")
    var userGender by mutableStateOf("")
    var userAvatarIndex by mutableIntStateOf(0)

    // WALLET STATES
    var wallets = mutableStateListOf<Wallet>()
    var selectedWalletId by mutableStateOf<String?>(null) // Null means "All Wallets"
    var lastUsedWalletId by mutableStateOf(prefs.getString("last_wallet_id", ""))

    // RECURRING STATES
    var recurringRules = mutableStateListOf<RecurringRule>()

    // Sorted list for Dashboard "Upcoming" section
    val upcomingRules by derivedStateOf {
        recurringRules.sortedBy { it.nextRunDate }
    }

    // RECURRING EDIT STATE
    var recurringRuleToEdit by mutableStateOf<RecurringRule?>(null)

    fun clearRecurringEdit() {
        recurringRuleToEdit = null
    }

    private var recurringCheckJob: Job? = null

    // Edit States
    var expenseToEdit by mutableStateOf<Expense?>(null)
    var walletToEdit by mutableStateOf<Wallet?>(null)

    // Categories
    var categoryMap by mutableStateOf<Map<String, List<String>>>(emptyMap())

    val availableAvatars = listOf(
        R.drawable.avatar_1, R.drawable.avatar_2, R.drawable.avatar_3,
        R.drawable.avatar_4, R.drawable.avatar_5
    )

    init {
        checkInitialAuthState()
    }

    // ============================================================================================
    // REGION: INITIALIZATION & AUTH
    // ============================================================================================

    private fun checkInitialAuthState() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists() && doc.contains("name")) {
                        loadUserData()
                    } else {
                        isLoggedIn = false
                        isCheckingAuthState = false
                    }
                }
                .addOnFailureListener {
                    isLoggedIn = false
                    isCheckingAuthState = false
                }
        } else {
            isLoggedIn = false
            isCheckingAuthState = false
        }
    }

    private fun loadUserData() {
        fetchUserProfile()
        fetchWallets()
        fetchUserCategories()
        fetchRecurringRules()
        startRecurringCheckLoop()
        isLoggedIn = true
        isCheckingAuthState = false
    }

    fun signInWithGoogle(idToken: String, onResult: (Boolean) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists() && doc.contains("name")) {
                                loadUserData()
                                onResult(false) // Not new user
                            } else {
                                onResult(true) // New user
                            }
                        }
                        .addOnFailureListener { onResult(true) }
                } else {
                    onResult(false)
                }
            }
            .addOnFailureListener { Log.e("Auth", "Google sign in failed", it) }
    }

    fun finalizeProfile(
        name: String, dob: String, gender: String, balance: Double, avatarIdx: Int,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return
        val userMap = hashMapOf(
            "name" to name, "dob" to dob, "gender" to gender,
            "avatarIndex" to avatarIdx, "email" to auth.currentUser?.email
        )

        db.collection("users").document(uid).set(userMap)
            .addOnSuccessListener {
                loadUserData()
                onSuccess()
            }
            .addOnFailureListener { e -> onFailure(e.message ?: "Registration failed") }
    }

    fun logout() {
        recurringCheckJob?.cancel()
        auth.signOut()
        viewModelScope.launch {
            wallets.clear()
            recurringRules.clear()
            categoryMap = emptyMap()
            userName = ""
            userEmail = ""
            userDob = ""
            userGender = ""
            selectedWalletId = null
            isLoggedIn = false
            isCheckingAuthState = false
        }
    }

    fun deleteUserAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val userDocRef = db.collection("users").document(uid)

        val collections = listOf("wallets", "transactions", "recurring_rules")
        val fetchTasks = collections.map { userDocRef.collection(it).get() }

        Tasks.whenAllSuccess<com.google.firebase.firestore.QuerySnapshot>(fetchTasks)
            .addOnSuccessListener { snapshots ->
                val batch = db.batch()
                snapshots.flatMap { it.documents }.forEach { batch.delete(it.reference) }
                batch.delete(userDocRef)

                batch.commit().addOnSuccessListener {
                    user.delete().addOnSuccessListener {
                        viewModelScope.launch {
                            logout()
                            onSuccess()
                        }
                    }.addOnFailureListener { e ->
                        if (e is FirebaseAuthRecentLoginRequiredException) onError("Please Log Out and Log In again to delete account.")
                        else onError(e.message ?: "Auth Error")
                    }
                }.addOnFailureListener { e -> onError("Data Delete Failed: ${e.message}") }
            }
            .addOnFailureListener { e -> onError("Failed to fetch data: ${e.message}") }
    }

    // ============================================================================================
    // REGION: DATA FETCHING (User, Wallets, Recurring)
    // ============================================================================================

    fun fetchUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            userName = doc.getString("name") ?: ""
            userEmail = doc.getString("email") ?: ""
            userDob = doc.getString("dob") ?: ""
            userGender = doc.getString("gender") ?: ""
            userAvatarIndex = doc.getLong("avatarIndex")?.toInt() ?: 0
        }
    }

    private fun fetchWallets() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("wallets")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.toObjects(Wallet::class.java)
                    wallets.clear()
                    wallets.addAll(list)
                    // If no wallet is selected yet, default to "All" (null) or first wallet if you prefer
                    if (wallets.isNotEmpty() && selectedWalletId == null) {
                        // selectedWalletId = wallets.first().id // Optional: Default to first
                    }
                }
            }
    }

    private fun fetchRecurringRules() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("recurring_rules")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    recurringRules.clear()
                    recurringRules.addAll(snapshot.toObjects(RecurringRule::class.java))
                }
            }
    }

    // ============================================================================================
    // REGION: TRANSACTIONS & EXPENSES
    // ============================================================================================

    private val _expensesFlow = callbackFlow {
        val user = auth.currentUser
        if (user == null) { trySend(emptyList()); awaitClose {}; return@callbackFlow }

        val subscription = db.collection("users").document(user.uid)
            .collection("transactions")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.toObjects(Expense::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { subscription.remove() }
    }

    val filteredExpenses = combine(_expensesFlow, snapshotFlow { selectedWalletId }) { list, walletId ->
        if (walletId == null) list
        else list.filter { it.walletId == walletId || it.toWalletId == walletId }
            .map { expense ->
                if (expense.type == "TRANSFER" && expense.toWalletId == walletId) {
                    expense.copy(amount = abs(expense.amount)) // Incoming transfer is positive
                } else {
                    expense
                }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- NEW: Current Month Spending (For Dashboard) ---
    val currentMonthSpending = filteredExpenses.map { list ->
        list.filter { it.type == "EXPENSE" && it.isCurrentMonth() }
            .groupBy { it.category.substringBefore(" -> ") }
            .mapValues { entry -> entry.value.sumOf { abs(it.amount) } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val totalIncome = filteredExpenses.map { list ->
        list.filter { it.type == "INCOME" || (it.type == "TRANSFER" && it.toWalletId == selectedWalletId) }
            .sumOf { abs(it.amount) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense = filteredExpenses.map { list ->
        list.filter { it.type == "EXPENSE" || (it.type == "TRANSFER" && it.walletId == selectedWalletId) }
            .sumOf { abs(it.amount) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun addTransaction(expense: Expense) {
        val uid = auth.currentUser?.uid ?: return
        val newRef = db.collection("users").document(uid).collection("transactions").document()
        val finalAmount = if (expense.type == "EXPENSE" || expense.type == "TRANSFER") -abs(expense.amount) else abs(expense.amount)
        val finalExpense = expense.copy(id = newRef.id, amount = finalAmount)

        if (expense.walletId.isNotEmpty()) {
            lastUsedWalletId = expense.walletId
            prefs.edit().putString("last_wallet_id", expense.walletId).apply()
        }

        db.runTransaction { transaction ->
            val walletRef = db.collection("users").document(uid).collection("wallets").document(expense.walletId)
            val currentBal = transaction.get(walletRef).getDouble("balance") ?: 0.0

            var destRef: DocumentReference? = null
            var destBal = 0.0

            if (expense.type == "TRANSFER" && expense.toWalletId != null) {
                destRef = db.collection("users").document(uid).collection("wallets").document(expense.toWalletId)
                destBal = transaction.get(destRef).getDouble("balance") ?: 0.0
            }

            transaction.set(newRef, finalExpense)
            transaction.update(walletRef, "balance", currentBal + finalAmount) // Deduct from source

            if (destRef != null) {
                transaction.update(destRef, "balance", destBal + abs(expense.amount)) // Add to dest
            }
        }
    }

    fun updateTransaction(oldExpense: Expense, newExpense: Expense) {
        val uid = auth.currentUser?.uid ?: return
        val docRef = db.collection("users").document(uid).collection("transactions").document(oldExpense.id)

        db.runTransaction { transaction ->
            // 1. Identify all wallets involved (Old Source/Dest and New Source/Dest)
            val walletIds = mutableSetOf<String>()
            walletIds.add(oldExpense.walletId)
            if (oldExpense.type == "TRANSFER" && !oldExpense.toWalletId.isNullOrEmpty()) walletIds.add(oldExpense.toWalletId)
            walletIds.add(newExpense.walletId)
            if (newExpense.type == "TRANSFER" && !newExpense.toWalletId.isNullOrEmpty()) walletIds.add(newExpense.toWalletId)

            // 2. Read all wallet snapshots first (Firestore requires reads before writes)
            val walletSnapshots = walletIds.associateWith { id ->
                transaction.get(db.collection("users").document(uid).collection("wallets").document(id))
            }

            // 3. Create a mutable map of current balances to track changes
            val balances = walletSnapshots.mapValues { it.value.getDouble("balance") ?: 0.0 }.toMutableMap()

            // 4. REVERT OLD Transaction (Undo previous effect)
            val oldAmt = abs(oldExpense.amount)
            when (oldExpense.type) {
                "INCOME" -> balances[oldExpense.walletId] = balances[oldExpense.walletId]!! - oldAmt
                "EXPENSE" -> balances[oldExpense.walletId] = balances[oldExpense.walletId]!! + oldAmt
                "TRANSFER" -> {
                    balances[oldExpense.walletId] = balances[oldExpense.walletId]!! + oldAmt // Refund Source
                    if (!oldExpense.toWalletId.isNullOrEmpty() && balances.containsKey(oldExpense.toWalletId)) {
                        balances[oldExpense.toWalletId] = balances[oldExpense.toWalletId]!! - oldAmt // Deduct from Dest
                    }
                }
            }

            // 5. APPLY NEW Transaction (Apply new effect)
            val newAmt = abs(newExpense.amount)
            when (newExpense.type) {
                "INCOME" -> balances[newExpense.walletId] = balances[newExpense.walletId]!! + newAmt
                "EXPENSE" -> balances[newExpense.walletId] = balances[newExpense.walletId]!! - newAmt
                "TRANSFER" -> {
                    balances[newExpense.walletId] = balances[newExpense.walletId]!! - newAmt // Deduct Source
                    if (!newExpense.toWalletId.isNullOrEmpty() && balances.containsKey(newExpense.toWalletId)) {
                        balances[newExpense.toWalletId] = balances[newExpense.toWalletId]!! + newAmt // Add to Dest
                    }
                }
            }

            // 6. Write Updates
            // Save Transaction
            val finalStoredAmount = if (newExpense.type == "INCOME") newAmt else -newAmt
            transaction.set(docRef, newExpense.copy(amount = finalStoredAmount))

            // Update Wallets
            balances.forEach { (id, newBal) ->
                val ref = db.collection("users").document(uid).collection("wallets").document(id)
                transaction.update(ref, "balance", newBal)
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        val uid = auth.currentUser?.uid ?: return
        val docRef = db.collection("users").document(uid).collection("transactions").document(expense.id)

        db.runTransaction { transaction ->
            // 1. Get Source Wallet
            val walletRef = db.collection("users").document(uid).collection("wallets").document(expense.walletId)
            val walletSnapshot = transaction.get(walletRef)
            val currentBal = walletSnapshot.getDouble("balance") ?: 0.0

            // 2. Handle Transfer Destination Wallet
            if (expense.type == "TRANSFER" && !expense.toWalletId.isNullOrEmpty()) {
                val destRef = db.collection("users").document(uid).collection("wallets").document(expense.toWalletId)
                val destSnapshot = transaction.get(destRef)

                // Only try to update destination if it still exists
                if (destSnapshot.exists()) {
                    val destBal = destSnapshot.getDouble("balance") ?: 0.0
                    transaction.update(destRef, "balance", destBal - abs(expense.amount)) // Remove the money that was added
                }
            }

            // 3. Delete Transaction and Refund/Adjust Source
            transaction.delete(docRef)

            if (expense.type == "INCOME") {
                transaction.update(walletRef, "balance", currentBal - abs(expense.amount)) // Remove Income
            } else {
                // For EXPENSE and TRANSFER (Source), we add the money back
                transaction.update(walletRef, "balance", currentBal + abs(expense.amount))
            }
        }
    }
    // ============================================================================================
    // REGION: WALLET CRUD & CASCADE DELETE
    // ============================================================================================

    fun addOrUpdateWallet(wallet: Wallet, onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val walletId = if (wallet.id.isEmpty()) UUID.randomUUID().toString() else wallet.id
        db.collection("users").document(uid).collection("wallets").document(walletId).set(wallet.copy(id = walletId)).addOnSuccessListener { onSuccess() }
    }

    fun deleteWallet(walletId: String, onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val walletRef = db.collection("users").document(uid).collection("wallets").document(walletId)

        // Find all recurring rules linked to this wallet
        db.collection("users").document(uid).collection("recurring_rules")
            .whereEqualTo("walletId", walletId)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()

                // 1. Delete the wallet
                batch.delete(walletRef)

                // 2. Cascade Delete: Remove all linked recurring rules
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }

                batch.commit().addOnSuccessListener {
                    if(selectedWalletId == walletId) selectedWalletId = null
                    onSuccess()
                }.addOnFailureListener { e ->
                    Log.e("WalletDelete", "Failed to cascade delete: ${e.message}")
                }
            }
            .addOnFailureListener { e ->
                // If fetch fails, try deleting just the wallet
                walletRef.delete().addOnSuccessListener { onSuccess() }
            }
    }

    // ============================================================================================
    // REGION: RECURRING LOGIC (FIXED)
    // ============================================================================================

    fun refreshRecurring() {
        processRecurringRules()
    }

    fun addRecurringRule(rule: RecurringRule) {
        val uid = auth.currentUser?.uid ?: return
        val id = if (rule.id.isEmpty()) UUID.randomUUID().toString() else rule.id
        val finalRule = rule.copy(id = id)
        db.collection("users").document(uid).collection("recurring_rules").document(id).set(finalRule)

        if (finalRule.nextRunDate <= System.currentTimeMillis()) {
            processRecurringRules()
        }
    }

    fun deleteRecurringRule(ruleId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("recurring_rules").document(ruleId).delete()
    }

    private fun startRecurringCheckLoop() {
        recurringCheckJob?.cancel()
        recurringCheckJob = viewModelScope.launch {
            while (isActive && isLoggedIn) {
                processRecurringRules()
                delay(60 * 1000) // Check every minute
            }
        }
    }

    private fun processRecurringRules() {
        val uid = auth.currentUser?.uid ?: return
        val now = System.currentTimeMillis()

        db.collection("users").document(uid).collection("recurring_rules").get().addOnSuccessListener { snapshot ->
            for (doc in snapshot.documents) {
                val rule = doc.toObject(RecurringRule::class.java) ?: continue

                if (rule.nextRunDate <= now) {
                    db.runTransaction { transaction ->
                        val walletRef = db.collection("users").document(uid).collection("wallets").document(rule.walletId)
                        val ruleRef = db.collection("users").document(uid).collection("recurring_rules").document(rule.id)
                        val newTxRef = db.collection("users").document(uid).collection("transactions").document()

                        val walletSnapshot = transaction.get(walletRef)

                        // SAFETY CHECK: Ensure wallet still exists before charging
                        if (walletSnapshot.exists()) {
                            val currentBalance = walletSnapshot.getDouble("balance") ?: 0.0
                            val amount = abs(rule.amount)

                            val tx = Expense(
                                id = newTxRef.id,
                                amount = if (rule.type == "INCOME") amount else -amount,
                                title = "Auto: ${rule.title}",
                                category = rule.category,
                                type = rule.type,
                                date = rule.nextRunDate,
                                walletId = rule.walletId
                            )

                            var nextDate = calculateNextDate(rule.nextRunDate, rule.frequency)
                            while (nextDate <= now) {
                                nextDate = calculateNextDate(nextDate, rule.frequency)
                            }

                            transaction.set(newTxRef, tx)
                            transaction.update(walletRef, "balance", if (rule.type == "INCOME") currentBalance + amount else currentBalance - amount)
                            transaction.update(ruleRef, "nextRunDate", nextDate)
                        } else {
                            // If wallet missing, maybe delete the rule? For now, we just skip.
                            Log.w("Recurring", "Skipping rule ${rule.title} because wallet ${rule.walletId} was deleted.")
                        }
                    }
                }
            }
        }
    }

    private fun calculateNextDate(current: Long, freq: String): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = current
        val cleanFreq = freq.lowercase().trim()

        val numberRegex = "\\d+".toRegex()
        val match = numberRegex.find(cleanFreq)
        val count = match?.value?.toIntOrNull() ?: 1

        when {
            "day" in cleanFreq || "daily" in cleanFreq -> cal.add(Calendar.DAY_OF_YEAR, count)
            "week" in cleanFreq || "weekly" in cleanFreq -> cal.add(Calendar.WEEK_OF_YEAR, count)
            "month" in cleanFreq || "monthly" in cleanFreq -> cal.add(Calendar.MONTH, count)
            "year" in cleanFreq || "yearly" in cleanFreq -> cal.add(Calendar.YEAR, count)
            else -> cal.add(Calendar.MONTH, 1)
        }

        return cal.timeInMillis
    }

    // ============================================================================================
    // REGION: CATEGORIES & HELPERS
    // ============================================================================================

    private fun fetchUserCategories() {
        if (categoryMap.isEmpty()) {
            categoryMap = mapOf(
                "Food & Drinks" to listOf("Groceries", "Restaurants", "Fast Food", "Coffee", "Bars", "Delivery"),
                "Shopping" to listOf("Clothing", "Electronics", "Home & Garden", "Health & Beauty", "Gifts", "Kids"),
                "Housing" to listOf("Rent", "Mortgage", "Utilities", "Maintenance", "Services", "Insurance"),
                "Transportation" to listOf("Public Transport", "Taxi", "Flight", "Train", "Fuel", "Parking"),
                "Vehicle" to listOf("Fuel", "Insurance", "Parking", "Repairs", "Maintenance", "Wash"),
                "Entertainment" to listOf("Movies", "Games", "Sports", "Events", "Streaming", "Music"),
                "Communication" to listOf("Phone Bill", "Internet", "Software", "Postal"),
                "Finance" to listOf("Taxes", "Fees", "Fines", "Insurance", "Loan", "Investment"),
                "Investments" to listOf("Stocks", "Crypto", "Real Estate", "Savings", "Bonds"),
                "Income" to listOf("Salary", "Bonus", "Commission", "Interest", "Dividends", "Rental Income", "Freelance", "Side Hustle", "Gifts", "Refunds", "Grants", "Sale of Items")
            )
        }
    }

    fun getIconForCategory(name: String): ImageVector {
        return when (name) {
            "Food & Drinks" -> Icons.Default.Restaurant
            "Shopping" -> Icons.Default.ShoppingBag
            "Housing" -> Icons.Default.Home
            "Transportation" -> Icons.Default.DirectionsBus
            "Vehicle" -> Icons.Default.DirectionsCar
            "Entertainment" -> Icons.Default.Movie
            "Communication" -> Icons.Default.Phone
            "Finance" -> Icons.Default.AccountBalanceWallet
            "Investments" -> Icons.Default.TrendingUp
            "Income" -> Icons.Default.AttachMoney
            "Groceries" -> Icons.Default.ShoppingCart
            "Restaurants" -> Icons.Default.RestaurantMenu
            "Fast Food" -> Icons.Default.Fastfood
            "Coffee" -> Icons.Default.LocalCafe
            "Bars" -> Icons.Default.LocalBar
            "Delivery" -> Icons.Default.DeliveryDining
            "Clothing" -> Icons.Default.Checkroom
            "Electronics" -> Icons.Default.Devices
            "Home & Garden" -> Icons.Default.Yard
            "Health & Beauty" -> Icons.Default.Spa
            "Gifts" -> Icons.Default.CardGiftcard
            "Kids" -> Icons.Default.ChildFriendly
            "Rent" -> Icons.Default.House
            "Mortgage" -> Icons.Default.Key
            "Utilities" -> Icons.Default.Lightbulb
            "Maintenance" -> Icons.Default.Build
            "Services" -> Icons.Default.CleaningServices
            "Insurance" -> Icons.Default.Security
            "Public Transport" -> Icons.Default.Train
            "Taxi" -> Icons.Default.LocalTaxi
            "Flight" -> Icons.Default.Flight
            "Train" -> Icons.Default.Train
            "Fuel" -> Icons.Default.LocalGasStation
            "Parking" -> Icons.Default.LocalParking
            "Repairs" -> Icons.Default.CarRepair
            "Wash" -> Icons.Default.LocalCarWash
            "Movies" -> Icons.Default.Theaters
            "Games" -> Icons.Default.Gamepad
            "Sports" -> Icons.Default.SportsSoccer
            "Events" -> Icons.Default.Event
            "Streaming" -> Icons.Default.Tv
            "Music" -> Icons.Default.MusicNote
            "Phone Bill" -> Icons.Default.PhoneAndroid
            "Internet" -> Icons.Default.Wifi
            "Software" -> Icons.Default.Code
            "Postal" -> Icons.Default.Markunread
            "Taxes" -> Icons.Default.RequestQuote
            "Fees" -> Icons.Default.Payments
            "Fines" -> Icons.Default.Gavel
            "Loan" -> Icons.Default.CreditScore
            "Investment" -> Icons.Default.ShowChart
            "Stocks" -> Icons.Default.Timeline
            "Crypto" -> Icons.Default.CurrencyBitcoin
            "Real Estate" -> Icons.Default.Apartment
            "Savings" -> Icons.Default.Savings
            "Bonds" -> Icons.Default.Description
            "Salary" -> Icons.Default.Work
            "Bonus" -> Icons.Default.Stars
            "Commission" -> Icons.Default.Percent
            "Interest" -> Icons.Default.AccountBalance
            "Dividends" -> Icons.Default.PieChart
            "Rental Income" -> Icons.Default.Domain
            "Freelance" -> Icons.Default.LaptopMac
            "Side Hustle" -> Icons.Default.Bolt
            "Refunds" -> Icons.Default.Undo
            "Grants" -> Icons.Default.School
            "Sale of Items" -> Icons.Default.Sell
            else -> Icons.Default.Category
        }
    }

    fun getCategoriesForType(isExpense: Boolean): Map<String, List<String>> {
        return if (isExpense) categoryMap.filterKeys { it != "Income" } else categoryMap.filterKeys { it == "Income" }
    }

    fun updateBiometric(enabled: Boolean) { isBiometricEnabled = enabled; prefs.edit().putBoolean("biometric_enabled", enabled).apply() }
    fun toggleTheme(isDark: Boolean) { isDarkMode = isDark; prefs.edit().putBoolean("is_dark_mode", isDark).apply() }
    fun getCurrentAvatar() = if (userAvatarIndex in availableAvatars.indices) availableAvatars[userAvatarIndex] else R.drawable.avatar_1
    fun completeOnboarding() { showOnboarding = false; prefs.edit().putBoolean("is_first_launch", false).apply() }
    fun clearEditState() { expenseToEdit = null }
    fun selectWallet(walletId: String?) { selectedWalletId = walletId }
}