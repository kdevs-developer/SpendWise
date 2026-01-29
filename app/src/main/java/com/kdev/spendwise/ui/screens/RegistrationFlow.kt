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
import androidx.compose.foundation.pager.PagerDefaults
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

    LaunchedEffect(pagerState.settledPage) {
        onItemSelected(pagerState.settledPage)
    }

    Box(
        modifier = modifier.height(200.dp), // Increased container height
        contentAlignment = Alignment.Center
    ) {
        // Selection Indicator Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp) // Slightly taller bar
                .padding(horizontal = 4.dp)
                .background(primaryColor.copy(0.1f), RoundedCornerShape(12.dp))
        )

        VerticalPager(
            state = pagerState,
            flingBehavior = PagerDefaults.flingBehavior(state = pagerState),
            contentPadding = PaddingValues(vertical = 76.dp), // Fine-tuned for 200.dp height
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 2,
            userScrollEnabled = true
        ) { page ->
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)

            // Reduced max scale slightly to prevent clipping and used alpha for focus
            val scale = 1f - (pageOffset.absoluteValue * 0.25f).coerceIn(0f, 0.4f)
            val alpha = 1f - (pageOffset.absoluteValue * 0.6f).coerceIn(0f, 0.8f)
            val rotationX = (pageOffset * 25f).coerceIn(-45f, 45f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp) // Match the indicator bar height
                    .graphicsLayer {
                        this.scaleX = scale
                        this.scaleY = scale
                        this.alpha = alpha
                        this.rotationX = rotationX
                        this.cameraDistance = 12 * density
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = items[page],
                    // Reduced size from titleLarge to titleMedium for better fit
                    style = MaterialTheme.typography.titleMedium,
                    color = if (pagerState.currentPage == page) primaryColor else Color.Gray,
                    fontWeight = if (pagerState.currentPage == page) FontWeight.ExtraBold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DobPickerDialog(
    initialDateMillis: Long,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var selectedDate by remember { mutableLongStateOf(initialDateMillis) }

    var viewYear by remember { mutableIntStateOf(Calendar.getInstance().apply { timeInMillis = initialDateMillis }.get(Calendar.YEAR)) }
    var viewMonth by remember { mutableIntStateOf(Calendar.getInstance().apply { timeInMillis = initialDateMillis }.get(Calendar.MONTH)) }

    var isWheelMode by remember { mutableStateOf(false) }

    val monthName = remember(viewMonth) {
        SimpleDateFormat("MMMM", Locale.getDefault()).format(Calendar.getInstance().apply { set(Calendar.MONTH, viewMonth) }.time)
    }

    val monthsList = remember { DateFormatSymbols().months.filter { it.isNotEmpty() } }
    val yearsList = remember { (1900..Calendar.getInstance().get(Calendar.YEAR)).map { it.toString() } }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp) // More breathing room
                .wrapContentHeight(),
            shape = RoundedCornerShape(32.dp), // More rounded for modern look
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    "Date of Birth",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
                )

                // --- HEADER ---
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isWheelMode) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                if (viewMonth == 0) { viewMonth = 11; viewYear-- } else { viewMonth-- }
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(0.4f), CircleShape)
                        ) { Icon(Icons.Default.ChevronLeft, null) }
                    } else Spacer(Modifier.size(40.dp))

                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            isWheelMode = !isWheelMode
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "$monthName $viewYear",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryColor
                        )
                        Icon(if (isWheelMode) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = primaryColor)
                    }

                    if (!isWheelMode) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                if (viewMonth == 11) { viewMonth = 0; viewYear++ } else { viewMonth++ }
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(0.4f), CircleShape)
                        ) { Icon(Icons.Default.ChevronRight, null) }
                    } else Spacer(Modifier.size(40.dp))
                }

                // --- CONTENT ---
                AnimatedContent(
                    targetState = isWheelMode,
                    transitionSpec = {
                        (scaleIn(tween(300)) + fadeIn()).togetherWith(scaleOut(tween(200)) + fadeOut())
                    },
                    label = "PickerTransition"
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
                            onDateSelected = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                selectedDate = it
                            },
                            primaryColor = primaryColor
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // --- ACTIONS ---
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.LightGray.copy(0.5f))
                    ) { Text("Cancel", color = Color.Gray) }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            if (isWheelMode) isWheelMode = false else onConfirm(selectedDate)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(if (isWheelMode) "Set Date" else "Confirm", fontWeight = FontWeight.Bold)
                    }
                }
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
    var currentMonth by remember { mutableIntStateOf(initialMonth) }
    var currentYear by remember { mutableIntStateOf(initialYear) }

    Row(Modifier.fillMaxWidth().height(180.dp), Arrangement.Center) {
        GenericWheelColumn(
            items = months,
            initialIndex = initialMonth,
            onItemSelected = {
                currentMonth = it
                onSelectionChanged(currentMonth, currentYear)
            },
            primaryColor = primaryColor,
            modifier = Modifier.weight(1.2f)
        )
        GenericWheelColumn(
            items = years,
            initialIndex = years.indexOf(initialYear.toString()).coerceAtLeast(0),
            onItemSelected = {
                currentYear = years[it].toInt()
                onSelectionChanged(currentMonth, currentYear)
            },
            primaryColor = primaryColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun DobCalendarGrid(year: Int, month: Int, selectedDateMillis: Long, onDateSelected: (Long) -> Unit, primaryColor: Color) {
    //
    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val startDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val weekDays = listOf("S", "M", "T", "W", "T", "F", "S")

    Column {
        Row(Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray.copy(0.7f),
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        val totalSlots = (startDayOfWeek - 1) + daysInMonth
        val rows = (totalSlots + 6) / 7

        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val dayIndex = (row * 7 + col) - (startDayOfWeek - 1) + 1
                    if (dayIndex in 1..daysInMonth) {
                        val currentDayCal = Calendar.getInstance().apply {
                            set(year, month, dayIndex, 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val isSelected = currentDayCal.timeInMillis == Calendar.getInstance().apply {
                            timeInMillis = selectedDateMillis
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }.timeInMillis

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) primaryColor else Color.Transparent)
                                .clickable { onDateSelected(currentDayCal.timeInMillis) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayIndex.toString(),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    } else {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}