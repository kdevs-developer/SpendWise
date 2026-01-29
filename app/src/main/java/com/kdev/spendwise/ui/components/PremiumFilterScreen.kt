package com.kdev.spendwise.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdev.spendwise.ui.MainViewModel
import com.kdev.spendwise.ui.screens.TimeFilter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PremiumFilterScreen(
    viewModel: MainViewModel,
    currentCategories: Set<String>,
    selectedTimeFilter: TimeFilter,
    customDateRange: Pair<Long?, Long?>,
    onApply: (Set<String>, TimeFilter, Pair<Long?, Long?>) -> Unit,
    onDismiss: () -> Unit
) {
    // Handle back press
    BackHandler { onDismiss() }

    // Tab State: 0 = Period, 1 = Categories
    var activeTab by remember { mutableIntStateOf(0) }

    // Temporary Filter State
    var tempCategories by remember { mutableStateOf(currentCategories) }
    var tempTimeFilter by remember { mutableStateOf(selectedTimeFilter) }

    val dateState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = customDateRange.first,
        initialSelectedEndDateMillis = customDateRange.second
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxSize(),
        // 1. PREMIUM TOP BAR
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Filter Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        tempCategories = emptySet()
                        tempTimeFilter = TimeFilter.Month
                    }) {
                        Text("Reset", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        // 2. FIXED BOTTOM BAR FOR "APPLY"
        bottomBar = {
            Surface(
                shadowElevation = 16.dp,
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.navigationBarsPadding() // Handled safety for bottom nav
            ) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Button(
                        onClick = {
                            val range = if (tempTimeFilter == TimeFilter.Custom) {
                                dateState.selectedStartDateMillis to dateState.selectedEndDateMillis
                            } else {
                                null to null
                            }
                            onApply(tempCategories, tempTimeFilter, range)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            "Apply Filters",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        // 3. SPLIT VIEW CONTENT
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- LEFT SIDEBAR (TABS) ---
            Column(
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                SidebarTabItem(
                    label = "Period",
                    icon = Icons.Default.DateRange,
                    isSelected = activeTab == 0,
                    onClick = { activeTab = 0 }
                )
                SidebarTabItem(
                    label = "Category",
                    icon = Icons.Default.Category,
                    isSelected = activeTab == 1,
                    onClick = { activeTab = 1 }
                )
            }

            // --- RIGHT CONTENT AREA ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                when (activeTab) {
                    0 -> PeriodFilterContent(
                        currentFilter = tempTimeFilter,
                        onFilterSelect = { tempTimeFilter = it },
                        dateState = dateState
                    )

                    1 -> CategoriesFilterContent(
                        categoryMap = viewModel.categoryMap,
                        selectedCategories = tempCategories,
                        viewModel = viewModel,
                        onCategoryToggle = { cat ->
                            tempCategories = if (cat in tempCategories) {
                                tempCategories - cat
                            } else {
                                tempCategories + cat
                            }
                        }
                    )
                }
            }
        }
    }
}

// ============================================================================================
// UI COMPONENTS
// ============================================================================================

@Composable
fun SidebarTabItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection Indicator Line
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        // Icon & Label
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = fontWeight
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodFilterContent(
    currentFilter: TimeFilter,
    onFilterSelect: (TimeFilter) -> Unit,
    dateState: DateRangePickerState
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        TimeFilter.values().forEach { filter ->
            val isSelected = currentFilter == filter
            val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(0.2f)

            Surface(
                onClick = { onFilterSelect(filter) },
                shape = RoundedCornerShape(12.dp),
                color = containerColor,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = filter.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if(isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (currentFilter == TimeFilter.Custom) {
            Spacer(Modifier.height(16.dp))
            Text("Select Range", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            // Embed Date Picker
            DateRangePicker(
                state = dateState,
                showModeToggle = false,
                title = null,
                headline = null,
                modifier = Modifier.height(320.dp)
            )
        }

        Spacer(Modifier.height(80.dp)) // Padding for bottom bar
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoriesFilterContent(
    categoryMap: Map<String, List<String>>,
    selectedCategories: Set<String>,
    viewModel: MainViewModel,
    onCategoryToggle: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 100.dp) // Padding for bottom bar
    ) {
        // "Transaction Type" Section for Transfer
        item {
            Column {
                Text(
                    text = "Transaction Type",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))

                val isTransferSelected = "Transfer" in selectedCategories
                FilterChip(
                    selected = isTransferSelected,
                    onClick = { onCategoryToggle("Transfer") },
                    label = { Text("Transfer") },
                    leadingIcon = {
                        Icon(Icons.Default.SwapHoriz, null, Modifier.size(18.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = MaterialTheme.colorScheme.outline.copy(0.3f),
                        enabled = true, selected = isTransferSelected
                    )
                )
            }
        }

        // Standard Categories
        categoryMap.forEach { (mainCategory, subList) ->
            item {
                Column {
                    Text(
                        text = mainCategory,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        subList.forEach { sub ->
                            val fullName = "$mainCategory -> $sub"
                            val isSelected = fullName in selectedCategories

                            FilterChip(
                                selected = isSelected,
                                onClick = { onCategoryToggle(fullName) },
                                label = { Text(sub) },
                                leadingIcon = {
                                    Icon(
                                        viewModel.getIconForCategory(sub),
                                        null,
                                        Modifier.size(18.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = MaterialTheme.colorScheme.outline.copy(0.3f),
                                    enabled = true, selected = isSelected
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}