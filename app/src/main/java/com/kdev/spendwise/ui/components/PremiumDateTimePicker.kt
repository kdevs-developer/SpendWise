package com.kdev.spendwise.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDateTimePickerDialog(
    initialDateMillis: Long,
    initialHour: Int,
    initialMinute: Int,
    initialIsAm: Boolean,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Long, Int, Int, Boolean) -> Unit
) {
    var selectedDate by remember { mutableLongStateOf(initialDateMillis) }
    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }
    var isAm by remember { mutableStateOf(initialIsAm) }

    // Navigation State
    var viewYear by remember { mutableIntStateOf(Calendar.getInstance().apply { timeInMillis = initialDateMillis }.get(Calendar.YEAR)) }
    var viewMonth by remember { mutableIntStateOf(Calendar.getInstance().apply { timeInMillis = initialDateMillis }.get(Calendar.MONTH)) }

    // Toggle Mode
    var isWheelMode by remember { mutableStateOf(false) }

    val monthName = remember(viewMonth) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, viewMonth)
        SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp).wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(Modifier.padding(20.dp)) {
                // HEADER
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hide arrows in Wheel Mode
                    if(!isWheelMode) {
                        IconButton(onClick = { if (viewMonth == 0) { viewMonth = 11; viewYear-- } else { viewMonth-- } }) { Icon(Icons.Default.ChevronLeft, null) }
                    } else { Spacer(Modifier.size(48.dp)) }

                    TextButton(
                        onClick = { isWheelMode = !isWheelMode },
                        colors = ButtonDefaults.textButtonColors(contentColor = primaryColor)
                    ) {
                        Text("$monthName $viewYear", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        Icon(if(isWheelMode) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.padding(start = 4.dp))
                    }

                    if(!isWheelMode) {
                        IconButton(onClick = { if (viewMonth == 11) { viewMonth = 0; viewYear++ } else { viewMonth++ } }) { Icon(Icons.Default.ChevronRight, null) }
                    } else { Spacer(Modifier.size(48.dp)) }
                }

                // CONTENT
                AnimatedContent(
                    targetState = isWheelMode,
                    transitionSpec = { fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300)) },
                    label = "CalendarViewSwitch"
                ) { showWheel ->
                    if (showWheel) {
                        WheelDatePicker(
                            initialMonth = viewMonth,
                            initialYear = viewYear,
                            onSelectionChanged = { m, y ->
                                viewMonth = m
                                viewYear = y
                            },
                            primaryColor = primaryColor
                        )
                    } else {
                        CalendarGrid(
                            year = viewYear,
                            month = viewMonth,
                            selectedDateMillis = selectedDate,
                            onDateSelected = { selectedDate = it },
                            primaryColor = primaryColor
                        )
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 16.dp).alpha(0.2f))

                // TIME
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
                    NumberPicker(selectedHour, 1..12) { selectedHour = it }
                    Text(":", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    NumberPicker(selectedMinute, 0..59, { String.format("%02d", it) }) { selectedMinute = it }
                    AmPmPicker(isAm) { isAm = it }
                }

                Spacer(Modifier.height(24.dp))

                // BUTTONS
                Row(Modifier.fillMaxWidth(), Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                    Spacer(Modifier.width(8.dp))

                    // UPDATED BUTTON LOGIC
                    Button(
                        onClick = {
                            if (isWheelMode) {
                                // If in Wheel Mode, "Set" just switches to Grid view
                                isWheelMode = false
                            } else {
                                // If in Grid Mode, "Confirm" finishes the dialog
                                val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                                val originalDay = cal.get(Calendar.DAY_OF_MONTH)
                                cal.set(Calendar.YEAR, viewYear)
                                cal.set(Calendar.MONTH, viewMonth)
                                val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                                cal.set(Calendar.DAY_OF_MONTH, originalDay.coerceAtMost(maxDays))
                                onConfirm(cal.timeInMillis, selectedHour, selectedMinute, isAm)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isWheelMode) "Set" else "Confirm", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarGrid(year: Int, month: Int, selectedDateMillis: Long, onDateSelected: (Long) -> Unit, primaryColor: Color) {
    val calendar = Calendar.getInstance().apply { set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, 1) }
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val startDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val weekDays = listOf("S", "M", "T", "W", "T", "F", "S")

    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }

    Column {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            weekDays.forEach { day -> Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        }
        Spacer(Modifier.height(12.dp))

        val totalSlots = (startDayOfWeek - 1) + daysInMonth
        val rows = (totalSlots / 7) + if (totalSlots % 7 == 0) 0 else 1

        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), Arrangement.SpaceBetween) {
                for (col in 0 until 7) {
                    val dayIndex = (row * 7 + col) - (startDayOfWeek - 1) + 1
                    if (dayIndex in 1..daysInMonth) {
                        val currentDayCal = Calendar.getInstance().apply { set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, dayIndex); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                        val selectedCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                        val isSelected = currentDayCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                                currentDayCal.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR)

                        val isFuture = currentDayCal.timeInMillis > todayStart.timeInMillis

                        Box(
                            modifier = Modifier.weight(1f).aspectRatio(1f).clip(CircleShape)
                                .background(if (isSelected) primaryColor else Color.Transparent)
                                .clickable(enabled = !isFuture) { onDateSelected(currentDayCal.timeInMillis) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayIndex.toString(),
                                color = if (isSelected) Color.White else if(isFuture) Color.LightGray.copy(0.4f) else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelDatePicker(initialMonth: Int, initialYear: Int, onSelectionChanged: (Int, Int) -> Unit, primaryColor: Color) {
    val months = remember { DateFormatSymbols().months.toList() }

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = remember { (1950..currentYear).map { it.toString() }.toList() }

    // Internal state to avoid jumpiness
    var currentMonthIndex by remember { mutableIntStateOf(initialMonth) }
    var currentYearIndex by remember { mutableIntStateOf(years.indexOf(initialYear.toString()).coerceAtLeast(0)) }

    Row(modifier = Modifier.fillMaxWidth().height(180.dp), horizontalArrangement = Arrangement.Center) {

        WheelColumn(
            items = months,
            initialIndex = currentMonthIndex,
            onItemSelected = { index ->
                currentMonthIndex = index
                val yearStr = years.getOrNull(currentYearIndex) ?: years.last()
                onSelectionChanged(currentMonthIndex, yearStr.toInt())
            },
            primaryColor = primaryColor,
            modifier = Modifier.weight(1f)
        )

        WheelColumn(
            items = years,
            initialIndex = currentYearIndex,
            onItemSelected = { index ->
                currentYearIndex = index
                val yearStr = years[index]
                onSelectionChanged(currentMonthIndex, yearStr.toInt())
            },
            primaryColor = primaryColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelColumn(
    items: List<String>,
    initialIndex: Int,
    onItemSelected: (Int) -> Unit,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { items.size }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            onItemSelected(page)
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth().height(40.dp).background(primaryColor.copy(0.1f), RoundedCornerShape(8.dp)))

        VerticalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(40.dp),
            contentPadding = PaddingValues(vertical = 70.dp),
            flingBehavior = rememberSnapFlingBehavior(lazyListState = rememberLazyListState()) as TargetedFlingBehavior
        ) { page ->
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            val scale = lerp(1f, 0.7f, pageOffset.coerceIn(0f, 1f))
            val alpha = lerp(1f, 0.3f, pageOffset.coerceIn(0f, 1f))

            Box(
                modifier = Modifier
                    .height(40.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha },
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalContentColor provides if(page == pagerState.currentPage) primaryColor else Color.Gray) {
                    Text(text = items[page], style = MaterialTheme.typography.titleMedium, fontWeight = if(page == pagerState.currentPage) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
fun NumberPicker(value: Int, range: IntRange, format: (Int) -> String = { it.toString() }, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { onValueChange(if (value < range.last) value + 1 else range.first) }) { Icon(Icons.Default.ArrowDropUp, null) }
        Text(format(value), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        IconButton(onClick = { onValueChange(if (value > range.first) value - 1 else range.last) }) { Icon(Icons.Default.ArrowDropDown, null) }
    }
}

@Composable
fun AmPmPicker(isAm: Boolean, onToggle: (Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val shape = RoundedCornerShape(8.dp)
        listOf(true to "AM", false to "PM").forEach { (type, label) -> Surface(onClick = { onToggle(type) }, shape = shape, color = if (isAm == type) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, border = BorderStroke(1.dp, Color.LightGray)) { Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold) }; if (type) Spacer(Modifier.height(4.dp)) }
    }
}