package com.kdev.spendwise.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdev.spendwise.data.Wallet
import com.kdev.spendwise.ui.MainViewModel
import com.kdev.spendwise.data.cardThemes
import com.kdev.spendwise.ui.components.PremiumAlertDialog
import com.kdev.spendwise.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCardScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val editingWallet = viewModel.walletToEdit

    // Form States
    var bankName by remember { mutableStateOf(editingWallet?.bankName ?: "") }

    // Default to "Savings" if new, or use existing name
    var walletType by remember { mutableStateOf(editingWallet?.name ?: "Savings") }

    // --- FIX: Using 'cardNumber' to match your Wallet Data Class ---
    var cardNumber by remember { mutableStateOf(editingWallet?.cardNumber ?: "") }

    var balance by remember { mutableStateOf(editingWallet?.balance?.toString() ?: "") }
    var selectedThemeIndex by remember { mutableIntStateOf(editingWallet?.cardThemeId ?: 0) }

    // Dialog State
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (editingWallet == null) "Add Wallet" else "Edit Wallet",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (editingWallet == null) "Set up a new bank account" else "Update account or bank info",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    if (editingWallet != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {

            // --- 1. LIVE CARD PREVIEW ---
            Text("Preview", style = MaterialTheme.typography.labelLarge, color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp))

            val theme = cardThemes.getOrElse(selectedThemeIndex) { cardThemes[0] }
            val brush = remember(selectedThemeIndex) {
                Brush.linearGradient(colors = listOf(theme.start, theme.end))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .shadow(10.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(brush)
            ) {
                Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.05f)))

                Column(
                    modifier = Modifier.padding(24.dp).fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = bankName.ifEmpty { "Bank Name" },
                                color = Color.White.copy(0.9f),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            // Display selected Wallet Type as card label
                            Text(walletType, color = Color.White.copy(0.7f), fontSize = 12.sp)
                        }
                        Icon(Icons.Default.Nfc, contentDescription = null, tint = Color.White.copy(0.8f), modifier = Modifier.size(32.dp))
                    }

                    Column {
                        Text("Current Balance", color = Color.White.copy(0.7f), fontSize = 12.sp)
                        Text(
                            text = CurrencyUtils.formatINR(balance.toDoubleOrNull() ?: 0.0),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CARD HOLDER",
                            color = Color.White.copy(0.9f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        // --- PREVIEW: Shows what you type instantly ---
                        val displayNum = if (cardNumber.isNotEmpty()) cardNumber.takeLast(4).padEnd(4, ' ') else "1234"
                        Text(
                            text = "**** **** **** $displayNum",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // --- 2. THEME SELECTOR ---
            Text("Card Design", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                cardThemes.forEachIndexed { index, t ->
                    val isSelected = selectedThemeIndex == index
                    Box(
                        modifier = Modifier
                            .padding(end = 13.dp)
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(t.start, t.end)))
                            .clickable { selectedThemeIndex = index }
                            .then(if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.align(Alignment.Center))
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // --- 3. INPUT FIELDS ---

            // Wallet Type Dropdown
            PremiumDropdown(
                options = listOf("Savings", "Current", "Cash", "Credit Card", "Salary", "Investment"),
                selectedOption = walletType,
                onOptionSelected = { walletType = it },
                label = "Wallet Type",
                icon = Icons.Default.AccountBalanceWallet
            )

            Spacer(Modifier.height(16.dp))

            // Bank Name
            PremiumTextField(
                value = bankName,
                onValueChange = { bankName = it },
                label = "Bank Name",
                icon = Icons.Default.AccountBalance
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(1f)) {
                    // --- INPUT FIELD: Card Number (Last 4 Digits) ---
                    PremiumTextField(
                        value = cardNumber,
                        onValueChange = { if (it.length <= 4) cardNumber = it },
                        label = "Last 4",
                        icon = Icons.Default.Pin,
                        keyboardType = KeyboardType.Number
                    )
                }
                Box(Modifier.weight(1f)) {
                    PremiumTextField(
                        value = balance,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) balance = it },
                        label = "Balance",
                        icon = Icons.Default.AttachMoney,
                        keyboardType = KeyboardType.Decimal
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // --- 4. SAVE BUTTON ---
            Button(
                onClick = {
                    if (bankName.isBlank()) {
                        Toast.makeText(context, "Please enter a Bank Name", Toast.LENGTH_SHORT).show()
                    } else {
                        val balVal = balance.toDoubleOrNull() ?: 0.0

                        // --- CREATING WALLET OBJECT CORRECTLY ---
                        val wallet = Wallet(
                            id = editingWallet?.id ?: "",
                            name = walletType, // Save type as name
                            bankName = bankName,
                            cardNumber = cardNumber, // MATCHING VARIABLE NAME
                            balance = balVal,
                            cardThemeId = selectedThemeIndex
                        )
                        viewModel.addOrUpdateWallet(wallet) {
                            Toast.makeText(context, "Wallet Saved", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save Wallet", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // --- DELETE CONFIRMATION DIALOG ---
    if (showDeleteDialog) {
        PremiumAlertDialog(
            title = "Delete Wallet?",
            message = "Are you sure you want to delete this wallet? All transaction history associated with it will be lost permanently.",
            confirmText = "Delete",
            dismissText = "Cancel",
            icon = Icons.Default.DeleteForever,
            confirmButtonColor = MaterialTheme.colorScheme.error, // Red for destructive action
            iconColor = MaterialTheme.colorScheme.error,
            onConfirm = {
                editingWallet?.let { wallet ->
                    viewModel.deleteWallet(wallet.id) {
                        showDeleteDialog = false
                        onBack()
                    }
                }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

// --- COMPONENTS ---

@Composable
fun PremiumDropdown(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    label: String,
    icon: ImageVector
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                Icon(
                    if(expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    null,
                    modifier = Modifier.clickable { expanded = !expanded }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            singleLine = true
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(16.dp))
                .clickable { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                    leadingIcon = {
                        if (option == selectedOption) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        singleLine = true
    )
}

fun Modifier.shadow(elevation: Dp, shape: androidx.compose.ui.graphics.Shape) = this.then(
    Modifier.graphicsLayer {
        shadowElevation = elevation.toPx()
        this.shape = shape
        clip = true
    }
)