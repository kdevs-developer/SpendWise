package com.kdev.spendwise.ui.screens

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
import com.kdev.spendwise.ui.MainViewModel
import com.kdev.spendwise.ui.components.CategoryNestedPicker
import com.kdev.spendwise.ui.components.TransactionDateTimePickerDialog
import com.kdev.spendwise.util.CurrencyUtils
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val noteFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val wallets = viewModel.wallets

    // --- STATE ---
    val transactionTypes = listOf("Expense", "Income", "Transfer")
    var selectedType by remember { mutableStateOf("Expense") }

    var amount by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }

    // Wallet States
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

    // Ensure wallet is selected if list loads late
    LaunchedEffect(wallets) {
        if (selectedWalletId.isEmpty() && wallets.isNotEmpty()) {
            selectedWalletId = viewModel.selectedWalletId ?: wallets.first().id
        }
        if (toWalletId.isEmpty() && wallets.size > 1) {
            toWalletId = wallets.firstOrNull { it.id != selectedWalletId }?.id ?: ""
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()

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
        val displayedWallets = if (!isSourceWalletPicker && selectedType == "Transfer") {
            wallets.filter { it.id != selectedWalletId }
        } else {
            wallets
        }

        WalletPickerDialog(
            wallets = displayedWallets,
            onDismiss = { showWalletPicker = false },
            onWalletSelected = { walletId ->
                if (isSourceWalletPicker) {
                    selectedWalletId = walletId
                    if (selectedType == "Transfer" && toWalletId == walletId) {
                        toWalletId = ""
                    }
                } else {
                    toWalletId = walletId
                }
                showWalletPicker = false
            }
        )
    }

    // --- MAIN UI ---

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.expenseToEdit != null) "Edit Entry" else "New Entry", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { viewModel.clearEditState(); onBack() }) { Icon(Icons.Default.Close, null) } }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    val amountVal = amount.toDoubleOrNull() ?: 0.0
                    if (amountVal <= 0) { Toast.makeText(context, "Enter valid amount", Toast.LENGTH_SHORT).show(); return@Button }
                    if (selectedWalletId.isBlank()) { Toast.makeText(context, "Please create a Wallet first!", Toast.LENGTH_LONG).show(); return@Button }
                    if (selectedType == "Transfer" && (toWalletId.isBlank() || selectedWalletId == toWalletId)) { Toast.makeText(context, "Select valid destination wallet", Toast.LENGTH_SHORT).show(); return@Button }

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
                enabled = amount.isNotEmpty()
            ) { Text("SAVE TRANSACTION", fontWeight = FontWeight.ExtraBold) }
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding)
                .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
        ) {
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
                                        focusManager.clearFocus()
                                        if (selectedType != type) {
                                            selectedType = type
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

                // 2. AMOUNT INPUT (Action: DONE -> Close Keyboard)
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
                            // KEYBOARD SETTINGS: Decimal + Done Action (Closes Keyboard)
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() } // Just close keyboard
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

                // 4. CATEGORY CARD
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

                        val dateStr = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
                        val timeStr = String.format("%02d:%02d %s", selectedHour, selectedMinute, if (isAm) "AM" else "PM")

                        Text("$dateStr • $timeStr", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 6. NOTE INPUT (Action: Done -> Close Keyboard Only)
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Note (Optional)") },
                    modifier = Modifier.fillMaxWidth().focusRequester(noteFocusRequester),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                    // KEYBOARD: Done -> Clear Focus (Hide Keyboard)
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

// --- NEW COMPONENT: Wallet Selection Card (Click to Open Dialog) ---
@Composable
fun WalletSelectionCard(
    walletId: String,
    wallets: List<Wallet>,
    themeColor: Color,
    onClick: () -> Unit
) {
    val selectedWallet = wallets.find { it.id == walletId }
    val displayText = selectedWallet?.let {
        if(it.bankName.isNotBlank()) "${it.bankName} - ${it.name}" else it.name
    } ?: "Select Wallet"

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccountBalanceWallet, null, tint = Color.Gray)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(displayText, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                if (selectedWallet != null) {
                    Text("Balance: ${CurrencyUtils.formatINR(selectedWallet.balance)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
        }
    }
}

// --- NEW COMPONENT: Wallet Picker Dialog (List View) ---
@Composable
fun WalletPickerDialog(
    wallets: List<Wallet>,
    onDismiss: () -> Unit,
    onWalletSelected: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                // Header
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                    Text("Select Wallet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                // List
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    wallets.forEach { wallet ->
                        Card(
                            onClick = { onWalletSelected(wallet.id) },
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AccountBalance, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(wallet.bankName.ifBlank { "Wallet" }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(wallet.name, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Spacer(Modifier.weight(1f))
                                Text(CurrencyUtils.formatINR(wallet.balance), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Cancel Option
                    Card(
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, Color.Gray.copy(0.5f)),
                        onClick = onDismiss
                    ) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}