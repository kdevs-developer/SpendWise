package com.kdev.spendwise.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kdev.spendwise.data.Wallet
import com.kdev.spendwise.ui.MainViewModel
import com.kdev.spendwise.data.cardThemes
import com.kdev.spendwise.util.CurrencyUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WalletListScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onEditWallet: () -> Unit,
    onAddWallet: () -> Unit
) {
    var localWallets by remember(viewModel.wallets) { mutableStateOf(viewModel.wallets.toList()) }

    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // Premium Drag Physics & Lock States
    var draggedWalletId by remember { mutableStateOf<String?>(null) } // Locks gesture to one card
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    val dragVisualOffset = remember { Animatable(0f) }
    var isDraggingState by remember { mutableStateOf(false) }

    // Compiler-safe continuous Jiggle Animation
    val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
    val jiggleRotation by infiniteTransition.animateFloat(
        initialValue = if (isDraggingState) -1.5f else 0f,
        targetValue = if (isDraggingState) 1.5f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jiggle_anim"
    )

    // The core swap logic isolated so it can run during manual drag AND auto-scroll
    val checkAndSwap = {
        val currentIndex = draggingIndex
        if (currentIndex != null) {
            val currentItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == currentIndex }
            if (currentItem != null) {
                val currentCenter = currentItem.offset + dragVisualOffset.value + (currentItem.size / 2f)

                // Advanced Center-Crossing Physics
                val targetItem = listState.layoutInfo.visibleItemsInfo
                    .filter { it.index != currentIndex }
                    .find { item ->
                        val itemCenter = item.offset + (item.size / 2f)
                        (dragVisualOffset.value > 0 && currentCenter > itemCenter && currentIndex < item.index) ||
                                (dragVisualOffset.value < 0 && currentCenter < itemCenter && currentIndex > item.index)
                    }

                if (targetItem != null) {
                    val from = currentIndex
                    val to = targetItem.index

                    // Swap instantly in local list
                    val list = localWallets.toMutableList()
                    val movedItem = list.removeAt(from)
                    list.add(to, movedItem)
                    localWallets = list

                    // Adjust visual offset perfectly so the card never jumps
                    val offsetDiff = currentItem.offset - targetItem.offset
                    coroutineScope.launch {
                        dragVisualOffset.snapTo(dragVisualOffset.value + offsetDiff)
                    }

                    draggingIndex = to
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) // Tick sound
                }
            }
        }
    }

    // Auto-scroll logic perfectly synced with the swap engine
    LaunchedEffect(isDraggingState) {
        while (isDraggingState) {
            val currentIndex = draggingIndex ?: break
            val viewportHeight = listState.layoutInfo.viewportSize.height.takeIf { it > 0 } ?: 2000

            val itemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == currentIndex }
            if (itemInfo != null) {
                // Calculate actual physical position on screen
                val cardTop = itemInfo.offset + dragVisualOffset.value
                val cardBottom = cardTop + itemInfo.size

                val topThreshold = 150f
                val bottomThreshold = viewportHeight - 150f

                var scrollDelta = 0f
                if (cardTop < topThreshold) {
                    scrollDelta = -15f
                } else if (cardBottom > bottomThreshold) {
                    scrollDelta = 15f
                }

                if (scrollDelta != 0f) {
                    val consumed = listState.scrollBy(scrollDelta)
                    if (consumed != 0f) {
                        // Lock the card to the finger while the list scrolls away underneath
                        dragVisualOffset.snapTo(dragVisualOffset.value + consumed)
                        checkAndSwap()
                    }
                }
            }
            delay(16) // ~60fps smooth scrolling
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            AnimatedVisibility(
                visible = !isDraggingState,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        viewModel.walletToEdit = null
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
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(
                    top = 130.dp, // Dynamic space for the floating header
                    bottom = 100.dp,
                    start = 24.dp,
                    end = 24.dp
                )
            ) {
                itemsIndexed(items = localWallets, key = { _, wallet -> wallet.id }) { _, wallet ->
                    // FIX 1: Always lookup dynamic index in case it was swapped
                    val dynamicIndex = localWallets.indexOfFirst { it.id == wallet.id }
                    val isDragging = dynamicIndex == draggingIndex && draggedWalletId == wallet.id

                    val theme = cardThemes.getOrElse(wallet.cardThemeId) { cardThemes[0] }
                    val brush = Brush.linearGradient(colors = listOf(theme.start, theme.end))
                    val displayNum = if (wallet.cardNumber.isNotBlank()) wallet.cardNumber.takeLast(4) else "••••"

                    // --- Premium Animations ---
                    val scale by animateFloatAsState(
                        targetValue = if (isDragging) 1.05f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "scale"
                    )

                    val elevation by animateDpAsState(
                        targetValue = if (isDragging) 24.dp else 8.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                        label = "elevation"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .animateItem()
                            .zIndex(if (isDragging) 2f else 1f)
                            .pointerInput(wallet.id) {
                                detectTapGestures(
                                    onTap = {
                                        if (!isDraggingState) {
                                            viewModel.walletToEdit = wallet
                                            onEditWallet()
                                        }
                                    }
                                )
                            }
                            .pointerInput(wallet.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        // FIX 2: Multi-touch Guard - Ignore if already dragging a card
                                        if (draggedWalletId != null) return@detectDragGesturesAfterLongPress

                                        val actualIndex = localWallets.indexOfFirst { it.id == wallet.id }
                                        if (actualIndex != -1) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            draggedWalletId = wallet.id
                                            draggingIndex = actualIndex
                                            isDraggingState = true
                                            coroutineScope.launch { dragVisualOffset.snapTo(0f) }
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        // Guard to ensure ONLY the active card handles the drag math
                                        if (draggedWalletId != wallet.id) return@detectDragGesturesAfterLongPress

                                        change.consume()
                                        coroutineScope.launch {
                                            dragVisualOffset.snapTo(dragVisualOffset.value + dragAmount.y)
                                        }
                                        checkAndSwap()
                                    },
                                    onDragEnd = {
                                        if (draggedWalletId != wallet.id) return@detectDragGesturesAfterLongPress

                                        isDraggingState = false
                                        coroutineScope.launch {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            dragVisualOffset.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                                            )
                                            draggingIndex = null
                                            draggedWalletId = null
                                            viewModel.saveWalletOrder(localWallets)
                                        }
                                    },
                                    onDragCancel = {
                                        if (draggedWalletId != wallet.id) return@detectDragGesturesAfterLongPress

                                        isDraggingState = false
                                        coroutineScope.launch {
                                            dragVisualOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy))
                                            draggingIndex = null
                                            draggedWalletId = null
                                        }
                                    }
                                )
                            }
                            .graphicsLayer {
                                if (isDragging) {
                                    translationY = dragVisualOffset.value
                                    scaleX = scale
                                    scaleY = scale
                                    rotationZ = jiggleRotation
                                }
                            },
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(brush)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.White.copy(alpha = 0.05f))
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp, vertical = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
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

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                            .clip(CircleShape)
                                            .clickable {
                                                if (!isDraggingState) {
                                                    viewModel.walletToEdit = wallet
                                                    onEditWallet()
                                                }
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

            // Frosted Custom Header Area creates a beautiful visual fade at the top of the list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(bottom = 16.dp)
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

                Text(
                    text = "Long-press and drag to reorder accounts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}