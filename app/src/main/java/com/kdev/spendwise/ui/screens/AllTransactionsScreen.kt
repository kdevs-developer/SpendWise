package com.kdev.spendwise.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kdev.spendwise.data.Expense
import com.kdev.spendwise.data.Wallet
import com.kdev.spendwise.ui.MainViewModel
import com.kdev.spendwise.ui.components.PremiumAlertDialog
import com.kdev.spendwise.ui.components.PremiumFilterScreen
import com.kdev.spendwise.util.CurrencyUtils
import com.kdev.spendwise.util.PdfUtils
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
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
    var selectedWalletId by remember { mutableStateOf<String?>(null) }

    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    // --- DATE LOGIC ---
    val (startDate, endDate) = remember(selectedTimeFilter, monthOffset, customDateRange) {
        calculateDateRange(selectedTimeFilter, monthOffset, customDateRange)
    }

    // --- FILTERING LOGIC ---
    val filteredViewTransactions = expenses.filter { expense ->
        val dateMatch = (startDate == null || expense.date >= startDate) && (endDate == null || expense.date <= endDate)
        val searchMatch = searchQuery.isEmpty() || expense.title.contains(searchQuery, ignoreCase = true) || expense.category.contains(searchQuery, ignoreCase = true)
        val categoryMatch = selectedCategories.isEmpty() || expense.category in selectedCategories
        val walletMatch = selectedWalletId == null || expense.walletId == selectedWalletId || expense.toWalletId == selectedWalletId

        dateMatch && searchMatch && categoryMatch && walletMatch
    }

    // --- SMART CALCULATIONS ---
    val periodIncome = filteredViewTransactions.filter { it.amount > 0 }.sumOf { it.amount }
    val periodExpense = filteredViewTransactions.filter { it.amount < 0 }.sumOf { abs(it.amount) }
    val periodNet = periodIncome - periodExpense

    val topCategoryPair = remember(filteredViewTransactions) {
        filteredViewTransactions
            .filter { it.amount < 0 }
            .groupBy { it.category.substringBefore(" -> ") }
            .mapValues { entry -> entry.value.sumOf { abs(it.amount) } }
            .maxByOrNull { it.value }
    }

    val dailyAverage = remember(startDate, endDate, periodExpense) {
        if (startDate != null && endDate != null && periodExpense > 0) {
            val now = System.currentTimeMillis()
            val effectiveEnd = if (endDate > now) now else endDate
            val diff = effectiveEnd - startDate
            val days = TimeUnit.MILLISECONDS.toDays(diff).coerceAtLeast(1)
            periodExpense / days
        } else 0.0
    }

    val groupedExpenses = filteredViewTransactions.groupBy { formatGroupDate(it.date) }

    // --- [CHANGE] WRAP EVERYTHING IN A BOX TO HANDLE LAYERING ---
    Box(modifier = Modifier.fillMaxSize()) {

        // 1. MAIN CONTENT SCAFFOLD
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text("History", fontWeight = FontWeight.Bold)
                                val subtitle = if(selectedWalletId != null) {
                                    wallets.find { it.id == selectedWalletId }?.bankName ?: "Specific Wallet"
                                } else {
                                    "Your Activity Log"
                                }
                                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
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
                                    tint = if (isSearchVisible) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                                    contentDescription = "Search"
                                )
                            }
                            IconButton(onClick = {
                                val walletMap = wallets.associate { it.id to it.bankName }
                                PdfUtils.exportTransactionsToPdf(
                                    context = context,
                                    transactions = filteredViewTransactions,
                                    walletMap = walletMap,
                                    initialBalance = 0.0,
                                    totalIncome = periodIncome,
                                    totalExpense = periodExpense,
                                    closingBalance = periodNet,
                                    reportTitle = "Report: ${getDateRangeLabel(selectedTimeFilter, startDate, endDate)}"
                                )
                            }) {
                                Icon(Icons.Default.Download, "PDF", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { showFilterSheet = true }) {
                                val isFiltered = selectedCategories.isNotEmpty() || selectedTimeFilter != TimeFilter.Month || selectedWalletId != null
                                Box {
                                    Icon(Icons.Default.Tune, null, tint = if (isFiltered) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                                    if (isFiltered) Badge(modifier = Modifier.size(8.dp).align(Alignment.TopEnd))
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                    )

                    if (selectedTimeFilter == TimeFilter.Month) {
                        MonthSelector(
                            currentOffset = monthOffset,
                            onPrevious = { monthOffset-- },
                            onNext = { if (monthOffset < 0) monthOffset++ }
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                Column(
                    modifier = Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { focusManager.clearFocus() }
                ) {
                    // Insight Card
                    SmartInsightCard(
                        income = periodIncome,
                        expense = periodExpense,
                        net = periodNet,
                        dailyAvg = dailyAverage,
                        topCategoryName = topCategoryPair?.key,
                        topCategoryAmount = topCategoryPair?.value,
                        topCategoryIcon = if (topCategoryPair != null) viewModel.getIconForCategory(topCategoryPair.key) else null
                    )

                    AnimatedVisibility(visible = isSearchVisible) {
                        SearchBarView(searchQuery) { searchQuery = it }
                    }

                    if (selectedCategories.isNotEmpty() || selectedWalletId != null) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if(selectedWalletId != null) {
                                val wName = wallets.find { it.id == selectedWalletId }?.bankName ?: "Wallet"
                                InputChip(
                                    selected = true,
                                    onClick = { selectedWalletId = null },
                                    label = { Text(wName) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) },
                                    colors = InputChipDefaults.inputChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
                                )
                            }
                            if(selectedCategories.isNotEmpty()) {
                                InputChip(
                                    selected = true,
                                    onClick = { selectedCategories = emptySet() },
                                    label = { Text("Categories (${selectedCategories.size})") },
                                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) },
                                    colors = InputChipDefaults.inputChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
                                )
                            }
                        }
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
                                        onEdit = {
                                            viewModel.expenseToEdit = expense
                                            onEditNavigate()
                                        },
                                        onDelete = { expenseToDelete = expense }
                                    )
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }

                if (expenseToDelete != null) {
                    PremiumAlertDialog(
                        title = "Delete Transaction?",
                        message = "Are you sure you want to remove this transaction?",
                        confirmText = "Delete",
                        dismissText = "Cancel",
                        icon = Icons.Default.DeleteForever,
                        confirmButtonColor = MaterialTheme.colorScheme.error,
                        iconColor = MaterialTheme.colorScheme.error,
                        onConfirm = {
                            viewModel.deleteExpense(expenseToDelete!!)
                            expenseToDelete = null
                        },
                        onDismiss = { expenseToDelete = null }
                    )
                }
            }
        }

        // [CHANGE] 2. FILTER SCREEN OVERLAY (Outside Scaffold, inside parent Box)
        // This ensures it covers the TopBar and everything else.
        AnimatedVisibility(
            visible = showFilterSheet,
            enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }), // Slide from Right
            exit = slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }),
            modifier = Modifier.zIndex(2f) // Force on top
        ) {
            //
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface) // Solid Background
            ) {
                PremiumFilterScreen(
                    viewModel = viewModel,
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
}

// ============================================================================================
// NEW COMPONENT: Smart Insight Card
// ============================================================================================



@Composable
fun SmartInsightCard(
    income: Double,
    expense: Double,
    net: Double,
    dailyAvg: Double,
    topCategoryName: String?,
    topCategoryAmount: Double?,
    topCategoryIcon: ImageVector?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Row 1: Net Balance & Totals
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Period Net", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(
                        CurrencyUtils.formatINR(net),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Mini Bar
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("+${CurrencyUtils.formatCompact(income)}", color = Color(0xFF388E3C), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("Income", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Box(Modifier.width(1.dp).height(24.dp).background(Color.LightGray.copy(0.5f)))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("-${CurrencyUtils.formatCompact(expense)}", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("Expense", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(modifier = Modifier.alpha(0.1f))
            Spacer(Modifier.height(16.dp))

            // Row 2: Smart Insights (Daily Avg & Top Category)
            Row(Modifier.fillMaxWidth()) {
                // Daily Avg
                Row(
                    Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(36.dp).background(MaterialTheme.colorScheme.secondaryContainer.copy(0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Speed, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Daily Avg", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(CurrencyUtils.formatINR(dailyAvg), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Top Category
                if (topCategoryName != null && topCategoryAmount != null) {
                    Row(
                        Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(36.dp).background(MaterialTheme.colorScheme.errorContainer.copy(0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(topCategoryIcon ?: Icons.Default.Category, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Top Spend", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(topCategoryName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

// --- UTILS ---
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
fun MonthSelector(currentOffset: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
    val displayDate = remember(currentOffset) { val cal = Calendar.getInstance(); cal.add(Calendar.MONTH, currentOffset); SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) { IconButton(onClick = onPrevious) { Icon(Icons.Default.ChevronLeft, null) }; Text(displayDate, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); IconButton(onClick = onNext, enabled = currentOffset < 0) { Icon(Icons.Default.ChevronRight, null, tint = if(currentOffset < 0) MaterialTheme.colorScheme.onSurface else Color.LightGray) } }
}

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
                    false // Don't dismiss immediately, let the dialog handle it
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit()
                    false // Reset state after triggering edit
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

            // Dynamic Color: Blue for Edit, Red for Delete
            val color = if (direction == SwipeToDismissBoxValue.StartToEnd) Color(0xFF2196F3) else MaterialTheme.colorScheme.error

            // Alignment: Start for Edit, End for Delete
            val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd

            // Icon: Edit vs Delete
            val icon = if (direction == SwipeToDismissBoxValue.StartToEnd) Icons.Default.Edit else Icons.Default.Delete

            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp)) // Matches card shape
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        content = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                onClick = onEdit // Tap acts as Edit
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

    // Color Logic: Red for Expense, Green for Income/Transfer
    val color = if (isExpense) Color(0xFFD32F2F) else Color(0xFF388E3C)

    // Resolve Wallet Name
    val walletName = wallets.find { it.id == expense.walletId }?.let {
        if(it.bankName.isNotBlank()) it.bankName else it.name
    } ?: "Wallet"

    // Resolve Category Name (e.g., "Food & Drinks -> Groceries" becomes just "Groceries")
    val category = expense.category.substringAfter(" -> ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- 1. ICON ---
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = viewModel.getIconForCategory(category),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        // --- 2. DETAILS (Title + Bank) ---
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.title.ifEmpty { category },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Bank Tag
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = Color.Gray
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = walletName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }

        // --- 3. AMOUNT & TIME ---
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (isExpense) "-" else "+"} ${CurrencyUtils.formatINR(displayAmount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(expense.date)),
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray
            )
        }
    }
}
@Composable
fun SearchBarView(query: String, onQueryChange: (String) -> Unit) { OutlinedTextField(value = query, onValueChange = onQueryChange, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), placeholder = { Text("Search transactions...") }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) }, shape = RoundedCornerShape(16.dp), singleLine = true) }

@Composable
fun DateHeader(text: String) { Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), Alignment.Center) { Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant.copy(0.8f)) { Text(text, Modifier.padding(horizontal = 16.dp, vertical = 6.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) } } }


@Composable
fun EmptyStateView() { Box(Modifier.fillMaxWidth().height(300.dp), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.ReceiptLong, null, Modifier.size(64.dp), tint = Color.Gray.copy(0.5f)); Spacer(Modifier.height(16.dp)); Text("No transactions found for this period", color = Color.Gray) } } }
fun formatGroupDate(timestamp: Long): String { val d = Date(timestamp); val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()); return sdf.format(d) }