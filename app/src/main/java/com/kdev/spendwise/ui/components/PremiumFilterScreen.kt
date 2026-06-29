package com.kdev.spendwise.ui.components

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdev.spendwise.ui.MainViewModel
import com.kdev.spendwise.ui.screens.TimeFilter
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalLocale

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
    var tempCustomRange by remember { mutableStateOf(customDateRange) }

    var showDatePickerDialog by remember { mutableStateOf(false) }

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
                        tempCustomRange = null to null
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
                modifier = Modifier.navigationBarsPadding()
            ) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Button(
                        onClick = { onApply(tempCategories, tempTimeFilter, tempCustomRange) },
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
                        Text("Apply Filters", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                        customRange = tempCustomRange,
                        onFilterSelect = { filter ->
                            tempTimeFilter = filter
                            if (filter == TimeFilter.Custom) {
                                showDatePickerDialog = true // Trigger Premium Modal Instantly
                            }
                        },
                        onOpenPicker = { showDatePickerDialog = true }
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

    // --- PREMIUM MODAL POPUP ---
    if (showDatePickerDialog) {
        PremiumDateRangePickerDialog(
            initialStart = tempCustomRange.first,
            initialEnd = tempCustomRange.second,
            onDismiss = {
                showDatePickerDialog = false
                // Revert to month if they cancelled without selecting anything
                if (tempCustomRange.first == null) tempTimeFilter = TimeFilter.Month
            },
            onDateRangeSelected = { start, end ->
                tempCustomRange = start to end
                tempTimeFilter = TimeFilter.Custom
                showDatePickerDialog = false
            }
        )
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
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

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

@Composable
fun PeriodFilterContent(
    currentFilter: TimeFilter,
    customRange: Pair<Long?, Long?>,
    onFilterSelect: (TimeFilter) -> Unit,
    onOpenPicker: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM, yy", Locale.getDefault()) }

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

        // Beautiful Summary Card for Custom Dates
        AnimatedVisibility(visible = currentFilter == TimeFilter.Custom && customRange.first != null) {
            Column {
                Spacer(Modifier.height(16.dp))
                Text("Selected Range", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "From: ${customRange.first?.let { sdf.format(Date(it)) } ?: "--"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "To: ${customRange.second?.let { sdf.format(Date(it)) } ?: "--"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(
                            onClick = onOpenPicker,
                            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Range", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
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
        contentPadding = PaddingValues(bottom = 100.dp)
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

// ============================================================================================
// PREMIUM MODAL: DATE RANGE PICKER
// ============================================================================================
@SuppressLint("NonObservableLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumDateRangePickerDialog(
    initialStart: Long?,
    initialEnd: Long?,
    onDismiss: () -> Unit,
    onDateRangeSelected: (Long, Long) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStart,
        initialSelectedEndDateMillis = initialEnd
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        colors = DatePickerDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        confirmButton = {
            Button(
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis ?: start
                    if (start != null && end != null) {
                        onDateRangeSelected(start, end)
                    }
                },
                enabled = dateRangePickerState.selectedStartDateMillis != null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
            ) {
                Text("Select Range", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("Cancel", color = Color.Gray, fontWeight = FontWeight.SemiBold)
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = {
                Text(
                    text = "Custom Date Filter",
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            headline = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Start Date
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "From",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = if (dateRangePickerState.selectedStartDateMillis != null) {
                                SimpleDateFormat("dd MMM, yy", LocalLocale.current.platformLocale).format(Date(dateRangePickerState.selectedStartDateMillis!!))
                            } else "--",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (dateRangePickerState.selectedStartDateMillis != null) MaterialTheme.colorScheme.onSurface else Color.LightGray
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "to",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // End Date
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = "To",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = if (dateRangePickerState.selectedEndDateMillis != null) {
                                SimpleDateFormat("dd MMM, yy", LocalLocale.current.platformLocale).format(Date(dateRangePickerState.selectedEndDateMillis!!))
                            } else "--",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (dateRangePickerState.selectedEndDateMillis != null) MaterialTheme.colorScheme.onSurface else Color.LightGray
                        )
                    }
                }
            },
            showModeToggle = false,
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.primary,
                headlineContentColor = MaterialTheme.colorScheme.onSurface,
                weekdayContentColor = Color.Gray,
                subheadContentColor = MaterialTheme.colorScheme.primary,
                yearContentColor = MaterialTheme.colorScheme.onSurface,
                currentYearContentColor = MaterialTheme.colorScheme.primary,
                selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                dayContentColor = MaterialTheme.colorScheme.onSurface,
                selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                todayContentColor = MaterialTheme.colorScheme.primary,
                todayDateBorderColor = MaterialTheme.colorScheme.primary,
                dayInSelectionRangeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                dayInSelectionRangeContainerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}