package com.kdev.spendwise.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdev.spendwise.data.cardThemes
import com.kdev.spendwise.ui.MainViewModel
import com.kdev.spendwise.ui.components.PremiumWalletSelectionCard
import com.kdev.spendwise.ui.theme.BlueGreyMain
import com.kdev.spendwise.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisWalletSelectionScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val wallets = viewModel.wallets
    val selectedId = viewModel.selectedWalletId

    // Theme Color
    val selectionColor = BlueGreyMain

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(bottom = 8.dp)
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Select Account",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )

                // [NEW] Subtitle under the header
                Text(
                    text = "Choose data source for reports",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp) // Aligned with title padding
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp), // Increased spacing for premium look
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {

            // --- Option 1: ALL ACCOUNTS ---
            item {
                val isAllSelected = selectedId == null

                // Dark Premium Gradient for "All Accounts"
                val allAccountsBrush = Brush.linearGradient(
                    colors = listOf(Color(0xFF232526), Color(0xFF414345))
                )

                PremiumSelectionCardItem(
                    bankName = "All Accounts",
                    cardLabel = "Consolidated View",
                    displayNum = "TOTAL",
                    brush = allAccountsBrush,
                    isSelected = isAllSelected,
                    selectionColor = selectionColor,
                    balance = null, // Optional: You could sum all wallet balances here if desired
                    onClick = {
                        viewModel.selectWallet(null)
                        onBack()
                    }
                )
            }

            // --- Option 2...N: Individual Wallets ---
            items(wallets) { wallet ->
                val theme = cardThemes.getOrElse(wallet.cardThemeId) { cardThemes[0] }
                val brush = Brush.linearGradient(colors = listOf(theme.start, theme.end))

                // Display Logic
                val displayNum = if (wallet.cardNumber.isNotBlank()) wallet.cardNumber.takeLast(4) else "••••"

                PremiumSelectionCardItem(
                    bankName = wallet.bankName.ifBlank { "Wallet" },
                    cardLabel = wallet.name,
                    displayNum = displayNum,
                    brush = brush,
                    isSelected = selectedId == wallet.id,
                    selectionColor = selectionColor,
                    balance = wallet.balance,
                    onClick = {
                        viewModel.selectWallet(wallet.id)
                        onBack()
                    }
                )
            }
        }
    }
}

// --- HELPER COMPOSABLE TO REUSE THE DESIGN ---
@Composable
fun PremiumSelectionCardItem(
    bankName: String,
    cardLabel: String,
    displayNum: String,
    brush: Brush,
    isSelected: Boolean,
    selectionColor: Color,
    balance: Double?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp), // Fixed taller height
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 12.dp else 2.dp
        ),
        border = if (isSelected) BorderStroke(2.dp, selectionColor) else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
        ) {
            // Texture Overlay
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.1f))
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Label / Nickname (Small Top)
                    Text(
                        text = cardLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Bank Name (Big Middle)
                    Text(
                        text = bankName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.wrapContentWidth(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Number (Bottom)
                    Text(
                        text = if (displayNum == "TOTAL") "VIEW ALL" else "••••  ••••  ••••  $displayNum",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Right: Selection & Balance
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    // Checkbox Circle
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(selectionColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Balance (Only show if not null, e.g. for All Accounts you might hide it)
                    if (balance != null) {
                        Text(
                            text = CurrencyUtils.formatINR(balance),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}