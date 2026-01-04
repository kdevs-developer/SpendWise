package com.kdev.spendwise.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kdev.spendwise.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryNestedPicker(
    viewModel: MainViewModel,
    isExpense: Boolean,
    onDismiss: () -> Unit,
    onConfirmed: (String, String) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var tempMain by remember { mutableStateOf("") }

    // Edit/Delete states (Optional: keep if you want to allow deleting existing custom ones,
    // otherwise these can be removed too if you want a strictly read-only list)
    var itemToEdit by remember { mutableStateOf<String?>(null) }
    var itemToDelete by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = true) { if (step == 2) step = 1 else onDismiss() }

    val categories = viewModel.getCategoriesForType(isExpense)
    val sortedMainCategories = categories.keys.toList().sorted()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                // --- HEADER ---
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (step == 1) "Categories" else tempMain,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { if (step == 1) onDismiss() else step = 1 }) {
                            Icon(if (step == 1) Icons.Default.Close else Icons.Default.ArrowBack, null)
                        }
                    }
                )

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.2f))

                // --- CONTENT (LIST VIEW) ---
                if (categories.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Category, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("No categories found", color = Color.Gray, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                } else {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally { width -> width } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> -width } + fadeOut()
                            } else {
                                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> width } + fadeOut()
                            }
                        },
                        label = "CategoryStepAnimation"
                    ) { currentStep ->
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (currentStep == 1) {
                                // MAIN CATEGORIES LIST
                                items(sortedMainCategories) { mainCat ->
                                    CategoryListItem(
                                        name = mainCat,
                                        iconVector = viewModel.getIconForCategory(mainCat),
                                        showChevron = true,
                                        onClick = { tempMain = mainCat; step = 2 },
                                        onEdit = { /* Disabled editing main cats */ },
                                        onDelete = { /* Disabled deleting main cats */ }
                                    )
                                }
                            } else {
                                // SUB CATEGORIES LIST
                                val subCats = categories[tempMain] ?: emptyList()

                                items(subCats) { subCat ->
                                    CategoryListItem(
                                        name = subCat,
                                        iconVector = viewModel.getIconForCategory(subCat),
                                        showChevron = false,
                                        onClick = { onConfirmed(tempMain, subCat) },
                                        onEdit = { /* Disabled editing sub cats */ },
                                        onDelete = { /* Disabled deleting sub cats */ }
                                    )
                                }

                                // REMOVED: "Add New" Button
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- LIST ITEM COMPONENT ---
@Composable
fun CategoryListItem(
    name: String,
    iconVector: ImageVector,
    showChevron: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconVector, null, tint = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.width(16.dp))

            // Name
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            // Actions
            if (showChevron) {
                Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
            }
        }
    }
}