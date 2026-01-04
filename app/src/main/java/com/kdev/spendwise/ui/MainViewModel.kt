package com.kdev.spendwise.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import kotlin.math.abs

data class CardTheme(val start: Color, val end: Color, val name: String)

val cardThemes = listOf(
    CardTheme(Color(0xFF1E3C72), Color(0xFF2A5298), "Classic Blue"),
    CardTheme(Color(0xFF00c6ff), Color(0xFF0072ff), "Azure Lane"),
    CardTheme(Color(0xFF1A2980), Color(0xFF26D0CE), "Aquamarine"),
    CardTheme(Color(0xFF11998e), Color(0xFF38ef7d), "Mint Green"),
    CardTheme(Color(0xFF56ab2f), Color(0xFFa8e063), "Lush Bamboo"),
    CardTheme(Color(0xFF8E2DE2), Color(0xFF4A00E0), "Royal Purple"),
    CardTheme(Color(0xFF834d9b), Color(0xFFd04ed6), "Mystic Purple"),
    CardTheme(Color(0xFF860029), Color(0xFFC31432), "Axis Burgundy"),
    CardTheme(Color(0xFFFF512F), Color(0xFFDD2476), "Sunset Orange"),
    CardTheme(Color(0xFFCC95C0), Color(0xFF7AA1D2), "Pastel Dream"),
    CardTheme(Color(0xFF000000), Color(0xFF434343), "Midnight Black")
)

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

    private fun checkInitialAuthState() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists() && doc.contains("name")) {
                        fetchUserProfile()
                        fetchWallets()
                        fetchUserCategories()
                        fetchRecurringRules()
                        processRecurringRules()
                        isLoggedIn = true
                    } else {
                        isLoggedIn = false
                    }
                    isCheckingAuthState = false
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

    // --- AUTHENTICATION ---
    fun signInWithGoogle(idToken: String, onResult: (Boolean) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { task ->
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists() && doc.contains("name")) {
                                fetchUserProfile()
                                fetchWallets()
                                fetchUserCategories()
                                fetchRecurringRules()
                                isLoggedIn = true
                                onResult(false)
                            } else {
                                onResult(true)
                            }
                        }
                        .addOnFailureListener { onResult(true) }
                } else {
                    onResult(false)
                }
            }
            .addOnFailureListener { Log.e("Auth", "Google sign in failed", it) }
    }

    // *** FIX HERE: REMOVED DEFAULT WALLET CREATION ***
    fun finalizeProfile(
        name: String, dob: String, gender: String, balance: Double, avatarIdx: Int,
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onFailure("User not authenticated. Please sign in again.")
            return
        }

        val userMap = hashMapOf(
            "name" to name, "dob" to dob, "gender" to gender,
            "avatarIndex" to avatarIdx, "email" to auth.currentUser?.email
        )

        // Only save user profile, NO default wallet created here
        db.collection("users").document(uid).set(userMap)
            .addOnSuccessListener {
                fetchUserProfile()
                fetchWallets()
                fetchUserCategories()
                isLoggedIn = true
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Registration failed")
            }
    }

    fun logout() {
        Log.d("MainViewModel", "Performing Logout")
        auth.signOut()

        // Use viewModelScope to ensure state updates happen on the main thread
        viewModelScope.launch {
            // CLEAR ALL LOCAL STATE
            wallets.clear()
            recurringRules.clear()
            categoryMap = emptyMap()
            userName = ""
            userEmail = ""
            userDob = ""
            userGender = ""
            selectedWalletId = null

            // This triggers the UI navigation to Login Screen
            isLoggedIn = false
            isCheckingAuthState = false // Ensure we aren't stuck in loading
        }
    }

    // --- DATA FETCHING ---
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
                    // Auto-select first wallet only if none selected AND wallets exist
                    if (wallets.isNotEmpty() && selectedWalletId == null) {
                        selectedWalletId = wallets.first().id
                    }
                }
            }
    }

    // --- EXPENSE LOGIC ---
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
                    expense.copy(amount = abs(expense.amount))
                } else {
                    expense
                }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topExpenses = filteredExpenses.map { list ->
        list.filter { it.type == "EXPENSE" }
            .sortedByDescending { abs(it.amount) }
            .take(3)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailySpending = filteredExpenses.map { list ->
        list.filter { it.type == "EXPENSE" }
            .groupBy {
                val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            .mapValues { entry -> entry.value.sumOf { abs(it.amount) } }
            .toSortedMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val categorySpending = filteredExpenses.map { list ->
        list.filter { it.type == "EXPENSE" }
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

    // --- TRANSACTION OPERATIONS ---
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
            transaction.update(walletRef, "balance", currentBal + finalAmount)

            if (destRef != null) {
                transaction.update(destRef, "balance", destBal + abs(expense.amount))
            }
        }
    }

    fun updateTransaction(oldExpense: Expense, newExpense: Expense) {
        val uid = auth.currentUser?.uid ?: return
        val docRef = db.collection("users").document(uid).collection("transactions").document(oldExpense.id)
        val finalNewAmount = if (newExpense.type == "EXPENSE" || newExpense.type == "TRANSFER") -abs(newExpense.amount) else abs(newExpense.amount)
        val finalNewExpense = newExpense.copy(amount = finalNewAmount)

        db.runTransaction { transaction ->
            val walletsToRead = mutableSetOf(oldExpense.walletId, newExpense.walletId)
            oldExpense.toWalletId?.let { walletsToRead.add(it) }
            newExpense.toWalletId?.let { walletsToRead.add(it) }
            val balances = mutableMapOf<String, Double>()
            for (wId in walletsToRead.filter { it.isNotEmpty() }) {
                balances[wId] = transaction.get(db.collection("users").document(uid).collection("wallets").document(wId)).getDouble("balance") ?: 0.0
            }

            if (oldExpense.type == "EXPENSE") balances[oldExpense.walletId] = (balances[oldExpense.walletId] ?: 0.0) + abs(oldExpense.amount)
            else if (oldExpense.type == "INCOME") balances[oldExpense.walletId] = (balances[oldExpense.walletId] ?: 0.0) - abs(oldExpense.amount)
            else if (oldExpense.type == "TRANSFER" && oldExpense.toWalletId != null) {
                balances[oldExpense.walletId] = (balances[oldExpense.walletId] ?: 0.0) + abs(oldExpense.amount)
                balances[oldExpense.toWalletId] = (balances[oldExpense.toWalletId] ?: 0.0) - abs(oldExpense.amount)
            }

            if (finalNewExpense.type == "EXPENSE") balances[finalNewExpense.walletId] = (balances[finalNewExpense.walletId] ?: 0.0) - abs(finalNewExpense.amount)
            else if (finalNewExpense.type == "INCOME") balances[finalNewExpense.walletId] = (balances[finalNewExpense.walletId] ?: 0.0) + abs(finalNewExpense.amount)
            else if (finalNewExpense.type == "TRANSFER" && finalNewExpense.toWalletId != null) {
                balances[finalNewExpense.walletId] = (balances[finalNewExpense.walletId] ?: 0.0) - abs(finalNewExpense.amount)
                balances[finalNewExpense.toWalletId] = (balances[finalNewExpense.toWalletId] ?: 0.0) + abs(finalNewExpense.amount)
            }

            transaction.set(docRef, finalNewExpense)
            for ((wId, bal) in balances) transaction.update(db.collection("users").document(uid).collection("wallets").document(wId), "balance", bal)
        }
    }

    fun deleteExpense(expense: Expense) {
        val uid = auth.currentUser?.uid ?: return
        val docRef = db.collection("users").document(uid).collection("transactions").document(expense.id)

        db.runTransaction { transaction ->
            val walletRef = db.collection("users").document(uid).collection("wallets").document(expense.walletId)
            val currentBal = transaction.get(walletRef).getDouble("balance") ?: 0.0
            var destRef: DocumentReference? = null
            var destBal = 0.0
            if (expense.type == "TRANSFER" && expense.toWalletId != null) {
                destRef = db.collection("users").document(uid).collection("wallets").document(expense.toWalletId)
                destBal = transaction.get(destRef).getDouble("balance") ?: 0.0
            }

            transaction.delete(docRef)

            if (expense.type == "TRANSFER" && destRef != null) {
                transaction.update(walletRef, "balance", currentBal + abs(expense.amount))
                transaction.update(destRef, "balance", destBal - abs(expense.amount))
            } else if (expense.type == "EXPENSE") {
                transaction.update(walletRef, "balance", currentBal + abs(expense.amount))
            } else {
                transaction.update(walletRef, "balance", currentBal - abs(expense.amount))
            }
        }
    }

    // --- WALLET CRUD ---
    fun addOrUpdateWallet(wallet: Wallet, onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val walletId = if (wallet.id.isEmpty()) UUID.randomUUID().toString() else wallet.id
        db.collection("users").document(uid).collection("wallets").document(walletId).set(wallet.copy(id = walletId)).addOnSuccessListener { onSuccess() }
    }

    fun deleteWallet(walletId: String, onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("transactions").whereEqualTo("walletId", walletId).get().addOnSuccessListener { s1 ->
            db.collection("users").document(uid).collection("transactions").whereEqualTo("toWalletId", walletId).get().addOnSuccessListener { s2 ->
                val batch = db.batch()
                s1.documents.forEach { batch.delete(it.reference) }
                s2.documents.forEach { batch.delete(it.reference) }
                batch.delete(db.collection("users").document(uid).collection("wallets").document(walletId))
                batch.commit().addOnSuccessListener { if(selectedWalletId == walletId) selectedWalletId = null; onSuccess() }
            }
        }
    }

    // --- RECURRING & ACCOUNT DELETION ---
    fun deleteUserAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val userDocRef = db.collection("users").document(uid)

        Log.d("DeleteAccount", "Starting deletion for user: $uid")

        val collections = listOf("wallets", "transactions", "recurring_rules")
        val fetchTasks = collections.map { userDocRef.collection(it).get() }

        Tasks.whenAllSuccess<com.google.firebase.firestore.QuerySnapshot>(fetchTasks)
            .addOnSuccessListener { snapshots ->
                val batch = db.batch()
                val allDocs = snapshots.flatMap { it.documents }

                Log.d("DeleteAccount", "Found ${allDocs.size} documents to delete.")

                for (doc in allDocs) {
                    batch.delete(doc.reference)
                }

                batch.delete(userDocRef)

                batch.commit().addOnSuccessListener {
                    Log.d("DeleteAccount", "Batch delete successful. Deleting Auth User.")
                    user.delete()
                        .addOnSuccessListener {
                            Log.d("DeleteAccount", "Auth User deleted. Logging out.")

                            // Important: Run logout on main thread to trigger UI changes
                            viewModelScope.launch {
                                logout()
                                onSuccess()
                            }
                        }
                        .addOnFailureListener { e ->
                            if (e is FirebaseAuthRecentLoginRequiredException) {
                                onError("Security: Please Log Out and Log In again to delete your account.")
                            } else {
                                onError("Auth Error: ${e.message}")
                            }
                        }
                }.addOnFailureListener { e ->
                    onError("Data Delete Failed: ${e.message}")
                }
            }
            .addOnFailureListener { e ->
                onError("Failed to fetch data: ${e.message}")
            }
    }

    // --- RECURRING RULES ---
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

    fun addRecurringRule(rule: RecurringRule) {
        val uid = auth.currentUser?.uid ?: return
        val id = if (rule.id.isEmpty()) UUID.randomUUID().toString() else rule.id
        db.collection("users").document(uid).collection("recurring_rules").document(id).set(rule.copy(id = id))
    }

    fun deleteRecurringRule(ruleId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("recurring_rules").document(ruleId).delete()
    }

    private fun processRecurringRules() {
        val uid = auth.currentUser?.uid ?: return
        val today = System.currentTimeMillis()

        db.collection("users").document(uid).collection("recurring_rules")
            .whereLessThanOrEqualTo("nextRunDate", today)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val rule = doc.toObject(RecurringRule::class.java) ?: continue

                    val tx = Expense(
                        amount = rule.amount,
                        title = "Auto: ${rule.title}",
                        category = rule.category,
                        type = rule.type,
                        date = rule.nextRunDate,
                        walletId = rule.walletId
                    )
                    addTransaction(tx)

                    val nextDate = calculateNextDate(rule.nextRunDate, rule.frequency)
                    doc.reference.update("nextRunDate", nextDate)
                }
            }
    }

    private fun calculateNextDate(current: Long, freq: String): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = current }

        if (freq.startsWith("Every")) {
            try {
                val parts = freq.split(" ")
                val count = parts[1].toIntOrNull() ?: 1
                val unit = parts[2]
                when (unit) {
                    "Days", "Day" -> cal.add(Calendar.DAY_OF_YEAR, count)
                    "Weeks", "Week" -> cal.add(Calendar.WEEK_OF_YEAR, count)
                    "Months", "Month" -> cal.add(Calendar.MONTH, count)
                    "Years", "Year" -> cal.add(Calendar.YEAR, count)
                }
            } catch (e: Exception) {
                cal.add(Calendar.MONTH, 1)
            }
        } else {
            when(freq) {
                "Daily" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                "Weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                "Monthly" -> cal.add(Calendar.MONTH, 1)
                "Yearly" -> cal.add(Calendar.YEAR, 1)
                else -> cal.add(Calendar.MONTH, 1)
            }
        }
        return cal.timeInMillis
    }

    // --- CATEGORY LOGIC ---
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
                "Income" to listOf("Salary", "Bonus", "Gifts", "Refunds", "Dividends", "Rental")
            )
        }
    }

    fun getIconForCategory(name: String): ImageVector {
        return when (name) {
            "Food & Drinks" -> Icons.Default.Restaurant
            "Groceries" -> Icons.Default.ShoppingCart
            "Restaurants" -> Icons.Default.RestaurantMenu
            "Fast Food" -> Icons.Default.Fastfood
            "Coffee" -> Icons.Default.LocalCafe
            "Bars" -> Icons.Default.LocalBar
            "Delivery" -> Icons.Default.DeliveryDining
            "Shopping" -> Icons.Default.ShoppingBag
            "Clothing" -> Icons.Default.Checkroom
            "Electronics" -> Icons.Default.Devices
            "Home & Garden" -> Icons.Default.Chair
            "Health & Beauty" -> Icons.Default.Spa
            "Gifts" -> Icons.Default.CardGiftcard
            "Kids" -> Icons.Default.ChildCare
            "Housing" -> Icons.Default.Home
            "Rent" -> Icons.Default.House
            "Mortgage" -> Icons.Default.AccountBalance
            "Utilities" -> Icons.Default.Lightbulb
            "Maintenance" -> Icons.Default.Build
            "Services" -> Icons.Default.CleaningServices
            "Insurance" -> Icons.Default.Security
            "Transportation" -> Icons.Default.DirectionsBus
            "Public Transport" -> Icons.Default.DirectionsBus
            "Taxi" -> Icons.Default.LocalTaxi
            "Flight" -> Icons.Default.Flight
            "Train" -> Icons.Default.Train
            "Fuel" -> Icons.Default.LocalGasStation
            "Parking" -> Icons.Default.LocalParking
            "Vehicle" -> Icons.Default.DirectionsCar
            "Repairs" -> Icons.Default.CarRepair
            "Wash" -> Icons.Default.LocalCarWash
            "Entertainment" -> Icons.Default.Movie
            "Movies" -> Icons.Default.Theaters
            "Games" -> Icons.Default.SportsEsports
            "Sports" -> Icons.Default.SportsSoccer
            "Events" -> Icons.Default.Event
            "Streaming" -> Icons.Default.Tv
            "Music" -> Icons.Default.MusicNote
            "Communication" -> Icons.Default.Phone
            "Phone Bill" -> Icons.Default.Smartphone
            "Internet" -> Icons.Default.Wifi
            "Software" -> Icons.Default.Code
            "Postal" -> Icons.Default.LocalPostOffice
            "Finance" -> Icons.Default.AccountBalanceWallet
            "Taxes" -> Icons.Default.RequestQuote
            "Fees" -> Icons.Default.Payments
            "Fines" -> Icons.Default.Gavel
            "Loan" -> Icons.Default.CreditScore
            "Investments" -> Icons.Default.TrendingUp
            "Stocks" -> Icons.Default.ShowChart
            "Crypto" -> Icons.Default.CurrencyExchange
            "Real Estate" -> Icons.Default.Domain
            "Savings" -> Icons.Default.Savings
            "Bonds" -> Icons.Default.ReceiptLong
            "Income" -> Icons.Default.AttachMoney
            "Salary" -> Icons.Default.Work
            "Bonus" -> Icons.Default.Stars
            "Refunds" -> Icons.Default.Undo
            "Dividends" -> Icons.Default.PieChart
            "Rental" -> Icons.Default.Key
            "Salary" -> Icons.Default.Work
            "Business" -> Icons.Default.BusinessCenter
            "Gift" -> Icons.Default.CardGiftcard
            else -> Icons.Default.Category
        }
    }

    fun getCategoriesForType(isExpense: Boolean): Map<String, List<String>> {
        return if (isExpense) {
            categoryMap.filterKeys { it != "Income" }
        } else {
            categoryMap.filterKeys { it == "Income" }
        }
    }

    fun saveCustomCategory(main: String, sub: String, iconName: String) {
        val newMap = categoryMap.toMutableMap()
        if (newMap.containsKey(main)) {
            val subs = newMap[main]?.toMutableList() ?: mutableListOf()
            if (!subs.contains(sub)) subs.add(sub)
            newMap[main] = subs
        } else {
            newMap[main] = listOf(sub)
        }
        categoryMap = newMap
    }

    fun isDefaultCategory(name: String): Boolean = false

    fun renameCustomCategory(old: String, new: String, isMain: Boolean, parent: String?, icon: String) {
        val newMap = categoryMap.toMutableMap()
        if (isMain) {
            val subs = newMap.remove(old) ?: emptyList()
            newMap[new] = subs
        } else if (parent != null) {
            val subs = newMap[parent]?.toMutableList() ?: mutableListOf()
            val index = subs.indexOf(old)
            if (index != -1) {
                subs[index] = new
                newMap[parent] = subs
            }
        }
        categoryMap = newMap
    }

    fun deleteCustomCategory(main: String, sub: String?) {
        val newMap = categoryMap.toMutableMap()
        if (sub == null) {
            newMap.remove(main)
        } else {
            val subs = newMap[main]?.toMutableList() ?: mutableListOf()
            subs.remove(sub)
            newMap[main] = subs
        }
        categoryMap = newMap
    }

    fun updateBiometric(enabled: Boolean) { isBiometricEnabled = enabled; prefs.edit().putBoolean("biometric_enabled", enabled).apply() }
    fun toggleTheme(isDark: Boolean) { isDarkMode = isDark; prefs.edit().putBoolean("is_dark_mode", isDark).apply() }
    fun getCurrentAvatar() = if (userAvatarIndex in availableAvatars.indices) availableAvatars[userAvatarIndex] else R.drawable.avatar_1
    fun completeOnboarding() { showOnboarding = false; prefs.edit().putBoolean("is_first_launch", false).apply() }
    fun clearEditState() { expenseToEdit = null }
    fun selectWallet(walletId: String?) { selectedWalletId = walletId }
}