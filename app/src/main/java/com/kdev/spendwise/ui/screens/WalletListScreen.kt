package com.kdev.spendwise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdev.spendwise.data.Wallet
import com.kdev.spendwise.ui.MainViewModel
import com.kdev.spendwise.data.cardThemes
import com.kdev.spendwise.util.CurrencyUtils
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletListScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onEditWallet: () -> Unit,
    onAddWallet: () -> Unit
) {
    val wallets = viewModel.wallets

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(bottom = 8.dp)
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = "My Wallets",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )

                // Premium Subtitle Style
                Text(
                    text = "Manage your accounts & cards",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.walletToEdit = null // Clear state for new wallet
                    onAddWallet()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Wallet")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp), // Matched padding with other screens
            verticalArrangement = Arrangement.spacedBy(20.dp), // Premium spacing
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp) // Extra bottom padding for FAB
        ) {
            items(wallets) { wallet ->
                val theme = cardThemes.getOrElse(wallet.cardThemeId) { cardThemes[0] }
                val brush = Brush.linearGradient(colors = listOf(theme.start, theme.end))
                val displayNum = if (wallet.cardNumber.isNotBlank()) wallet.cardNumber.takeLast(4) else "••••"

                // --- PREMIUM MANAGEMENT CARD ---
                Card(
                    onClick = {
                        viewModel.walletToEdit = wallet
                        onEditWallet()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp), // Standardized height
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                                    text = wallet.name.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 1.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Bank Name (Big Middle)
                                Text(
                                    text = wallet.bankName.ifBlank { "Bank Name" },
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    modifier = Modifier.wrapContentWidth(Alignment.Start)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Number (Bottom)
                                Text(
                                    text = "••••  ••••  ••••  $displayNum",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }

                            // Right: Edit Icon & Balance
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                // Edit Indicator (Subtle Pencil)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                        .clip(CircleShape)
                                        .clickable {
                                            viewModel.walletToEdit = wallet
                                            onEditWallet()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Edit",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                // Balance
                                Text(
                                    text = CurrencyUtils.formatINR(wallet.balance),
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
    }
}

@Composable
fun CompactWalletItem(wallet: Wallet, onClick: () -> Unit) {
    val theme = cardThemes.getOrElse(wallet.cardThemeId) { cardThemes[0] }
    val brush = Brush.linearGradient(colors = listOf(theme.start, theme.end))

    // Logic to safely get display number
    val displayNum = remember(wallet.cardNumber, wallet.id) {
        if (wallet.cardNumber.isNotBlank()) {
            wallet.cardNumber.takeLast(4)
        } else {
            // Fallback if empty
            val hash = wallet.id.hashCode().absoluteValue.toString()
            hash.takeLast(4).padStart(4, '0')
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
        ) {
            // Background Pattern
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.05f)))

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Info
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = wallet.bankName.ifBlank { "Wallet" },
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = wallet.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "**** $displayNum",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                // Right: Balance & Edit Icon
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.SpaceBetween) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = CurrencyUtils.formatINR(wallet.balance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}