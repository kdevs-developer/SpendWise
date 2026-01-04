package com.kdev.spendwise.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kdev.spendwise.ui.MainViewModel
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationFlow(
    viewModel: MainViewModel,
    onComplete: () -> Unit
) {
    val context = LocalContext.current

    // Form State
    var name by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var selectedAvatarIndex by remember { mutableIntStateOf(0) }

    // Loading State
    var isLoading by remember { mutableStateOf(false) }

    // Picker States
    var showDobPicker by remember { mutableStateOf(false) }
    var showGenderPicker by remember { mutableStateOf(false) }

    val currentCalendar = remember { Calendar.getInstance() }
    var selectedDateMillis by remember { mutableLongStateOf(currentCalendar.timeInMillis) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Button(
                onClick = {
                    if (name.isBlank() || dob.isBlank() || gender.isBlank()) {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    } else {
                        isLoading = true
                        viewModel.finalizeProfile(
                            name = name,
                            dob = dob,
                            gender = gender,
                            balance = 0.0,
                            avatarIdx = selectedAvatarIndex,
                            onSuccess = {
                                isLoading = false
                                onComplete()
                            },
                            onFailure = { errorMsg ->
                                isLoading = false
                                Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("COMPLETE REGISTRATION", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            Text(
                "Tell us about yourself",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Help us personalize your experience",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(Modifier.height(32.dp))

            // AVATAR SELECTOR
            Text("Select Avatar", fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                viewModel.availableAvatars.forEachIndexed { index, drawableId ->
                    val isSelected = selectedAvatarIndex == index
                    val scale by animateFloatAsState(if (isSelected) 1.1f else 1f, label = "avatarScale")
                    val border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null

                    Card(
                        shape = CircleShape,
                        border = border,
                        modifier = Modifier
                            .size(55.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clickable { selectedAvatarIndex = index }
                    ) {
                        Image(
                            painter = painterResource(id = drawableId),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // NAME FIELD
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Person, null) },
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            // DATE OF BIRTH FIELD
            OutlinedTextField(
                value = dob,
                onValueChange = { },
                label = { Text("Date of Birth") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDobPicker = true },
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Cake, null) },
                trailingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(Modifier.height(16.dp))

            // GENDER FIELD
            OutlinedTextField(
                value = gender,
                onValueChange = {},
                label = { Text("Gender") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showGenderPicker = true },
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Wc, null) },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(Modifier.height(100.dp))
        }
    }

    // --- DIALOGS ---

    if (showDobPicker) {
        DobPickerDialog(
            initialDateMillis = selectedDateMillis,
            primaryColor = MaterialTheme.colorScheme.primary,
            onDismiss = { showDobPicker = false },
            onConfirm = { dateMillis ->
                selectedDateMillis = dateMillis
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                dob = sdf.format(Date(dateMillis))
                showDobPicker = false
            }
        )
    }

    if (showGenderPicker) {
        GenderPickerDialog(
            initialSelection = if(gender.isNotEmpty()) gender else "Male",
            primaryColor = MaterialTheme.colorScheme.primary,
            onDismiss = { showGenderPicker = false },
            onConfirm = { selected ->
                gender = selected
                showGenderPicker = false
            }
        )
    }
}

// ==========================================
//      GENDER WHEEL COMPONENTS
// ==========================================

@Composable
fun GenderPickerDialog(
    initialSelection: String,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val options = listOf("Male", "Female", "Other")
    var currentSelection by remember { mutableStateOf(initialSelection) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp).wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Select Gender",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(24.dp))

                Box(modifier = Modifier.height(180.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    GenericWheelColumn(
                        items = options,
                        initialIndex = options.indexOf(initialSelection).coerceAtLeast(0),
                        onItemSelected = { idx -> currentSelection = options[idx] },
                        primaryColor = primaryColor
                    )
                }

                Spacer(Modifier.height(32.dp))

                Row(Modifier.fillMaxWidth(), Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(currentSelection) },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Confirm", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// ==========================================
//      DOB PICKER COMPONENTS (UPDATED)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DobPickerDialog(
    initialDateMillis: Long,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    // Current selected date (for Grid)
    var selectedDate by remember { mutableLongStateOf(initialDateMillis) }

    // Navigation State (Current view focus)
    var viewYear by remember { mutableIntStateOf(Calendar.getInstance().apply { timeInMillis = initialDateMillis }.get(Calendar.YEAR)) }
    var viewMonth by remember { mutableIntStateOf(Calendar.getInstance().apply { timeInMillis = initialDateMillis }.get(Calendar.MONTH)) }

    // Toggle Mode
    var isWheelMode by remember { mutableStateOf(false) }

    val monthName = remember(viewMonth) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, viewMonth)
        SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)
    }

    val monthsList = remember { DateFormatSymbols().months.filter { it.isNotEmpty() } }
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val yearsList = remember { (1900..currentYear).map { it.toString() }.toList() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp).wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(Modifier.padding(20.dp)) {

                Text(
                    "Select Date of Birth",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp)
                )

                // HEADER (Switch between Wheel and Grid)
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
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.95f, animationSpec = tween(200))) togetherWith
                                (fadeOut(animationSpec = tween(200)))
                    },
                    label = "CalendarSwitch"
                ) { showWheel ->
                    if (showWheel) {
                        DobWheelDatePicker(
                            months = monthsList,
                            years = yearsList,
                            initialMonth = viewMonth,
                            initialYear = viewYear,
                            onSelectionChanged = { m, y ->
                                viewMonth = m
                                viewYear = y
                            },
                            primaryColor = primaryColor
                        )
                    } else {
                        DobCalendarGrid(
                            year = viewYear,
                            month = viewMonth,
                            selectedDateMillis = selectedDate,
                            onDateSelected = { selectedDate = it },
                            primaryColor = primaryColor
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ACTION BUTTONS
                Row(Modifier.fillMaxWidth(), Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                    Spacer(Modifier.width(8.dp))

                    // UPDATED BUTTON LOGIC
                    Button(
                        onClick = {
                            if (isWheelMode) {
                                // If in Wheel Mode, "Set" switches to Calendar Grid
                                isWheelMode = false
                            } else {
                                // If in Calendar Mode, "Confirm" finishes selection
                                onConfirm(selectedDate)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        // Dynamic Text: Set vs Confirm
                        Text(if (isWheelMode) "Set" else "Confirm", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- CALENDAR GRID ---
@Composable
fun DobCalendarGrid(year: Int, month: Int, selectedDateMillis: Long, onDateSelected: (Long) -> Unit, primaryColor: Color) {
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
                        val isSelected = currentDayCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) && currentDayCal.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR)
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

// --- OPTIMIZED WHEEL COMPONENTS ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GenericWheelColumn(
    items: List<String>,
    initialIndex: Int,
    onItemSelected: (Int) -> Unit,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { items.size }

    // Use snapshotFlow to detect settled page changes only
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
                val isSelected = page == pagerState.currentPage
                Text(
                    text = items[page],
                    style = MaterialTheme.typography.titleMedium,
                    color = if(isSelected) primaryColor else Color.Gray,
                    fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun DobWheelDatePicker(
    months: List<String>,
    years: List<String>,
    initialMonth: Int,
    initialYear: Int,
    onSelectionChanged: (Int, Int) -> Unit,
    primaryColor: Color
) {
    // Isolate local state to avoid jumpy behavior
    var currentMonthIndex by remember { mutableIntStateOf(initialMonth) }
    var currentYearIndex by remember { mutableIntStateOf(years.indexOf(initialYear.toString()).coerceAtLeast(0)) }

    Row(modifier = Modifier.fillMaxWidth().height(180.dp), horizontalArrangement = Arrangement.Center) {
        GenericWheelColumn(
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
        GenericWheelColumn(
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