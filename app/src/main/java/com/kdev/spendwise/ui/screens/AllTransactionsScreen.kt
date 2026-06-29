package com.kdev.spendwise.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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
import com.kdev.spendwise.data.cardThemes
import com.kdev.spendwise.ui.MainViewModel
import com.kdev.spendwise.ui.components.PremiumAlertDialog
import com.kdev.spendwise.ui.components.PremiumFilterScreen
import com.kdev.spendwise.util.CurrencyUtils
import com.kdev.spendwise.util.PdfUtils
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import androidx.compose.ui.platform.LocalLocale

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
    var showCustomDateRangePicker by remember { mutableStateOf(false) }

    // Filter States
    var selectedTimeFilter by remember { mutableStateOf(TimeFilter.Month) }
    var monthOffset by remember { mutableIntStateOf(0) }
    var customDateRange by remember { mutableStateOf<Pair<Long?, Long?>>(null to null) }
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
    // Note: We removed local selectedWalletId and now directly use viewModel.selectedWalletId

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

        // Removed walletMatch because viewModel.filteredExpenses already handles it automatically
        dateMatch && searchMatch && categoryMatch
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
                                val subtitle = if (viewModel.selectedWalletId != null) {
                                    wallets.find { it.id == viewModel.selectedWalletId }?.bankName ?: "Specific Account"
                                } else {
                                    "All Accounts"
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
                                val isFiltered = selectedCategories.isNotEmpty() || selectedTimeFilter != TimeFilter.Month || viewModel.selectedWalletId != null
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

                    // --- PREMIUM HORIZONTAL WALLET SELECTOR ---
                    PremiumWalletSelector(
                        wallets = wallets,
                        selectedWalletId = viewModel.selectedWalletId,
                        onWalletSelected = { viewModel.selectWallet(it) } // Syncs directly with ViewModel
                    )

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

                    // Active Filters Display
                    if (selectedCategories.isNotEmpty() || selectedTimeFilter == TimeFilter.Custom || viewModel.selectedWalletId != null) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (viewModel.selectedWalletId != null) {
                                val wName = wallets.find { it.id == viewModel.selectedWalletId }?.bankName ?: "Wallet"
                                InputChip(
                                    selected = true,
                                    onClick = { viewModel.selectWallet(null) },
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
                            if (selectedTimeFilter == TimeFilter.Custom) {
                                InputChip(
                                    selected = true,
                                    onClick = {
                                        selectedTimeFilter = TimeFilter.Month
                                        customDateRange = null to null
                                    },
                                    label = { Text("Custom Range") },
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

                // Delete Confirmation Dialog
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

        // 2. FILTER SCREEN OVERLAY
        AnimatedVisibility(
            visible = showFilterSheet,
            enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }),
            exit = slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }),
            modifier = Modifier.zIndex(2f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                PremiumFilterScreen(
                    viewModel = viewModel,
                    currentCategories = selectedCategories,
                    selectedTimeFilter = selectedTimeFilter,
                    customDateRange = customDateRange,
                    onApply = { cats, timeFilter, range ->
                        selectedCategories = cats

                        // Intercept Custom Date logic to trigger the modal pop-up
                        if (timeFilter == TimeFilter.Custom && range.first == null) {
                            showFilterSheet = false
                            showCustomDateRangePicker = true
                        } else {
                            selectedTimeFilter = timeFilter
                            customDateRange = range
                            if (timeFilter == TimeFilter.Month && selectedTimeFilter != TimeFilter.Month) monthOffset = 0
                            showFilterSheet = false
                        }
                    },
                    onDismiss = { showFilterSheet = false }
                )
            }
        }

        // 3. PREMIUM DATE RANGE PICKER
        if (showCustomDateRangePicker) {
            PremiumDateRangePickerDialog(
                onDismiss = { showCustomDateRangePicker = false },
                onDateRangeSelected = { start, end ->
                    customDateRange = start to end
                    selectedTimeFilter = TimeFilter.Custom
                    showCustomDateRangePicker = false
                }
            )
        }
    }
}

// ============================================================================================
// COMPONENT: Premium Horizontal Wallet Selector (Left/Right Layout)
// ============================================================================================
@Composable
fun PremiumWalletSelector(
    wallets: List<Wallet>,
    selectedWalletId: String?,
    onWalletSelected: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // "All Accounts" Option
        item {
            WalletPill(
                title = "All Accounts",
                subtitle = "Overview",
                balanceStr = null, // Set to null to hide the total amount as requested
                icon = Icons.Default.AllInclusive,
                isSelected = selectedWalletId == null,
                brush = Brush.linearGradient(listOf(Color(0xFF424242), Color(0xFF1B1B1B))),
                onClick = { onWalletSelected(null) }
            )
        }

        // Individual Wallets
        items(wallets, key = { it.id }) { wallet ->
            val theme = cardThemes.getOrElse(wallet.cardThemeId) { cardThemes[0] }
            val brush = Brush.linearGradient(listOf(theme.start, theme.end))
            val fallbackTypeSubtitle = wallet.type.name.lowercase().replaceFirstChar { it.uppercase() }

            WalletPill(
                title = wallet.bankName.ifBlank { wallet.name },
                subtitle = if (wallet.cardNumber.isNotBlank()) "•••• ${wallet.cardNumber.takeLast(4)}" else fallbackTypeSubtitle,
                balanceStr = CurrencyUtils.formatINR(wallet.balance),
                icon = wallet.icon,
                isSelected = selectedWalletId == wallet.id,
                brush = brush,
                onClick = { onWalletSelected(wallet.id) }
            )
        }
    }
}

@Composable
fun WalletPill(
    title: String,
    subtitle: String,
    balanceStr: String?,
    icon: ImageVector,
    isSelected: Boolean,
    brush: Brush,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (isSelected) 1.05f else 1f, label = "pill_scale")
    val alpha by animateFloatAsState(if (isSelected) 1f else 0.5f, label = "pill_alpha")
    val borderWidth by animateDpAsState(if (isSelected) 2.dp else 0.dp, label = "pill_border")

    Card(
        modifier = Modifier
            .width(200.dp) // Adjusted width for left/right layout
            .height(85.dp)
            .scale(scale)
            .alpha(alpha)
            .clickable { onClick() }
            .border(borderWidth, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(if (isSelected) 6.dp else 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(brush)) {
            // Glossy Overlay
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.05f)))

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT SIDE: Bank Name & Account Number
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // RIGHT SIDE: Icon & Amount
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    // Icon Circle
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (balanceStr != null) {
                        Text(
                            text = balanceStr,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        // Invisible spacer to keep the layout aligned if there's no amount
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}


// ============================================================================================
// ENHANCED COMPONENT: Premium Date Range Picker Dialog
// ============================================================================================
@SuppressLint("NonObservableLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumDateRangePickerDialog(
    onDismiss: () -> Unit,
    onDateRangeSelected: (Long, Long) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        colors = DatePickerDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        confirmButton = {
            Button(
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis ?: start
                    if (start != null && end != null) {
                        onDateRangeSelected(start, end)
                    }
                },
                enabled = dateRangePickerState.selectedStartDateMillis != null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
            ) {
                Text("Select Range", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("Cancel", color = Color.Gray, fontWeight = FontWeight.SemiBold)
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = {
                Text(
                    text = "Custom Date Filter",
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            headline = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Start Date
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "From",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = if (dateRangePickerState.selectedStartDateMillis != null) {
                                SimpleDateFormat("dd MMM, yy", LocalLocale.current.platformLocale).format(Date(dateRangePickerState.selectedStartDateMillis!!))
                            } else "--",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (dateRangePickerState.selectedStartDateMillis != null) MaterialTheme.colorScheme.onSurface else Color.LightGray
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "to",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // End Date
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = "To",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = if (dateRangePickerState.selectedEndDateMillis != null) {
                                SimpleDateFormat("dd MMM, yy", LocalLocale.current.platformLocale).format(Date(dateRangePickerState.selectedEndDateMillis!!))
                            } else "--",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (dateRangePickerState.selectedEndDateMillis != null) MaterialTheme.colorScheme.onSurface else Color.LightGray
                        )
                    }
                }
            },
            showModeToggle = false,
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.primary,
                headlineContentColor = MaterialTheme.colorScheme.onSurface,
                weekdayContentColor = Color.Gray,
                subheadContentColor = MaterialTheme.colorScheme.primary,
                yearContentColor = MaterialTheme.colorScheme.onSurface,
                currentYearContentColor = MaterialTheme.colorScheme.primary,
                selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                dayContentColor = MaterialTheme.colorScheme.onSurface,
                selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                todayContentColor = MaterialTheme.colorScheme.primary,
                todayDateBorderColor = MaterialTheme.colorScheme.primary,
                dayInSelectionRangeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                dayInSelectionRangeContainerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

// ============================================================================================
// COMPONENT: Smart Insight Card
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
                // Ensure TransactionItem is imported from components/DashboardScreen
                com.kdev.spendwise.ui.components.TransactionItem(expense, viewModel, wallets)
            }
        }
    )
}

@Composable
fun SearchBarView(query: String, onQueryChange: (String) -> Unit) { OutlinedTextField(value = query, onValueChange = onQueryChange, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), placeholder = { Text("Search transactions...") }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) }, shape = RoundedCornerShape(16.dp), singleLine = true) }

@Composable
fun DateHeader(text: String) { Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), Alignment.Center) { Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant.copy(0.8f)) { Text(text, Modifier.padding(horizontal = 16.dp, vertical = 6.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) } } }

@Composable
fun EmptyStateView() { Box(Modifier.fillMaxWidth().height(300.dp), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.ReceiptLong, null, Modifier.size(64.dp), tint = Color.Gray.copy(0.5f)); Spacer(Modifier.height(16.dp)); Text("No transactions found for this period", color = Color.Gray) } } }

fun formatGroupDate(timestamp: Long): String { val d = Date(timestamp); val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()); return sdf.format(d) }