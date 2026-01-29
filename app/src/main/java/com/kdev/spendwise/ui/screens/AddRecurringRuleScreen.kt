package com.kdev.spendwise.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdev.spendwise.data.RecurringRule
import com.kdev.spendwise.ui.MainViewModel
import com.kdev.spendwise.ui.components.*
import com.kdev.spendwise.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringRuleScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val wallets = viewModel.wallets
    val ruleToEdit = viewModel.recurringRuleToEdit
    val isEditing = ruleToEdit != null

    // [FIX] Handle System Back Button / Swipe Back
    // This ensures it goes back to the Recurring List, not the Dashboard
    BackHandler {
        viewModel.clearRecurringEdit()
        onBack()
    }

    // --- STATES ---
    var selectedType by remember { mutableStateOf(ruleToEdit?.type ?: "EXPENSE") }
    var title by remember { mutableStateOf(ruleToEdit?.title ?: "") }
    var amount by remember { mutableStateOf(if (isEditing) ruleToEdit!!.amount.toString() else "") }
    var frequency by remember { mutableStateOf(ruleToEdit?.frequency ?: "Monthly") }
    var selectedWalletId by remember { mutableStateOf(ruleToEdit?.walletId ?: wallets.firstOrNull()?.id ?: "") }

    // Category Parsing
    val catParts = ruleToEdit?.category?.split(" -> ")
    var selectedMainCat by remember { mutableStateOf(catParts?.getOrNull(0) ?: if(ruleToEdit?.type == "INCOME") "Income" else "Food & Drinks") }
    var selectedSubCategory by remember { mutableStateOf(catParts?.getOrNull(1) ?: if(ruleToEdit?.type == "INCOME") "Salary" else "Groceries") }

    // Date Logic
    val currentCalendar = remember { Calendar.getInstance() }
    var selectedDateMillis by remember { mutableLongStateOf(ruleToEdit?.nextRunDate ?: currentCalendar.timeInMillis) }

    // Parse time components for the picker logic
    val initialCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
    var selectedHour by remember { mutableIntStateOf(if (initialCal.get(Calendar.HOUR) == 0) 12 else initialCal.get(Calendar.HOUR)) }
    var selectedMinute by remember { mutableIntStateOf(initialCal.get(Calendar.MINUTE)) }
    var isAm by remember { mutableStateOf(initialCal.get(Calendar.AM_PM) == Calendar.AM) }

    // Dialog States
    var showCustomFreqDialog by remember { mutableStateOf(false) }
    var showWalletPicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showDateTimePicker by remember { mutableStateOf(false) }

    val themeColor by animateColorAsState(if(selectedType == "INCOME") Color(0xFF388E3C) else Color(0xFFD32F2F))

    // Reset categories when type changes (unless editing)
    LaunchedEffect(selectedType) {
        if (!isEditing || selectedType != ruleToEdit?.type) {
            if (selectedType == "INCOME") {
                selectedMainCat = "Income"; selectedSubCategory = "Salary"
            } else {
                selectedMainCat = "Food & Drinks"; selectedSubCategory = "Groceries"
            }
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
                val cal = Calendar.getInstance().apply {
                    timeInMillis = date
                    set(Calendar.HOUR, if (hour == 12) 0 else hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.AM_PM, if (am) Calendar.AM else Calendar.PM)
                }
                selectedDateMillis = cal.timeInMillis
                selectedHour = hour; selectedMinute = minute; isAm = am
                showDateTimePicker = false
            }
        )
    }

    if (showCategoryPicker) {
        CategoryNestedPicker(
            viewModel = viewModel,
            isExpense = (selectedType == "EXPENSE"),
            onDismiss = { showCategoryPicker = false },
            onConfirmed = { main, sub ->
                selectedMainCat = main
                selectedSubCategory = sub
                showCategoryPicker = false
            }
        )
    }

    if (showWalletPicker) {
        RecurringWalletPickerDialog(
            wallets = wallets,
            currentWalletId = selectedWalletId, // [NEW] Pass the current ID to show the green border
            onDismiss = { showWalletPicker = false },
            onWalletSelected = { walletId ->
                selectedWalletId = walletId
                showWalletPicker = false
            }
        )
    }

    if (showCustomFreqDialog) {
        CustomFrequencyDialog(
            onDismiss = { showCustomFreqDialog = false },
            onConfirm = { customFreq ->
                frequency = customFreq
                showCustomFreqDialog = false
            }
        )
    }

    // --- UI CONTENT ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if(isEditing) "Edit Auto-Pay" else "New Auto-Pay", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        // Manual Back Button Click
                        viewModel.clearRecurringEdit()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0 && selectedWalletId.isNotBlank()) {
                        viewModel.addRecurringRule(RecurringRule(
                            id = ruleToEdit?.id ?: "",
                            title = title,
                            amount = amt,
                            frequency = frequency,
                            walletId = selectedWalletId,
                            category = "$selectedMainCat -> $selectedSubCategory",
                            nextRunDate = selectedDateMillis,
                            type = selectedType
                        ))
                        onSaveSuccess()
                    } else {
                        Toast.makeText(context, "Please fill all details", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor)
            ) {
                Text("SAVE SCHEDULE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {

            // 1. TYPE SWITCHER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f), CircleShape)
                    .padding(4.dp)
            ) {
                listOf("EXPENSE", "INCOME").forEach { type ->
                    val selected = selectedType == type
                    val bgColor = if (selected) themeColor else Color.Transparent
                    val textColor = if (selected) Color.White else Color.Gray

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(bgColor)
                            .clickable { selectedType = type },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if(type=="EXPENSE") "Expense" else "Income", fontWeight = FontWeight.Bold, color = textColor)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // 2. TITLE & AMOUNT
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Title (e.g. Netflix, Rent)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { /* Moves focus automatically */ })
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = amount, onValueChange = { if(it.all { c -> c.isDigit() || c == '.' }) amount = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor, focusedLabelColor = themeColor),
                leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = themeColor) }
            )

            Spacer(Modifier.height(32.dp))

            // 3. FREQUENCY
            Text("How often?", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val standardOptions = listOf("Daily", "Weekly", "Monthly", "Yearly")
                standardOptions.forEach { freq ->
                    val selected = frequency == freq
                    val bgColor by animateColorAsState(if (selected) themeColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    val borderColor by animateColorAsState(if (selected) themeColor else Color.Transparent)
                    val textColor by animateColorAsState(if (selected) themeColor else Color.Gray)

                    Surface(
                        onClick = { frequency = freq },
                        shape = RoundedCornerShape(12.dp),
                        color = bgColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (selected) {
                                Icon(Icons.Default.Check, null, tint = textColor, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(freq, color = textColor, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                // Custom Option
                val isCustom = frequency !in standardOptions
                val customBg by animateColorAsState(if (isCustom) themeColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                Surface(
                    onClick = { showCustomFreqDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = customBg,
                    border = BorderStroke(1.dp, if(isCustom) themeColor else Color.Transparent),
                    modifier = Modifier.height(40.dp)
                ) {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isCustom) frequency else "Custom", color = if(isCustom) themeColor else Color.Gray, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 4. CATEGORY & DATE & WALLET
            Text("Details", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(Modifier.height(12.dp))

            // Category
            Card(
                onClick = { showCategoryPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).background(themeColor, CircleShape), Alignment.Center) {
                        Icon(viewModel.getIconForCategory(selectedSubCategory), null, tint = Color.White)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(selectedMainCat, fontWeight = FontWeight.Bold)
                        Text(selectedSubCategory, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Date
            Card(
                onClick = { showDateTimePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).background(Color.LightGray.copy(0.3f), CircleShape), Alignment.Center) {
                        Icon(Icons.Default.CalendarToday, null, tint = Color.Gray)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Start / Next Payment", fontWeight = FontWeight.Bold)
                        Text(SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()).format(Date(selectedDateMillis)), style = MaterialTheme.typography.labelSmall, color = themeColor)
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Wallet
            val selectedWallet = wallets.find { it.id == selectedWalletId }
            val bankName = selectedWallet?.bankName?.ifBlank { "Wallet" } ?: "Select Account"
            val last4 = if(selectedWallet != null && selectedWallet.cardNumber.isNotBlank()) " •••• ${selectedWallet.cardNumber.takeLast(4)}" else ""

            Card(
                onClick = { showWalletPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)),
                border = BorderStroke(1.dp, Color.Gray.copy(0.2f))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).background(Color.LightGray.copy(0.3f), CircleShape), Alignment.Center) {
                        Icon(Icons.Default.AccountBalanceWallet, null, tint = Color.Gray)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(bankName, fontWeight = FontWeight.Bold)
                        Text("${selectedWallet?.name ?: "Tap to choose"}$last4", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Spacer(Modifier.weight(1f))
                    if (selectedWallet != null) {
                        Text(CurrencyUtils.formatINR(selectedWallet.balance), fontWeight = FontWeight.Bold, color = themeColor)
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}