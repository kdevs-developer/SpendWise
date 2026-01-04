package com.kdev.spendwise.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdev.spendwise.data.RecurringRule
import com.kdev.spendwise.data.Wallet
import com.kdev.spendwise.ui.MainViewModel
import com.kdev.spendwise.ui.cardThemes
import com.kdev.spendwise.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onShowMoreClick: () -> Unit,
    onShowWalletsClick: () -> Unit,
    onEditCardClick: () -> Unit,
    onShowAnalysisClick: () -> Unit,
    onAddTransactionClick: () -> Unit,
    onProfileClick: () -> Unit,
    onRecurringClick: () -> Unit
) {
    val wallets = viewModel.wallets
    val categorySpending by viewModel.categorySpending.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val recurringRules = viewModel.recurringRules

    // --- DYNAMIC GREETING LOGIC ---
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good Morning,"
            in 12..16 -> "Good Afternoon,"
            else -> "Good Evening,"
        }
    }

    // Calculate initial page based on selected wallet
    val initialPage = remember(wallets, viewModel.selectedWalletId) {
        if (viewModel.selectedWalletId != null) {
            val index = wallets.indexOfFirst { it.id == viewModel.selectedWalletId }
            if (index != -1) index else 0
        } else {
            0
        }
    }

    // UNIFIED PAGE COUNT: If empty, we still show 1 page (The Add Button)
    val pageCount = if (wallets.isEmpty()) 1 else wallets.size + 1
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { pageCount })

    // Sync Pager with ViewModel Selection
    LaunchedEffect(pagerState.currentPage, wallets) {
        if (wallets.isNotEmpty() && pagerState.currentPage < wallets.size) {
            viewModel.selectedWalletId = wallets[pagerState.currentPage].id
        } else {
            viewModel.selectedWalletId = null
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {

            // --- 1. HEADER ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = viewModel.userName.ifEmpty { "User" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onProfileClick) {
                        Image(
                            painter = painterResource(id = viewModel.getCurrentAvatar()),
                            contentDescription = "Profile",
                            modifier = Modifier.size(45.dp).clip(CircleShape)
                        )
                    }
                }
            }

            // --- 2. WALLET CAROUSEL (UNIFIED LOGIC) ---
            item {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        pageSpacing = 16.dp
                    ) { page ->
                        // Logic: If we have wallets and the current page is within range, show Wallet.
                        // Otherwise (empty list OR last page), show Add New Wallet.
                        if (wallets.isNotEmpty() && page < wallets.size) {
                            WalletCard(
                                wallet = wallets[page],
                                income = totalIncome,
                                expense = totalExpense
                            )
                        } else {
                            AddNewWalletCard(onClick = {
                                viewModel.walletToEdit = null
                                onEditCardClick()
                            })
                        }
                    }
                }
            }

            // --- 3. QUICK ACTIONS ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionItem(Icons.Default.Analytics, "Analysis", onShowAnalysisClick)
                    QuickActionItem(Icons.Default.Loop, "Recurring", onRecurringClick)
                    QuickActionItem(Icons.Default.AccountBalanceWallet, "Wallets", onShowWalletsClick)
                    QuickActionItem(Icons.Default.History, "History", onShowMoreClick)
                }
            }

            // --- 4. SPENDING BREAKDOWN (Pie Chart) ---
            item {
                Column(Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        "Spending Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))

                    if (categorySpending.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(4.dp),
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Chart
                                Box(Modifier.weight(0.45f).aspectRatio(1f)) {
                                    MinimalPieChart(data = categorySpending)
                                }

                                Spacer(Modifier.width(16.dp))

                                // Legend
                                Column(
                                    Modifier.weight(0.55f).fillMaxHeight(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val sorted = categorySpending.entries.sortedByDescending { it.value }.take(4)
                                    val colors = listOf(
                                        Color(0xFF5C6BC0), Color(0xFF42A5F5),
                                        Color(0xFF26A69A), Color(0xFFFFA726)
                                    )

                                    sorted.forEachIndexed { index, (cat, amt) ->
                                        val color = colors.getOrElse(index) { Color.Gray }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.size(10.dp).background(color, CircleShape))
                                            Spacer(Modifier.width(8.dp))
                                            Column {
                                                Text(cat, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                                                Text(CurrencyUtils.formatINR(amt), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Empty State for Chart
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No spending data yet", color = Color.Gray)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // --- 5. UPCOMING PAYMENTS ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Upcoming Payments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onRecurringClick) {
                        Text("Manage")
                    }
                }

                if (recurringRules.isNotEmpty()) {
                    val sortedRules = recurringRules.sortedBy { it.nextRunDate }.take(3)
                    sortedRules.forEach { rule ->
                        UpcomingBillItem(rule = rule, viewModel = viewModel)
                    }
                } else {
                    // Empty State for Bills
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clickable { onRecurringClick() },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(0.2f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.NotificationsNone, null, tint = Color.White)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("No upcoming bills", fontWeight = FontWeight.Bold)
                                Text("Tap to set up recurring payments", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// --- COMPONENTS ---

@Composable
fun MinimalPieChart(data: Map<String, Double>) {
    val total = data.values.sum()
    val proportions = data.values.map { (it / total).toFloat() }
    val colors = listOf(Color(0xFF5C6BC0), Color(0xFF42A5F5), Color(0xFF26A69A), Color(0xFFFFA726))

    // Animation
    var animationPlayed by remember { mutableStateOf(false) }
    val animateRotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    LaunchedEffect(Unit) { animationPlayed = true }

    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        var startAngle = -90f
        proportions.forEachIndexed { index, proportion ->
            val sweepAngle = proportion * 360f * animateRotation
            val color = colors.getOrElse(index) { Color.LightGray }

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 40f, cap = StrokeCap.Butt)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun UpcomingBillItem(rule: RecurringRule, viewModel: MainViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(0.3f)),
                contentAlignment = Alignment.Center
            ) {
                // Use substringBefore for safety or full category string logic
                val catName = rule.category.split(" -> ").lastOrNull() ?: rule.category
                Icon(
                    imageVector = viewModel.getIconForCategory(catName),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(rule.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                val date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(rule.nextRunDate))
                Text("Due $date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
            Text(
                CurrencyUtils.formatINR(rule.amount),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun WalletCard(wallet: Wallet, income: Double, expense: Double) {
    val theme = cardThemes.getOrElse(wallet.cardThemeId) { cardThemes[0] }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .shadow(10.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(theme.start, theme.end)))
    ) {
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.05f)))

        Column(
            modifier = Modifier.padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(wallet.bankName.ifBlank { "Bank Name" }, color = Color.White.copy(0.9f), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(wallet.name, color = Color.White.copy(0.7f), fontSize = 12.sp)
                }
                Icon(Icons.Default.Nfc, null, tint = Color.White.copy(0.8f))
            }

            Text(
                text = CurrencyUtils.formatINR(wallet.balance),
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Income", color = Color.White.copy(0.7f), fontSize = 10.sp)
                    Text("+${CurrencyUtils.formatINR(income)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Expense", color = Color.White.copy(0.7f), fontSize = 10.sp)
                    Text("-${CurrencyUtils.formatINR(expense)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun AddNewWalletCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            Text("Add New Wallet", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun QuickActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
    }
}