package com.kdev.spendwise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdev.spendwise.ui.MainViewModel
import com.kdev.spendwise.ui.components.AnalysisOptionTab
import com.kdev.spendwise.ui.components.AnalyticalPieChart
import com.kdev.spendwise.ui.components.VerticalDivider
import com.kdev.spendwise.ui.components.getProgressColor
import com.kdev.spendwise.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedAnalysisScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToAccountSelect: () -> Unit
) {
    val context = LocalContext.current

    // 1. Data Sources
    val wallets = viewModel.wallets
    val filteredExpenses by viewModel.filteredExpenses.collectAsState()

    // 2. UI States
    var analysisType by remember { mutableStateOf("EXPENSE") } // "EXPENSE" or "INCOME"

    // 3. Month Navigation Logic
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }

    val isCurrentMonth = remember(selectedDate) {
        val today = Calendar.getInstance()
        selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                selectedDate.get(Calendar.MONTH) == today.get(Calendar.MONTH)
    }

    // 4. ROBUST FILTER LOGIC
    val displayedTransactions = remember(filteredExpenses, selectedDate, analysisType) {
        // A. Filter by Month (Ignoring Time - comparing Year & Month indices)
        val targetYear = selectedDate.get(Calendar.YEAR)
        val targetMonth = selectedDate.get(Calendar.MONTH)

        val monthData = filteredExpenses.filter { tx ->
            val txCal = Calendar.getInstance()
            txCal.timeInMillis = tx.date
            txCal.get(Calendar.YEAR) == targetYear && txCal.get(Calendar.MONTH) == targetMonth
        }

        // B. Filter by Type (Inclusive check)
        if (analysisType == "EXPENSE") {
            monthData.filter { it.amount < 0 || it.type == "EXPENSE" }
        } else {
            monthData.filter { it.amount > 0 || it.type == "INCOME" }
        }
    }

    // 5. Calculations
    val totalAmount = displayedTransactions.sumOf { abs(it.amount) }

    // 6. Grouping for Charts
    val categoryStats = remember(displayedTransactions) {
        displayedTransactions
            .groupBy { it.category.substringBefore(" -> ") }
            .mapValues { entry -> entry.value.sumOf { abs(it.amount) } }
            .entries.sortedByDescending { it.value }
    }

    val chartData = remember(categoryStats) {
        categoryStats.associate { it.key to it.value }
    }

    // Current Wallet Name for Display
    val currentWalletName = if (viewModel.selectedWalletId == null) {
        "All Accounts"
    } else {
        val w = wallets.find { it.id == viewModel.selectedWalletId }
        w?.bankName?.ifBlank { w.name } ?: "Unknown"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Analysis", fontWeight = FontWeight.Bold)
                        Text(
                            "Track your income & expenses",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {

            // --- 1. MONTH NAVIGATOR (Moved Here) ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            val newCal = selectedDate.clone() as Calendar
                            newCal.add(Calendar.MONTH, -1)
                            selectedDate = newCal
                        },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.ChevronLeft, null)
                    }

                    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    Text(
                        text = monthFormat.format(selectedDate.time),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    IconButton(
                        onClick = {
                            val newCal = selectedDate.clone() as Calendar
                            newCal.add(Calendar.MONTH, 1)
                            selectedDate = newCal
                        },
                        enabled = !isCurrentMonth,
                        modifier = Modifier.background(
                            if(isCurrentMonth) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f),
                            CircleShape
                        )
                    ) {
                        Icon(
                            Icons.Default.ChevronRight, null,
                            tint = if (isCurrentMonth) Color.LightGray else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // --- 2. SELECTION BAR (Account | Expense | Income) ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Option 1: Account
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onNavigateToAccountSelect() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                currentWalletName.take(10).let { if(currentWalletName.length>10) "$it.." else it },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }

                    VerticalDivider()

                    // Option 2: Expense
                    AnalysisOptionTab(
                        text = "Expense",
                        isSelected = analysisType == "EXPENSE",
                        onClick = { analysisType = "EXPENSE" },
                        modifier = Modifier.weight(1f)
                    )

                    VerticalDivider()

                    // Option 3: Income
                    AnalysisOptionTab(
                        text = "Income",
                        isSelected = analysisType == "INCOME",
                        onClick = { analysisType = "INCOME" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- 3. TOTAL & CHART CARD ---
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Total ${if(analysisType=="EXPENSE") "Outflow" else "Inflow"}", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                        Text(
                            CurrencyUtils.formatINR(totalAmount),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = if(analysisType=="EXPENSE") Color(0xFFD32F2F) else Color(0xFF388E3C)
                        )

                        Spacer(Modifier.height(32.dp))

                        if(displayedTransactions.isNotEmpty()) {
                            Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                                AnalyticalPieChart(data = chartData)
                            }
                        } else {
                            Box(modifier = Modifier.height(200.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.DonutSmall, null, tint = Color.Gray.copy(0.5f), modifier = Modifier.size(48.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("No data for this month", color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Category-wise Split", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            // --- 4. CATEGORY LIST ---
            items(categoryStats) { (category, amount) ->
                val percentage = if (totalAmount > 0) (amount / totalAmount).toFloat() else 0f

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = viewModel.getIconForCategory(category),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text(category, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                            Text(CurrencyUtils.formatINR(amount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { percentage },
                                modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = getProgressColor(percentage, analysisType == "EXPENSE"),
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("${(percentage * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
                        }
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.2f))
            }
        }
    }
}