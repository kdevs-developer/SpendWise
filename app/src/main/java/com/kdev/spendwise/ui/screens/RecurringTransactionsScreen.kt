package com.kdev.spendwise.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kdev.spendwise.data.RecurringRule
import com.kdev.spendwise.data.Wallet
import com.kdev.spendwise.ui.MainViewModel
import com.kdev.spendwise.ui.components.CategoryNestedPicker
import com.kdev.spendwise.ui.components.TransactionDateTimePickerDialog
import com.kdev.spendwise.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var showAddSheet by remember { mutableStateOf(false) }

    // States for Edit/Delete
    var ruleToEdit by remember { mutableStateOf<RecurringRule?>(null) }
    var ruleToDelete by remember { mutableStateOf<RecurringRule?>(null) }

    // Calculate Monthly Totals
    val (monthlyIncome, monthlyExpense) = remember(viewModel.recurringRules) {
        var inc = 0.0
        var exp = 0.0
        viewModel.recurringRules.forEach { rule ->
            val amt = rule.amount
            val monthlyAmt = when {
                rule.frequency == "Daily" -> amt * 30
                rule.frequency == "Weekly" -> amt * 4
                rule.frequency == "Yearly" -> amt / 12
                rule.frequency.startsWith("Every") -> amt
                else -> amt
            }
            if (rule.type == "INCOME") inc += monthlyAmt else exp += monthlyAmt
        }
        inc to exp
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Recurring & Bills", fontWeight = FontWeight.Bold)
                        Text("Manage subscriptions & income", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)), Alignment.Center) {
                            Icon(Icons.Default.ArrowBack, null)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    ruleToEdit = null
                    showAddSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("New Rule", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // --- SUMMARY CARD ---
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Fixed Expenses", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Spacer(Modifier.height(4.dp))
                        Text(CurrencyUtils.formatINR(monthlyExpense), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                    }
                    Box(Modifier.width(1.dp).height(40.dp).background(Color.LightGray.copy(0.5f)))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("Fixed Income", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Spacer(Modifier.height(4.dp))
                        Text(CurrencyUtils.formatINR(monthlyIncome), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF388E3C))
                    }
                }
            }

            // --- LIST ---
            if (viewModel.recurringRules.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EventRepeat, null, Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(Modifier.height(16.dp))
                        Text("No recurring rules set", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(viewModel.recurringRules) { rule ->
                        RecurringRuleItem(
                            rule = rule,
                            viewModel = viewModel,
                            onClick = {
                                ruleToEdit = rule
                                showAddSheet = true
                            },
                            onDelete = {
                                ruleToDelete = rule // Trigger Dialog
                            }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // --- DIALOGS ---

    // 1. Delete Confirmation
    if (ruleToDelete != null) {
        PremiumDeleteDialog(
            title = "Delete Rule?",
            message = "Are you sure you want to stop the recurring '${ruleToDelete!!.title}' transaction? This cannot be undone.",
            onDismiss = { ruleToDelete = null },
            onConfirm = {
                viewModel.deleteRecurringRule(ruleToDelete!!.id)
                ruleToDelete = null
            }
        )
    }

    // 2. Add/Edit Sheet
    if (showAddSheet || ruleToEdit != null) {
        AddRecurringBottomSheet(
            wallets = viewModel.wallets,
            viewModel = viewModel,
            initialRule = ruleToEdit,
            onDismiss = {
                showAddSheet = false
                ruleToEdit = null
            },
            onSave = { rule ->
                viewModel.addRecurringRule(rule)
                showAddSheet = false
                ruleToEdit = null
            }
        )
    }
}

// --- NEW COMPONENT: PREMIUM DELETE ALERT ---
@Composable
fun PremiumDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            modifier = Modifier.width(320.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Warning Icon Container
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color(0xFFFFEBEE), CircleShape), // Light Red background
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F), // Dark Red icon
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Text Content
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(Modifier.height(32.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel Button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }

                    // Delete Button
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("Delete", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// --- PREMIUM LIST ITEM (Clickable for Edit) ---
@Composable
fun RecurringRuleItem(
    rule: RecurringRule,
    viewModel: MainViewModel,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryName = rule.category.substringBefore(" -> ")
    val icon = viewModel.getIconForCategory(categoryName)
    val isIncome = rule.type == "INCOME"

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(if (isIncome) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    tint = if(isIncome) Color(0xFF2E7D32) else Color(0xFFC62828),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            // Details
            Column(Modifier.weight(1f)) {
                Text(rule.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            rule.frequency,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    val date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(rule.nextRunDate))
                    Text("Next: $date", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }

            // Amount & Delete
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if(isIncome) "+ " else "- ") + CurrencyUtils.formatINR(rule.amount),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = if(isIncome) Color(0xFF2E7D32) else Color.Black
                )
                // Stop propagation to prevent opening edit sheet when clicking delete
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp).padding(top = 4.dp)) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(0.6f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// --- ADD/EDIT RULE BOTTOM SHEET ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddRecurringBottomSheet(
    wallets: List<Wallet>,
    viewModel: MainViewModel,
    initialRule: RecurringRule? = null,
    onDismiss: () -> Unit,
    onSave: (RecurringRule) -> Unit
) {
    val context = LocalContext.current
    val isEditing = initialRule != null

    // Initialize States (Prefill if editing)
    var selectedType by remember { mutableStateOf(initialRule?.type ?: "EXPENSE") }
    var title by remember { mutableStateOf(initialRule?.title ?: "") }
    var amount by remember { mutableStateOf(if (isEditing) initialRule!!.amount.toString() else "") }
    var frequency by remember { mutableStateOf(initialRule?.frequency ?: "Monthly") }
    var selectedWalletId by remember { mutableStateOf(initialRule?.walletId ?: wallets.firstOrNull()?.id ?: "") }

    // Parse Category
    val catParts = initialRule?.category?.split(" -> ")
    var selectedMainCat by remember { mutableStateOf(catParts?.getOrNull(0) ?: if(initialRule?.type == "INCOME") "Income" else "Food & Drinks") }
    var selectedSubCategory by remember { mutableStateOf(catParts?.getOrNull(1) ?: if(initialRule?.type == "INCOME") "Salary" else "Groceries") }

    // Date Logic
    val currentCalendar = remember { Calendar.getInstance() }
    var selectedDateMillis by remember { mutableLongStateOf(initialRule?.nextRunDate ?: currentCalendar.timeInMillis) }

    // Time Picker States
    val initialCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
    var selectedHour by remember { mutableIntStateOf(if (initialCal.get(Calendar.HOUR) == 0) 12 else initialCal.get(Calendar.HOUR)) }
    var selectedMinute by remember { mutableIntStateOf(initialCal.get(Calendar.MINUTE)) }
    var isAm by remember { mutableStateOf(initialCal.get(Calendar.AM_PM) == Calendar.AM) }

    // Visibility Flags
    var showCustomFreqDialog by remember { mutableStateOf(false) }
    var showWalletPicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showDateTimePicker by remember { mutableStateOf(false) }

    val themeColor by animateColorAsState(if(selectedType == "INCOME") Color(0xFF388E3C) else Color(0xFFD32F2F))

    // Reset Category when Type changes (Only if NOT editing initially to avoid overwriting existing data)
    LaunchedEffect(selectedType) {
        if (!isEditing) {
            if (selectedType == "INCOME") {
                selectedMainCat = "Income"
                selectedSubCategory = "Salary"
            } else {
                selectedMainCat = "Food & Drinks"
                selectedSubCategory = "Groceries"
            }
        }
    }

    // --- DIALOGS (Keep same implementations) ---
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
        WalletListPickerDialog(
            wallets = wallets,
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

    // --- SHEET CONTENT ---
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if(isEditing) "Edit Recurring Rule" else "New Recurring Rule",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // TYPE TOGGLE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color.LightGray.copy(0.2f), CircleShape)
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

            Spacer(Modifier.height(24.dp))

            // 1. Title & Amount
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Title (e.g. ${if(selectedType=="INCOME") "Salary" else "Netflix"})") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = amount, onValueChange = { if(it.all { c -> c.isDigit() || c == '.' }) amount = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = themeColor, focusedLabelColor = themeColor)
            )

            Spacer(Modifier.height(24.dp))

            // 2. Frequency Chips (Scrollable Row)
            Text("Frequency", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val standardOptions = listOf("Weekly", "Monthly", "Yearly")

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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            if (selected) {
                                Icon(Icons.Default.Check, null, tint = textColor, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(freq, color = textColor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Custom Option
                val isCustom = frequency !in standardOptions
                val customBg by animateColorAsState(if (isCustom) themeColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                val customBorder by animateColorAsState(if (isCustom) themeColor else Color.Transparent)
                val customContentColor by animateColorAsState(if (isCustom) themeColor else Color.Gray)

                Surface(
                    onClick = { showCustomFreqDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = customBg,
                    border = BorderStroke(1.dp, customBorder),
                    modifier = Modifier.height(40.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(if (isCustom) Icons.Default.Edit else Icons.Default.Add, null, tint = customContentColor, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isCustom) frequency else "Custom", color = customContentColor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 3. Category Selector
            Text("Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Card(
                onClick = { showCategoryPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)),
                border = BorderStroke(1.dp, Color.Gray.copy(0.2f))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(32.dp).background(themeColor, CircleShape), Alignment.Center) {
                        Icon(viewModel.getIconForCategory(selectedSubCategory), null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(selectedMainCat, fontWeight = FontWeight.Bold)
                        Text(selectedSubCategory, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                }
            }

            Spacer(Modifier.height(16.dp))

            // 4. Start Date
            Text(if (isEditing) "Next Payment Date" else "First Payment Date", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Card(
                onClick = { showDateTimePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)),
                border = BorderStroke(1.dp, Color.Gray.copy(0.2f))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, null, tint = Color.Gray)
                    Spacer(Modifier.width(12.dp))
                    Text(SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()).format(Date(selectedDateMillis)), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))

            // 5. Wallet
            Text(if(selectedType=="INCOME") "Deposit To" else "Pay From", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            val selectedWallet = wallets.find { it.id == selectedWalletId }
            val displayBank = selectedWallet?.bankName?.ifBlank { selectedWallet.name } ?: "Select Account"
            val displayBalance = selectedWallet?.balance ?: 0.0

            Card(
                onClick = { showWalletPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)),
                border = BorderStroke(1.dp, Color.Gray.copy(0.2f))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalance, null, tint = Color.Gray)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(displayBank, fontWeight = FontWeight.Bold)
                        Text(CurrencyUtils.formatINR(displayBalance), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, null)
                }
            }

            Spacer(Modifier.height(32.dp))

            // Save Button
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0 && selectedWalletId.isNotBlank()) {
                        onSave(RecurringRule(
                            id = initialRule?.id ?: "", // Preserve ID if editing, else empty
                            title = title,
                            amount = amt,
                            frequency = frequency,
                            walletId = selectedWalletId,
                            category = "$selectedMainCat -> $selectedSubCategory",
                            nextRunDate = selectedDateMillis,
                            type = selectedType
                        ))
                    } else {
                        Toast.makeText(context, "Please fill all details", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor)
            ) {
                Text(if(isEditing) "Update Rule" else "Save Rule", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// --- NEW PREMIUM CUSTOM FREQUENCY DIALOG (White Background) ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomFrequencyDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var count by remember { mutableIntStateOf(1) }
    val units = listOf("Days", "Weeks", "Months", "Years")
    var unitIndex by remember { mutableIntStateOf(2) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            tonalElevation = 6.dp,
            modifier = Modifier.width(320.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Custom Interval", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(Modifier.height(8.dp))

                val selectedUnit = units[unitIndex]
                val displayUnit = if (count == 1) selectedUnit.dropLast(1) else selectedUnit

                Text(
                    text = "Repeats every $count $displayUnit",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(32.dp))

                Text("Frequency", style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilledIconButton(
                        onClick = { if (count > 1) count-- },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFF5F5F5)),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Remove, null, tint = Color.Black)
                    }

                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    FilledIconButton(
                        onClick = { if (count < 99) count++ },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White)
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text("Duration", style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8F9FA), RoundedCornerShape(16.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    IconButton(onClick = { unitIndex = (unitIndex - 1 + units.size) % units.size }) {
                        Icon(Icons.Default.ChevronLeft, null, tint = Color.Gray)
                    }

                    Text(
                        text = units[unitIndex],
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = { unitIndex = (unitIndex + 1) % units.size }) {
                        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                    }
                }

                Spacer(Modifier.height(32.dp))

                Row(Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                    ) { Text("Cancel") }

                    Spacer(Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val finalUnit = units[unitIndex]
                            val formatted = if (count == 1) "Every ${finalUnit.dropLast(1)}" else "Every $count $finalUnit"
                            onConfirm(formatted)
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text("Set") }
                }
            }
        }
    }
}

// --- REUSED COMPONENT: WALLET LIST PICKER ---
@Composable
fun WalletListPickerDialog(
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
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                    Text("Select Account", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

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