package com.kdev.spendwise.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdev.spendwise.data.RecurringRule
import com.kdev.spendwise.data.Wallet
import com.kdev.spendwise.data.cardThemes
import com.kdev.spendwise.ui.MainViewModel
import com.kdev.spendwise.ui.components.PremiumAlertDialog
import com.kdev.spendwise.ui.components.TransactionItem
import com.kdev.spendwise.util.CurrencyUtils
import com.kdev.spendwise.util.DateUtils
import java.util.*
import kotlin.math.absoluteValue

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
    val context = LocalContext.current
    val activity = context as? Activity

    // --- STATE OBSERVATION ---
    val wallets = viewModel.wallets
    val categorySpending by viewModel.currentMonthSpending.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val recentTransactions by viewModel.filteredExpenses.collectAsState()
    val upcomingRules = viewModel.upcomingRules
    val isLoading = viewModel.isCheckingAuthState

    // --- UI STATE ---
    var showExitDialog by remember { mutableStateOf(false) }
    var showNoWalletAlert by remember { mutableStateOf(false) }

    // --- BACK HANDLER ---
    BackHandler { showExitDialog = true }

    // --- SIDE EFFECTS ---
    LaunchedEffect(Unit) {
        viewModel.refreshRecurring()
    }

    // --- TIME GREETING ---
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good Morning,"
            in 12..16 -> "Good Afternoon,"
            else -> "Good Evening,"
        }
    }

    // --- PAGER LOGIC ---
    val initialPage = remember(wallets, viewModel.selectedWalletId) {
        if (viewModel.selectedWalletId != null) {
            val index = wallets.indexOfFirst { it.id == viewModel.selectedWalletId }
            if (index != -1) index else 0
        } else 0
    }
    val pageCount = if (wallets.isEmpty()) 1 else wallets.size + 1
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { pageCount })

    // Sync Pager Swipe with ViewModel
    LaunchedEffect(pagerState.currentPage, wallets) {
        if (wallets.isNotEmpty() && pagerState.currentPage < wallets.size) {
            viewModel.selectWallet(wallets[pagerState.currentPage].id)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (!isLoading) {
                FloatingActionButton(
                    onClick = {
                        if (wallets.isNotEmpty()) onAddTransactionClick()
                        else showNoWalletAlert = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                LoadingView()
            } else {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(600)),
                    exit = fadeOut()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        // 1. HEADER
                        item {
                            HeaderSection(
                                greeting = greeting,
                                userName = viewModel.userName,
                                avatarId = viewModel.getCurrentAvatar(),
                                onProfileClick = onProfileClick
                            )
                        }

                        // 2. WALLET CAROUSEL
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(230.dp)) {
                                HorizontalPager(
                                    state = pagerState,
                                    contentPadding = PaddingValues(horizontal = 24.dp),
                                    pageSpacing = 16.dp
                                ) { page ->
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

                        // 3. ANIMATED QUICK ACTIONS (Restored & Beats on Touch)
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 24.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                AnimatedQuickActionItem(Icons.Default.Analytics, "Analysis", 0, onShowAnalysisClick)
                                AnimatedQuickActionItem(Icons.Default.Loop, "Recurring", 100, onRecurringClick)
                                AnimatedQuickActionItem(Icons.Default.AccountBalanceWallet, "Wallets", 200, onShowWalletsClick)
                                AnimatedQuickActionItem(Icons.Default.History, "History", 300, onShowMoreClick)
                            }
                        }

                        // 4. RECENT ACTIVITY
                        item {
                            if (recentTransactions.isNotEmpty()) {
                                Column(Modifier.padding(horizontal = 24.dp)) {
                                    Text(
                                        "Recent Activity",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    recentTransactions.take(4).forEach { tx ->
                                        TransactionItem(
                                            expense = tx,
                                            viewModel = viewModel,
                                            wallets = wallets
                                        )
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }
                                Spacer(Modifier.height(24.dp))
                            }
                        }

                        // 5. MONTHLY SPENDING (PIE CHART)
                        item {
                            Column(Modifier.padding(horizontal = 24.dp)) {
                                Text(
                                    "Monthly Spending",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(16.dp))

                                if (categorySpending.isNotEmpty()) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(24.dp),
                                        elevation = CardDefaults.cardElevation(2.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier.size(200.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                AnimatedPieChart(data = categorySpending)

                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Total", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                    Text(
                                                        text = CurrencyUtils.formatINR(categorySpending.values.sum()),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(24.dp))
                                            LegendList(categorySpending)
                                        }
                                    }
                                } else {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) { Text("No spending data this month", color = Color.Gray) }
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                        }

                        // 6. UPCOMING BILLS
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Upcoming", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                TextButton(onClick = onRecurringClick) { Text("Manage") }
                            }

                            if (upcomingRules.isNotEmpty()) {
                                upcomingRules.take(3).forEach { rule ->
                                    val wallet = wallets.find { it.id == rule.walletId }
                                    UpcomingBillItem(rule = rule, wallet = wallet)
                                }
                            } else {
                                EmptyUpcomingCard(onRecurringClick)
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS ---
    if (showExitDialog) {
        PremiumAlertDialog(
            title = "Exit SpendWise?",
            message = "Are you sure you want to close the application?",
            confirmText = "Exit",
            dismissText = "Stay",
            icon = Icons.Default.ExitToApp,
            confirmButtonColor = MaterialTheme.colorScheme.primary,
            iconColor = MaterialTheme.colorScheme.primary,
            onConfirm = { activity?.finish() },
            onDismiss = { showExitDialog = false }
        )
    }

    if (showNoWalletAlert) {
        PremiumAlertDialog(
            title = "No Wallet Found",
            message = "You need to add a wallet before you can add a transaction.",
            confirmText = "Add Wallet",
            dismissText = "Cancel",
            icon = Icons.Default.AccountBalanceWallet,
            onConfirm = {
                showNoWalletAlert = false
                viewModel.walletToEdit = null
                onEditCardClick()
            },
            onDismiss = { showNoWalletAlert = false }
        )
    }
}

// ============================================================================================
// HELPER COMPONENTS
// ============================================================================================

@Composable
fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp,
                modifier = Modifier.size(50.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("Syncing Finance Data...", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun HeaderSection(greeting: String, userName: String, avatarId: Int, onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
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
                text = userName.ifEmpty { "User" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .size(50.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onProfileClick)
        ) {
            Image(
                painter = painterResource(id = avatarId),
                contentDescription = "Profile",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// --- UPDATED: BEATING HEART ANIMATION ON CLICK ---
@Composable
fun AnimatedQuickActionItem(
    icon: ImageVector,
    label: String,
    delay: Int,
    onClick: () -> Unit
) {
    // 1. Entrance Fade-In
    val visible = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        visible.value = true
    }

    // 2. Press Interaction (The "Beat" Effect)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Scale Logic: Shrinks to 0.85 when pressed, bounces back to 1.0 when released
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        label = "beatScale",
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy, // High bounce for "beat" feel
            stiffness = Spring.StiffnessMedium
        )
    )

    AnimatedVisibility(
        visible = visible.value,
        enter = scaleIn(tween(400)) + fadeIn(tween(400))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale) // Apply the beat scale
                .clickable(
                    interactionSource = interactionSource,
                    indication = null // Disable default ripple for cleaner beat effect
                ) { onClick() }
        ) {
            // Icon Box
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(0.8f)
            )
        }
    }
}

// --- ANIMATED PIE CHART ---
@Composable
fun AnimatedPieChart(data: Map<String, Double>) {
    val total = data.values.sum()
    val proportions = data.values.map { (it / total).toFloat() }
    val colors = listOf(Color(0xFF5C6BC0), Color(0xFF42A5F5), Color(0xFF26A69A), Color(0xFFFFA726), Color(0xFFEF5350))

    var animationPlayed by remember { mutableStateOf(false) }
    val animateProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "pieProgress"
    )

    LaunchedEffect(Unit) { animationPlayed = true }

    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        var startAngle = -90f
        proportions.forEachIndexed { index, proportion ->
            val sweepAngle = proportion * 360f * animateProgress
            val color = colors.getOrElse(index) { Color.LightGray }
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 35f, cap = StrokeCap.Round)
            )
            startAngle += proportion * 360f
        }
    }
}

@Composable
fun LegendList(data: Map<String, Double>) {
    val sorted = data.entries.sortedByDescending { it.value }.take(5)
    val colors = listOf(Color(0xFF5C6BC0), Color(0xFF42A5F5), Color(0xFF26A69A), Color(0xFFFFA726), Color(0xFFEF5350))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        sorted.forEachIndexed { index, entry ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(colors.getOrElse(index) { Color.Gray }, CircleShape))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = entry.key, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
                Text(
                    text = CurrencyUtils.formatINR(entry.value),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun WalletCard(wallet: Wallet, income: Double, expense: Double) {
    val theme = cardThemes.getOrElse(wallet.cardThemeId) { cardThemes[0] }

    val maskedNumber = remember(wallet.cardNumber, wallet.id) {
        if (wallet.cardNumber.isNotBlank()) wallet.cardNumber.takeLast(4)
        else wallet.id.hashCode().absoluteValue.toString().takeLast(4).padStart(4, '0')
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .shadow(12.dp, RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(theme.start, theme.end)))
    ) {
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.05f)))
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                radius = 180f,
                center = Offset(x = size.width, y = 0f)
            )
        }

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
                text = "**** **** **** $maskedNumber",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )

            Column {
                Text("Available Balance", color = Color.White.copy(0.6f), fontSize = 11.sp)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = CurrencyUtils.formatINR(wallet.balance),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowDownward, null, tint = Color(0xFF69F0AE), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(CurrencyUtils.formatCompact(income), color = Color.White.copy(0.9f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowUpward, null, tint = Color(0xFFFF8A80), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(CurrencyUtils.formatCompact(expense), color = Color.White.copy(0.9f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
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
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))
            .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(26.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(60.dp).background(MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Add New Wallet", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun UpcomingBillItem(rule: RecurringRule, wallet: Wallet?) {
    val isExpense = rule.type == "EXPENSE"
    val color = if (isExpense) Color(0xFFD32F2F) else Color(0xFF388E3C)
    val bgColor = color.copy(alpha = 0.1f)
    val sign = if (isExpense) "- " else "+ "
    val walletName = wallet?.bankName?.ifBlank { wallet.name } ?: "Unknown Wallet"

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.2f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isExpense) Icons.Default.ArrowOutward else Icons.Default.ArrowDownward,
                    contentDescription = null, tint = color, modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(rule.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalanceWallet, null, Modifier.size(12.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(walletName, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Text("Due: ${DateUtils.getRelativeDate(rule.nextRunDate)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Text("$sign${CurrencyUtils.formatINR(rule.amount)}", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.bodyLarge, color = color)
        }
    }
}

@Composable
fun EmptyUpcomingCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.Gray.copy(0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.NotificationsNone, null, tint = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("No upcoming bills", fontWeight = FontWeight.Bold)
                Text("Tap to set up Standing Instructions", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}