package com.kdev.spendwise.ui.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kdev.spendwise.data.Expense
import com.kdev.spendwise.data.Wallet
import com.kdev.spendwise.data.cardThemes
import com.kdev.spendwise.ui.MainViewModel
import com.kdev.spendwise.ui.components.CategoryNestedPicker
import com.kdev.spendwise.ui.components.TransactionDateTimePickerDialog
import com.kdev.spendwise.ui.theme.BlueGreyMain
import com.kdev.spendwise.util.CurrencyUtils
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue
import androidx.compose.ui.platform.LocalLocale

@SuppressLint("NonObservableLocale", "DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    onAddWallet: () -> Unit = {} // Added parameter to handle empty state routing
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val noteFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val wallets = viewModel.wallets

    // --- STATE ---
    val transactionTypes = listOf("Expense", "Income", "Transfer")
    var selectedType by remember { mutableStateOf("Expense") }

    var amount by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }

    // Wallet States (Pre-select logic)
    var selectedWalletId by remember {
        mutableStateOf(
            viewModel.selectedWalletId.takeIf { !it.isNullOrEmpty() }
                ?: viewModel.lastUsedWalletId.takeIf { !it.isNullOrEmpty() }
                ?: wallets.firstOrNull()?.id
                ?: ""
        )
    }
    var toWalletId by remember { mutableStateOf("") }

    // Dialog States
    var showWalletPicker by remember { mutableStateOf(false) }
    var isSourceWalletPicker by remember { mutableStateOf(true) } // True = Source, False = Destination (Transfer)

    var selectedMainCategory by remember { mutableStateOf("Food & Drinks") }
    var selectedSubCategory by remember { mutableStateOf("Groceries") }
    var isCategoryPickerVisible by remember { mutableStateOf(false) }

    // Date & Time Logic
    val currentCalendar = remember { Calendar.getInstance() }
    var selectedDateMillis by remember { mutableLongStateOf(currentCalendar.timeInMillis) }
    var selectedHour by remember { mutableIntStateOf(if (currentCalendar.get(Calendar.HOUR) == 0) 12 else currentCalendar.get(Calendar.HOUR)) }
    var selectedMinute by remember { mutableIntStateOf(currentCalendar.get(Calendar.MINUTE)) }
    var isAm by remember { mutableStateOf(currentCalendar.get(Calendar.AM_PM) == Calendar.AM) }
    var showDateTimePicker by remember { mutableStateOf(false) }

    val themeColor by animateColorAsState(
        targetValue = when (selectedType) {
            "Income" -> Color(0xFF388E3C)
            "Transfer" -> Color(0xFF1976D2)
            else -> Color(0xFFD32F2F)
        }, label = "themeColor"
    )

    // Initial Edit State Load
    LaunchedEffect(Unit) {
        if (wallets.isNotEmpty()) {
            delay(100)
            focusRequester.requestFocus()
        }

        viewModel.expenseToEdit?.let { edit ->
            selectedType = when(edit.type) {
                "INCOME" -> "Income"
                "TRANSFER" -> "Transfer"
                else -> "Expense"
            }
            amount = kotlin.math.abs(edit.amount).toString()
            title = edit.title
            selectedWalletId = edit.walletId
            toWalletId = edit.toWalletId ?: ""

            val parts = edit.category.split(" -> ")
            selectedMainCategory = parts.getOrNull(0) ?: "Food & Drinks"
            selectedSubCategory = parts.getOrNull(1) ?: "Groceries"

            val cal = Calendar.getInstance().apply { timeInMillis = edit.date }
            selectedDateMillis = edit.date
            selectedHour = if(cal.get(Calendar.HOUR) == 0) 12 else cal.get(Calendar.HOUR)
            selectedMinute = cal.get(Calendar.MINUTE)
            isAm = cal.get(Calendar.AM_PM) == Calendar.AM
        }
    }

    // --- DIALOGS ---

    if (showDateTimePicker) {
        TransactionDateTimePickerDialog(
            initialDateMillis = selectedDateMillis,
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            initialIsAm = isAm,
            primaryColor = themeColor,
            onDismiss = { showDateTimePicker = false },
            onConfirm = { date, hour, minute, am ->
                selectedDateMillis = date
                selectedHour = hour
                selectedMinute = minute
                isAm = am
                showDateTimePicker = false
            }
        )
    }

    if (isCategoryPickerVisible) {
        CategoryNestedPicker(
            viewModel = viewModel,
            isExpense = selectedType == "Expense",
            onDismiss = { isCategoryPickerVisible = false },
            onConfirmed = { main, sub ->
                selectedMainCategory = main
                selectedSubCategory = sub
                isCategoryPickerVisible = false
            }
        )
    }

    if (showWalletPicker) {
        // [Logic] Don't show the currently selected "From" wallet in the "To" list
        val displayedWallets = if (!isSourceWalletPicker && selectedType == "Transfer") {
            wallets.filter { it.id != selectedWalletId }
        } else {
            wallets
        }

        WalletPickerDialog(
            wallets = displayedWallets,
            currentWalletId = if (isSourceWalletPicker) selectedWalletId else toWalletId,
            onDismiss = { showWalletPicker = false },
            onWalletSelected = { walletId ->
                if (isSourceWalletPicker) {
                    selectedWalletId = walletId
                    // If source becomes same as dest, clear dest
                    if (selectedType == "Transfer" && toWalletId == walletId) {
                        toWalletId = ""
                    }
                } else {
                    toWalletId = walletId
                }
                showWalletPicker = false
            },
            onAddWallet = {
                showWalletPicker = false
                viewModel.walletToEdit = null
                onAddWallet()
            }
        )
    }

    // --- MAIN UI ---

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (viewModel.expenseToEdit != null) "Edit Entry" else "New Entry",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (viewModel.expenseToEdit != null) "Update transaction details" else "Record income or expense",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.clearEditState(); onBack() }) {
                        Icon(Icons.Default.Close, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            if (wallets.isNotEmpty()) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val amountVal = amount.toDoubleOrNull() ?: 0.0

                            // Validation Logic
                            if (amountVal <= 0) {
                                Toast.makeText(context, "Enter valid amount", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (selectedWalletId.isBlank()) {
                                Toast.makeText(context, "Please create a Wallet first!", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            if (selectedType == "Transfer" && (toWalletId.isBlank() || selectedWalletId == toWalletId)) {
                                Toast.makeText(context, "Select valid destination wallet", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // Date Construction
                            val calendar = Calendar.getInstance().apply {
                                timeInMillis = selectedDateMillis
                                set(Calendar.HOUR, if (selectedHour == 12) 0 else selectedHour)
                                set(Calendar.MINUTE, selectedMinute)
                                set(Calendar.AM_PM, if (isAm) Calendar.AM else Calendar.PM)
                            }

                            val finalAmt = if (selectedType == "Expense") -amountVal else amountVal
                            val categoryString = if(selectedType == "Transfer") "Transfer" else "$selectedMainCategory -> $selectedSubCategory"
                            val finalTitle = title.ifEmpty { if(selectedType == "Transfer") "Transfer" else "Transaction" }

                            val tx = Expense(
                                id = viewModel.expenseToEdit?.id ?: "",
                                amount = finalAmt,
                                title = finalTitle,
                                category = categoryString,
                                type = selectedType.uppercase(),
                                date = calendar.timeInMillis,
                                walletId = selectedWalletId,
                                toWalletId = if(selectedType == "Transfer") toWalletId else null
                            )

                            if (viewModel.expenseToEdit != null) viewModel.updateTransaction(viewModel.expenseToEdit!!, tx) else viewModel.addTransaction(tx)
                            viewModel.clearEditState()
                            onSaveSuccess()
                        },
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        // [BLOCKING LOGIC] Button Disabled if Amount is empty OR Wallet is missing
                        enabled = amount.isNotEmpty() && selectedWalletId.isNotBlank(),
                    ) { Text("SAVE TRANSACTION", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding)
                .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
        ) {
            // --- EMPTY WALLET STATE (Total App Empty State) ---
            if (wallets.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(50.dp)
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "No Wallets Available",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "You need to add a wallet or bank account before you can record any transactions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = {
                            viewModel.walletToEdit = null
                            onAddWallet()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add New Wallet", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            } else {
                // --- NORMAL TRANSACTION FORM ---
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {

                    // 1. TYPE SELECTOR
                    Box(Modifier.fillMaxWidth().height(50.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)).padding(4.dp)) {
                        Row(Modifier.fillMaxSize()) {
                            transactionTypes.forEach { type ->
                                val selected = selectedType == type
                                val bgColor by animateColorAsState(if (selected) themeColor else Color.Transparent, label = "bgColor")

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(bgColor)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            focusManager.clearFocus()
                                            if (selectedType != type) {
                                                selectedType = type
                                                // Reset categories based on type
                                                if (type == "Income") {
                                                    selectedMainCategory = "Income"
                                                    selectedSubCategory = "Salary"
                                                } else if (type == "Expense") {
                                                    selectedMainCategory = "Food & Drinks"
                                                    selectedSubCategory = "Groceries"
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(type, color = if (selected) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    // 2. AMOUNT INPUT
                    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                        Text("Enter Amount", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (selectedType == "Expense") "- ₹" else if (selectedType == "Transfer") "₹" else "+ ₹",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor
                            )
                            TextField(
                                value = amount,
                                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it },
                                textStyle = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = themeColor, textAlign = TextAlign.Start),
                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = themeColor,
                                    focusedTextColor = themeColor
                                ),
                                placeholder = { Text("0", fontSize = 48.sp, color = Color.LightGray) }
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // 3. WALLET SELECTION
                    val walletLabel = when (selectedType) {
                        "Expense" -> "Pay With"
                        "Income" -> "Deposit To"
                        else -> "Transfer From"
                    }

                    Text(walletLabel, style = MaterialTheme.typography.labelLarge, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))

                    WalletSelectionCard(
                        walletId = selectedWalletId,
                        wallets = wallets,
                        themeColor = themeColor,
                        onClick = {
                            isSourceWalletPicker = true
                            showWalletPicker = true
                        }
                    )

                    if(selectedType == "Transfer") {
                        Spacer(Modifier.height(16.dp))
                        Text("Transfer To", style = MaterialTheme.typography.labelLarge, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                        WalletSelectionCard(
                            walletId = toWalletId,
                            wallets = wallets,
                            themeColor = themeColor,
                            onClick = {
                                isSourceWalletPicker = false
                                showWalletPicker = true
                            }
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // 4. CATEGORY CARD (Hide for Transfer)
                    if (selectedType != "Transfer") {
                        Text("Category Selection", style = MaterialTheme.typography.labelLarge, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))

                        val displayMain = selectedMainCategory.ifEmpty { "Select Category" }
                        val displaySub = selectedSubCategory.ifEmpty { "Tap to choose" }

                        val categoryIcon = if (selectedSubCategory.isNotEmpty()) {
                            viewModel.getIconForCategory(selectedSubCategory)
                        } else if (selectedMainCategory.isNotEmpty()) {
                            viewModel.getIconForCategory(selectedMainCategory)
                        } else {
                            Icons.Default.Category
                        }

                        val iconTint = if(selectedSubCategory.isNotEmpty() || selectedMainCategory.isNotEmpty()) Color.White else Color.Gray
                        val bgTint = if(selectedSubCategory.isNotEmpty() || selectedMainCategory.isNotEmpty()) themeColor else MaterialTheme.colorScheme.surfaceVariant

                        Card(
                            onClick = { focusManager.clearFocus(); isCategoryPickerVisible = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, themeColor.copy(alpha = 0.2f))
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(48.dp).background(bgTint, CircleShape), Alignment.Center) {
                                    Icon(categoryIcon, null, tint = iconTint)
                                }
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(displayMain, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = themeColor)
                                    Text(displaySub, fontSize = 14.sp, color = Color.Gray)
                                }
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, null, tint = themeColor)
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    // 5. DATE & TIME
                    Text("Date & Time", style = MaterialTheme.typography.labelLarge, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                    Card(
                        onClick = { focusManager.clearFocus(); showDateTimePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, null, tint = Color.Gray)
                            Spacer(Modifier.width(12.dp))

                            val dateStr = SimpleDateFormat("dd MMM, yyyy", LocalLocale.current.platformLocale).format(Date(selectedDateMillis))
                            val timeStr = String.format("%02d:%02d %s", selectedHour, selectedMinute, if (isAm) "AM" else "PM")

                            Text("$dateStr • $timeStr", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // 6. NOTE INPUT
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        label = { Text("Note (Optional)") },
                        modifier = Modifier.fillMaxWidth().focusRequester(noteFocusRequester),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        singleLine = true
                    )

                    Spacer(Modifier.height(100.dp))
                }
            }
        }
    }
}

// --- UPDATED PREMIUM WALLET SELECTION CARD ---
@Composable
fun WalletSelectionCard(
    walletId: String,
    wallets: List<Wallet>,
    themeColor: Color,
    onClick: () -> Unit
) {
    val selectedWallet = wallets.find { it.id == walletId }
    val bankName = selectedWallet?.bankName?.ifBlank { "Wallet" } ?: "Select Wallet"
    val cardName = selectedWallet?.name ?: ""
    val last4 = if(selectedWallet != null && selectedWallet.cardNumber.isNotBlank())
        " •••• ${selectedWallet.cardNumber.takeLast(4)}"
    else ""

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)),
        border = if(walletId.isBlank()) BorderStroke(1.dp, MaterialTheme.colorScheme.error) else null
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccountBalanceWallet, null, tint = if(walletId.isBlank()) MaterialTheme.colorScheme.error else Color.Gray)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(bankName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (selectedWallet != null) {
                    Text("$cardName$last4", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Spacer(Modifier.weight(1f))
            if(selectedWallet != null) {
                Text(CurrencyUtils.formatINR(selectedWallet.balance), fontWeight = FontWeight.Bold, color = themeColor)
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
        }
    }
}

// --- UPDATED PREMIUM WALLET PICKER DIALOG ---
@Composable
fun WalletPickerDialog(
    wallets: List<Wallet>,
    currentWalletId: String?,
    onDismiss: () -> Unit,
    onWalletSelected: (String) -> Unit,
    onAddWallet: () -> Unit // Added callback for empty state action
) {
    // 1. Theme Color
    val selectionColor = BlueGreyMain

    // 2. Local State for Instant UI Updates
    var selectedId by remember(currentWalletId) { mutableStateOf(currentWalletId) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // --- HEADER ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Select Account",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Choose a wallet for this transaction",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // --- LIST OR EMPTY STATE ---
                if (wallets.isEmpty()) {
                    // Premium Empty State if no wallets available (e.g., Transfer with only 1 wallet)
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("No Accounts Found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "You need to add another wallet before you can perform this action.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = onAddWallet,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.height(48.dp).padding(horizontal = 24.dp)
                        ) {
                            Text("Add New Wallet", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        wallets.forEach { wallet ->
                            val isSelected = wallet.id == selectedId

                            // Theme Logic
                            val theme = cardThemes.getOrElse(wallet.cardThemeId) { cardThemes[0] }
                            val brush = Brush.linearGradient(colors = listOf(theme.start, theme.end))

                            val displayNum = if (wallet.cardNumber.isNotBlank()) {
                                wallet.cardNumber.takeLast(4)
                            } else {
                                val hash = wallet.id.hashCode().absoluteValue.toString()
                                hash.takeLast(4).padStart(4, '0')
                            }

                            Card(
                                onClick = {
                                    selectedId = wallet.id
                                    onWalletSelected(wallet.id)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                // NO ALPHA: Cards remain fully visible as requested
                                shape = RoundedCornerShape(24.dp),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = if (isSelected) 12.dp else 2.dp
                                ),
                                border = if (isSelected) BorderStroke(2.dp, selectionColor) else null
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(brush)
                                ) {
                                    // Texture Overlay
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.1f))
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 24.dp, vertical = 20.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Left: Info
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            // Wallet Nickname (Small Top)
                                            Text(
                                                text = wallet.name.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Medium,
                                                letterSpacing = 1.sp,
                                                color = Color.White.copy(alpha = 0.7f)
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))

                                            // Bank Name (Big Middle)
                                            Text(
                                                text = wallet.bankName.ifBlank { "Bank Name" },
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                maxLines = 1,
                                                modifier = Modifier.wrapContentWidth(Alignment.Start)
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Number (Bottom)
                                            Text(
                                                text = "••••  ••••  ••••  $displayNum",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }

                                        // Right: Selection & Balance
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxHeight()
                                        ) {
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .background(selectionColor, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                                )
                                            }

                                            Spacer(modifier = Modifier.weight(1f))

                                            // Added Balance to match premium layout
                                            Text(
                                                text = CurrencyUtils.formatINR(wallet.balance),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(48.dp))
                    }
                }
            }
        }
    }
}