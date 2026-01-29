package com.kdev.spendwise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kdev.spendwise.data.RecurringRule
import com.kdev.spendwise.ui.MainViewModel
import com.kdev.spendwise.ui.components.PremiumAlertDialog
import com.kdev.spendwise.ui.components.RecurringRuleItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (RecurringRule) -> Unit
) {
    val rules = viewModel.recurringRules
    val wallets = viewModel.wallets // [CHANGE] Get wallets list
    var showDeleteDialog by remember { mutableStateOf<RecurringRule?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Standing Instructions",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Automate your recurring payments",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearRecurringEdit()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (rules.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No standing instructions set up.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rules) { rule ->
                        // [CHANGE] Find the wallet for this rule
                        val linkedWallet = wallets.find { it.id == rule.walletId }

                        RecurringRuleItem(
                            rule = rule,
                            wallet = linkedWallet, // [CHANGE] Pass wallet to component
                            viewModel = viewModel,
                            onClick = { onNavigateToEdit(rule) },
                            onDelete = { showDeleteDialog = rule }
                        )
                    }
                }
            }
        }
    }

    // Delete Dialog
    if (showDeleteDialog != null) {
        PremiumAlertDialog(
            title = "Delete Payment?",
            message = "Are you sure you want to delete '${showDeleteDialog?.title}'? Future transactions will stop.",
            confirmText = "Delete",
            dismissText = "Cancel",
            icon = Icons.Default.DeleteForever,
            confirmButtonColor = MaterialTheme.colorScheme.error,
            iconColor = MaterialTheme.colorScheme.error,
            onConfirm = {
                showDeleteDialog?.let { rule ->
                    viewModel.deleteRecurringRule(rule.id)
                }
                showDeleteDialog = null
            },
            onDismiss = { showDeleteDialog = null }
        )
    }
}