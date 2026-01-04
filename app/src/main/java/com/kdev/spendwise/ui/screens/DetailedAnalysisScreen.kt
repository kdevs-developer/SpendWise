package com.kdev.spendwise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdev.spendwise.ui.MainViewModel
import com.kdev.spendwise.ui.components.AnalyticalPieChart
import com.kdev.spendwise.util.CurrencyUtils
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedAnalysisScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    // 1. UPDATED: Use filteredExpenses to match Dashboard selection
    val transactions by viewModel.filteredExpenses.collectAsState(initial = emptyList())

    // Get current wallet name for display
    val currentWalletName = remember(viewModel.selectedWalletId, viewModel.wallets) {
        viewModel.wallets.find { it.id == viewModel.selectedWalletId }?.name ?: "All Wallets"
    }

    // 2. Filter for Expenses only
    val expenseTransactions = remember(transactions) {
        transactions.filter { it.amount < 0 }
    }

    val totalSpent = remember(expenseTransactions) {
        expenseTransactions.sumOf { abs(it.amount) }
    }

    // 3. UPDATED: Group by "Main Category" only (Split "Food -> Groceries" to just "Food")
    val categoryStats = remember(expenseTransactions) {
        expenseTransactions
            .groupBy { it.category.substringBefore(" -> ") }
            .mapValues { entry -> entry.value.sumOf { abs(it.amount) } }
            .entries.sortedByDescending { it.value }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Spending Breakdown", fontWeight = FontWeight.Bold)
                        Text(currentWalletName, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp), // Consistent padding
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {

            // --- 1. PIE CHART CARD ---
            item {
                Spacer(Modifier.height(8.dp))
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
                        Text("Total Outflow", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                        Text(
                            CurrencyUtils.formatINR(totalSpent),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(Modifier.height(32.dp))

                        if(expenseTransactions.isNotEmpty()) {
                            // Pass processed category stats to Pie Chart if possible,
                            // otherwise pass raw expenses and let chart handle it.
                            Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                                AnalyticalPieChart(expenses = expenseTransactions)
                            }
                        } else {
                            Box(modifier = Modifier.height(200.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.CalendarMonth, null, tint = Color.Gray.copy(0.5f), modifier = Modifier.size(48.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("No expenses found", color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Category-wise Split",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // --- 2. CATEGORY LIST ---
            items(categoryStats) { (category, amount) ->
                val percentage = if (totalSpent > 0) (amount / totalSpent).toFloat() else 0f

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category Icon
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

                    // Progress & Text
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
                                color = getProgressColor(percentage),
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

fun getProgressColor(percentage: Float): Color {
    return when {
        percentage > 0.5f -> Color(0xFFE53935) // Red for high impact
        percentage > 0.20f -> Color(0xFFFB8C00) // Orange for medium
        else -> Color(0xFF43A047) // Green for low
    }
}