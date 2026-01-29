package com.kdev.spendwise.ui.components

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.utils.DateUtils
import com.kdev.spendwise.data.RecurringRule
import com.kdev.spendwise.data.Wallet
import com.kdev.spendwise.data.cardThemes
import com.kdev.spendwise.ui.MainViewModel
import com.kdev.spendwise.ui.theme.BlueGreyMain
import com.kdev.spendwise.util.CurrencyUtils
import com.kdev.spendwise.util.DateUtils.getRelativeDate
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringRuleItem(
    rule: RecurringRule,
    wallet: Wallet?, // [CHANGE] Added Wallet parameter
    viewModel: MainViewModel,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isIncome = rule.type == "INCOME"
    val color = if (isIncome) Color(0xFF388E3C) else Color(0xFFD32F2F)
    val bgColor = color.copy(alpha = 0.1f)

    // Resolve Bank Name
    val bankName = wallet?.bankName?.ifBlank { wallet.name } ?: "Unknown Wallet"
    val last4 = if (wallet?.cardNumber?.isNotBlank() == true) " •• ${wallet.cardNumber.takeLast(4)}" else ""

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(0.5.dp, Color.LightGray.copy(0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Default.EventAvailable else Icons.Default.EventBusy,
                    contentDescription = null,
                    tint = color
                )
            }

            Spacer(Modifier.width(16.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

                // [NEW] Bank Name Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$bankName$last4",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Frequency & Next Date
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = rule.frequency,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Next: ${getRelativeDate(rule.nextRunDate)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Amount & Delete
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyUtils.formatINR(rule.amount),
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

// --- CUSTOM FREQUENCY DIALOG ---
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
                Text("Repeats every $count $displayUnit", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(32.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    FilledIconButton(onClick = { if (count > 1) count-- }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFF5F5F5))) { Icon(Icons.Default.Remove, null, tint = Color.Black) }
                    Text(count.toString(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(horizontal = 24.dp))
                    FilledIconButton(onClick = { if (count < 99) count++ }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)) { Icon(Icons.Default.Add, null, tint = Color.White) }
                }
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(Color(0xFFF8F9FA), RoundedCornerShape(16.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    IconButton(onClick = { unitIndex = (unitIndex - 1 + units.size) % units.size }) { Icon(Icons.Default.ChevronLeft, null, tint = Color.Gray) }
                    Text(units[unitIndex], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    IconButton(onClick = { unitIndex = (unitIndex + 1) % units.size }) { Icon(Icons.Default.ChevronRight, null, tint = Color.Gray) }
                }
                Spacer(Modifier.height(32.dp))
                Row(Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color(0xFFE0E0E0)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)) { Text("Cancel") }
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = { val finalUnit = units[unitIndex]; val formatted = if (count == 1) "Every ${finalUnit.dropLast(1)}" else "Every $count $finalUnit"; onConfirm(formatted) }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Set") }
                }
            }
        }
    }
}

// --- UPDATED PREMIUM WALLET PICKER ---
@Composable
fun RecurringWalletPickerDialog(
    wallets: List<Wallet>,
    currentWalletId: String?,
    onDismiss: () -> Unit,
    onWalletSelected: (String) -> Unit
) {
    // [FIX] 1. Use your specific theme color
    val selectionColor = BlueGreyMain

    // [FIX] 2. Local State for Instant UI Updates
    // We initialize it with 'currentWalletId', but update it immediately on click
    var selectedId by remember(currentWalletId) { mutableStateOf(currentWalletId) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // --- HEADER ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Select Source",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Which wallet should handle this?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // --- LIST ---
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    wallets.forEach { wallet ->
                        // [FIX] Compare against our LOCAL state variable for instant feedback
                        val isSelected = wallet.id == selectedId

                        // Theme Logic
                        val theme = cardThemes.getOrElse(wallet.cardThemeId) { cardThemes[0] }
                        val brush = Brush.linearGradient(colors = listOf(theme.start, theme.end))

                        val displayNum = if (wallet.cardNumber.isNotBlank()) {
                            wallet.cardNumber.takeLast(4)
                        } else {
                            val hash = wallet.id.hashCode().absoluteValue.toString()
                            hash.takeLast(4).padStart(4, '0')
                        }

                        Card(
                            onClick = {
                                // [FIX] Update local state immediately so it becomes Opaque (1f) instantly
                                selectedId = wallet.id
                                onWalletSelected(wallet.id)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .alpha(if (isSelected) 1f else 0.5f), // Now uses the updated local state
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isSelected) 12.dp else 0.dp
                            ),
                            border = if (isSelected) BorderStroke(2.dp, selectionColor) else null
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(brush)
                            ) {
                                // Texture
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
                                        Text(
                                            text = wallet.name.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 1.sp,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = wallet.bankName.ifBlank { "Bank Name" },
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            maxLines = 1,
                                            modifier = Modifier.wrapContentWidth(Alignment.Start)
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = "••••  ••••  ••••  $displayNum",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }

                                    // Right: Selection Indicator
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxHeight()
                                    ) {
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
                                                    tint = Color.White, // White check on BlueGrey looks clean
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
                    Spacer(Modifier.height(48.dp))
                }
            }
        }
    }
}