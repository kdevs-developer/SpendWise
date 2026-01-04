package com.kdev.spendwise.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdev.spendwise.data.Expense
import com.kdev.spendwise.data.Wallet
import com.kdev.spendwise.ui.MainViewModel
import com.kdev.spendwise.util.CurrencyUtils
import com.kdev.spendwise.util.PdfUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

enum class TimeFilter {
    Month, Week, Year, All, Custom
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AllTransactionsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onEditNavigate: () -> Unit
) {
    val expenses by viewModel.filteredExpenses.collectAsState(initial = emptyList())
    val wallets = viewModel.wallets
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // --- STATE ---
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    // Filter States
    var selectedTimeFilter by remember { mutableStateOf(TimeFilter.Month) }
    var monthOffset by remember { mutableIntStateOf(0) }
    var customDateRange by remember { mutableStateOf<Pair<Long?, Long?>>(null to null) }
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    val allCategories = remember(expenses) { expenses.map { it.category }.distinct().sorted() }

    // --- DATE LOGIC ---
    val (startDate, endDate) = remember(selectedTimeFilter, monthOffset, customDateRange) {
        calculateDateRange(selectedTimeFilter, monthOffset, customDateRange)
    }

    // --- FILTERING ---
    val periodTransactions = expenses.filter {
        (startDate == null || it.date >= startDate) && (endDate == null || it.date <= endDate)
    }

    val filteredViewTransactions = periodTransactions.filter { expense ->
        val matchesSearch = expense.title.contains(searchQuery, ignoreCase = true) ||
                expense.category.contains(searchQuery, ignoreCase = true)
        val matchesCategory = if (selectedCategories.isEmpty()) true else expense.category in selectedCategories
        matchesSearch && matchesCategory
    }

    // --- CALCULATIONS ---
    val pastTransactions = if (startDate != null) {
        expenses.filter { it.date < startDate }
    } else emptyList()

    val openingBalance = if (selectedTimeFilter == TimeFilter.All) 0.0 else pastTransactions.sumOf {
        if(it.type == "EXPENSE" || (it.type == "TRANSFER" && it.amount < 0)) -abs(it.amount) else abs(it.amount)
    }

    val periodIncome = filteredViewTransactions.filter { it.amount > 0 }.sumOf { it.amount }
    val periodExpense = filteredViewTransactions.filter { it.amount < 0 }.sumOf { abs(it.amount) }
    val closingBalance = openingBalance + periodIncome - periodExpense

    val groupedExpenses = filteredViewTransactions.groupBy { formatGroupDate(it.date) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Transactions", fontWeight = FontWeight.Bold)
                        Text(getDateRangeLabel(selectedTimeFilter, startDate, endDate), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = {
                        isSearchVisible = !isSearchVisible
                        if (!isSearchVisible) searchQuery = ""
                    }) {
                        Icon(
                            imageVector = if (isSearchVisible) Icons.Default.SearchOff else Icons.Default.Search,
                            contentDescription = "Toggle Search",
                            tint = if (isSearchVisible) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }

                    IconButton(onClick = {
                        PdfUtils.exportTransactionsToPdf(
                            context = context,
                            transactions = filteredViewTransactions,
                            initialBalance = openingBalance,
                            totalIncome = periodIncome,
                            totalExpense = periodExpense,
                            availableBalance = closingBalance
                        )
                    }) {
                        Icon(Icons.Default.Download, "Export PDF", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(onClick = { showFilterSheet = true }) {
                        val isFiltered = selectedCategories.isNotEmpty() || selectedTimeFilter != TimeFilter.Month
                        Box {
                            Icon(Icons.Default.FilterList, null, tint = if (isFiltered) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                            if (isFiltered) Badge(modifier = Modifier.size(8.dp).align(Alignment.TopEnd))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(
                modifier = Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { focusManager.clearFocus() }
            ) {
                if (selectedTimeFilter == TimeFilter.Month) {
                    MonthSelector(
                        currentOffset = monthOffset,
                        onPrevious = { monthOffset-- },
                        onNext = { if (monthOffset < 0) monthOffset++ }
                    )
                }

                SummaryHeader(opening = openingBalance, income = periodIncome, expense = periodExpense, closing = closingBalance)

                AnimatedVisibility(
                    visible = isSearchVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SearchBarView(searchQuery) { searchQuery = it }
                }

                if (selectedCategories.isNotEmpty()) {
                    FilterStatusRow(selectedCategories, { selectedCategories = emptySet() })
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (groupedExpenses.isEmpty()) {
                        item { EmptyStateView() }
                    } else {
                        groupedExpenses.forEach { (dateHeader, expensesForDate) ->
                            stickyHeader { DateHeader(dateHeader) }
                            items(items = expensesForDate, key = { it.id }) { expense ->
                                SwipeableTransactionItem(
                                    expense = expense,
                                    viewModel = viewModel,
                                    wallets = wallets,
                                    modifier = Modifier.animateItem(),
                                    onEdit = onEditNavigate,
                                    onDelete = { expenseToDelete = expense }
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            if (expenseToDelete != null) {
                PremiumDeleteDialog(
                    onConfirm = { viewModel.deleteExpense(expenseToDelete!!); expenseToDelete = null },
                    onDismiss = { expenseToDelete = null }
                )
            }
        }

        if (showFilterSheet) {
            FilterBottomSheet(
                allCategories = allCategories,
                currentCategories = selectedCategories,
                selectedTimeFilter = selectedTimeFilter,
                customDateRange = customDateRange,
                onApply = { cats, timeFilter, range ->
                    selectedCategories = cats
                    selectedTimeFilter = timeFilter
                    customDateRange = range
                    if (timeFilter == TimeFilter.Month && selectedTimeFilter != TimeFilter.Month) monthOffset = 0
                    showFilterSheet = false
                },
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}

// --- REDESIGNED TRANSACTION ITEM ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTransactionItem(
    expense: Expense,
    viewModel: MainViewModel,
    wallets: List<Wallet>,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    false
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    viewModel.expenseToEdit = expense
                    onEdit()
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            // Only show background color on swipe action
            val color = if (direction == SwipeToDismissBoxValue.StartToEnd) Color(0xFF2196F3) else MaterialTheme.colorScheme.error
            val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            val icon = if (direction == SwipeToDismissBoxValue.StartToEnd) Icons.Default.Edit else Icons.Default.Delete

            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment
            ) {
                Icon(icon, null, tint = Color.White)
            }
        },
        content = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                TransactionItem(expense, viewModel, wallets)
            }
        }
    )
}

@Composable
fun TransactionItem(expense: Expense, viewModel: MainViewModel, wallets: List<Wallet>) {
    val isExpense = expense.amount < 0
    val displayAmount = abs(expense.amount)

    // RED / GREEN Logic
    val color = if (isExpense) Color(0xFFD32F2F) else Color(0xFF388E3C)
    val sign = if (isExpense) "- " else "+ "

    // NEUTRAL Icon Background
    val iconBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    // Data Resolution
    val walletName = wallets.find { it.id == expense.walletId }?.let {
        if(it.bankName.isNotBlank()) it.bankName else it.name
    } ?: "Wallet"

    val categoryParts = expense.category.split(" -> ")
    val mainCategory = categoryParts.getOrElse(0) { "General" }
    val subCategory = categoryParts.getOrElse(1) { "" }

    // UPDATED: Use Sub-category for icon if available, else Main
    val iconTarget = if (subCategory.isNotEmpty()) subCategory else mainCategory
    val displayCategory = if (subCategory.isNotEmpty()) "$mainCategory > $subCategory" else mainCategory

    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(expense.date))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // --- 1. ICON (Using Sub-category) ---
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = viewModel.getIconForCategory(iconTarget), // Updated here
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        // --- 2. MIDDLE (Vertical Stack: Title -> Category -> Wallet) ---
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Title
            Text(
                text = expense.title.ifEmpty { displayCategory },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Category > Sub
            Text(
                text = displayCategory,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Wallet Badge
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = Color.Gray
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = walletName,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // --- 3. RIGHT (Vertical Stack: Amount -> Time) ---
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            // Amount
            Text(
                text = sign + CurrencyUtils.formatINR(displayAmount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )

            // Time
            Spacer(Modifier.height(4.dp))
            Text(
                text = timeStr,
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray
            )
        }
    }
}

// --- FILTER SHEET & HELPERS (Keep existing) ---

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    allCategories: List<String>,
    currentCategories: Set<String>,
    selectedTimeFilter: TimeFilter,
    customDateRange: Pair<Long?, Long?>,
    onApply: (Set<String>, TimeFilter, Pair<Long?, Long?>) -> Unit,
    onDismiss: () -> Unit
) {
    var tempCategories by remember { mutableStateOf(currentCategories) }
    var tempTimeFilter by remember { mutableStateOf(selectedTimeFilter) }
    val dateState = rememberDateRangePickerState(initialSelectedStartDateMillis = customDateRange.first, initialSelectedEndDateMillis = customDateRange.second)
    var isCalendarExpanded by remember { mutableStateOf(selectedTimeFilter == TimeFilter.Custom) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxHeight(0.9f)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Text("Filters", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))

                Spacer(Modifier.height(24.dp))
                Text("Time Period", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeFilter.values().forEach { filter ->
                        FilterChip(
                            selected = tempTimeFilter == filter,
                            onClick = {
                                tempTimeFilter = filter
                                isCalendarExpanded = (filter == TimeFilter.Custom)
                            },
                            label = { Text(filter.name) },
                            leadingIcon = if (tempTimeFilter == filter) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null
                        )
                    }
                }

                AnimatedVisibility(visible = isCalendarExpanded) {
                    Column(Modifier.height(320.dp).padding(top = 12.dp)) {
                        DateRangePicker(state = dateState, showModeToggle = false, title = null, headline = null)
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text("Categories", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    allCategories.forEach { category ->
                        val isSelected = category in tempCategories
                        FilterChip(
                            selected = isSelected,
                            onClick = { tempCategories = if (isSelected) tempCategories - category else tempCategories + category },
                            label = { Text(category) }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            Surface(shadowElevation = 16.dp, color = MaterialTheme.colorScheme.surface) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(onClick = { tempCategories = emptySet(); tempTimeFilter = TimeFilter.Month }, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp)) {
                        Text("Reset")
                    }
                    Button(onClick = {
                        val range = if (tempTimeFilter == TimeFilter.Custom) dateState.selectedStartDateMillis to dateState.selectedEndDateMillis else null to null
                        onApply(tempCategories, tempTimeFilter, range)
                    }, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp)) {
                        Text("Apply", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun calculateDateRange(filter: TimeFilter, offset: Int, customRange: Pair<Long?, Long?>): Pair<Long?, Long?> {
    val cal = Calendar.getInstance()
    cal.firstDayOfWeek = Calendar.MONDAY
    fun startOfDay() { cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0) }
    fun endOfDay() { cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999) }

    return when (filter) {
        TimeFilter.Month -> {
            cal.add(Calendar.MONTH, offset); cal.set(Calendar.DAY_OF_MONTH, 1); startOfDay(); val start = cal.timeInMillis
            cal.add(Calendar.MONTH, 1); cal.add(Calendar.DAY_OF_MONTH, -1); endOfDay(); val end = cal.timeInMillis
            start to end
        }
        TimeFilter.Week -> {
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); startOfDay(); val start = cal.timeInMillis
            cal.add(Calendar.DAY_OF_WEEK, 6); endOfDay(); val end = cal.timeInMillis
            start to end
        }
        TimeFilter.Year -> {
            cal.set(Calendar.DAY_OF_YEAR, 1); startOfDay(); val start = cal.timeInMillis
            cal.set(Calendar.MONTH, 11); cal.set(Calendar.DAY_OF_MONTH, 31); endOfDay(); val end = cal.timeInMillis
            start to end
        }
        TimeFilter.Custom -> customRange
        TimeFilter.All -> null to null
    }
}

fun getDateRangeLabel(filter: TimeFilter, start: Long?, end: Long?): String {
    if (filter == TimeFilter.All) return "All Time"
    if (start == null || end == null) return "Unknown Range"
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return "${sdf.format(Date(start))} - ${sdf.format(Date(end))}"
}

@Composable
fun SummaryHeader(opening: Double, income: Double, expense: Double, closing: Double) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.1f)), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.1f))) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Column { Text("Opening", style = MaterialTheme.typography.labelMedium, color = Color.Gray); Text(CurrencyUtils.formatINR(opening), fontWeight = FontWeight.SemiBold) }; Column(horizontalAlignment = Alignment.End) { Text("Closing", style = MaterialTheme.typography.labelMedium, color = Color.Gray); Text(CurrencyUtils.formatINR(closing), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) } }
            HorizontalDivider(Modifier.padding(vertical = 12.dp).alpha(0.2f))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Column { Text("Income", style = MaterialTheme.typography.labelSmall, color = Color.Gray); Text("+${CurrencyUtils.formatINR(income)}", color = Color(0xFF388E3C), fontWeight = FontWeight.Bold) }; Column(horizontalAlignment = Alignment.End) { Text("Expense", style = MaterialTheme.typography.labelSmall, color = Color.Gray); Text("-${CurrencyUtils.formatINR(expense)}", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold) } }
        }
    }
}

@Composable
fun MonthSelector(currentOffset: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
    val displayDate = remember(currentOffset) { val cal = Calendar.getInstance(); cal.add(Calendar.MONTH, currentOffset); SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) { IconButton(onClick = onPrevious) { Icon(Icons.Default.ChevronLeft, null) }; Text(displayDate, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); IconButton(onClick = onNext, enabled = currentOffset < 0) { Icon(Icons.Default.ChevronRight, null, tint = if(currentOffset < 0) MaterialTheme.colorScheme.onSurface else Color.LightGray) } }
}

@Composable
fun SearchBarView(query: String, onQueryChange: (String) -> Unit) { OutlinedTextField(value = query, onValueChange = onQueryChange, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), placeholder = { Text("Search transactions...") }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) }, shape = RoundedCornerShape(16.dp), singleLine = true) }

@Composable
fun DateHeader(text: String) { Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), Alignment.Center) { Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant.copy(0.8f)) { Text(text, Modifier.padding(horizontal = 16.dp, vertical = 6.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) } } }

@Composable
fun FilterStatusRow(cats: Set<String>, onClearCat: () -> Unit) { Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { InputChip(selected = true, onClick = onClearCat, label = { Text("Categories (${cats.size})") }, trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }) } }

@Composable
fun EmptyStateView() { Box(Modifier.fillMaxWidth().height(300.dp), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.ReceiptLong, null, Modifier.size(64.dp), tint = Color.Gray.copy(0.5f)); Spacer(Modifier.height(16.dp)); Text("No transactions found for this period", color = Color.Gray) } } }

@Composable
fun PremiumDeleteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, icon = { Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.errorContainer.copy(0.2f), CircleShape), Alignment.Center) { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) } }, title = { Text("Delete Transaction?", fontWeight = FontWeight.Bold) }, text = { Text("Are you sure you want to remove this transaction?", textAlign = TextAlign.Center) }, confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) { Text("Yes, Delete", fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel", color = Color.Gray) } }, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp)) }

fun formatGroupDate(timestamp: Long): String { val d = Date(timestamp); val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()); return sdf.format(d) }