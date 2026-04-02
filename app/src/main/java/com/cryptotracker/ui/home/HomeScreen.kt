package com.cryptotracker.ui.home

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptotracker.domain.model.Coin
import com.cryptotracker.ui.components.CoinListItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onCoinClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Local mutable list that drives the UI during drag — no ViewModel round-trips
    val localCoins = remember { mutableStateListOf<Coin>() }
    var isDragging by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableIntStateOf(-1) }
    var dragDelta by remember { mutableFloatStateOf(0f) }
    var overscroll by remember { mutableFloatStateOf(0f) }

    // Sync from ViewModel when NOT dragging
    LaunchedEffect(uiState.coins, isDragging) {
        if (!isDragging) {
            localCoins.clear()
            localCoins.addAll(uiState.coins)
        }
    }

    // Continuous auto-scroll while dragging near edges
    LaunchedEffect(isDragging) {
        if (!isDragging) return@LaunchedEffect
        while (isActive && isDragging) {
            val viewport = listState.layoutInfo
            val viewportHeight = (viewport.viewportEndOffset - viewport.viewportStartOffset).toFloat()
            if (viewportHeight <= 0f) { delay(16); continue }

            val draggedInfo = viewport.visibleItemsInfo.firstOrNull { it.index == currentIndex }
            if (draggedInfo != null) {
                val itemCenter = draggedInfo.offset + dragDelta + draggedInfo.size / 2f
                val threshold = viewportHeight * 0.20f // 20% zone at each edge
                val topEdge = viewport.viewportStartOffset + threshold
                val bottomEdge = viewport.viewportEndOffset - threshold

                // How deep into the zone (0 = just entered, 1 = at the very edge)
                val fraction = when {
                    itemCenter < topEdge -> 1f - (itemCenter - viewport.viewportStartOffset) / threshold
                    itemCenter > bottomEdge -> 1f - (viewport.viewportEndOffset - itemCenter) / threshold
                    else -> 0f
                }.coerceIn(0f, 1f)

                if (fraction > 0f) {
                    // Accelerating curve: deeper into zone = much faster scroll
                    val maxSpeed = 50f
                    val speed = fraction * fraction * maxSpeed
                    val direction = if (itemCenter < topEdge) -1f else 1f
                    val consumed = listState.dispatchRawDelta(direction * speed)
                    overscroll += consumed
                }
            }
            delay(16) // ~60fps
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crypto Tracker", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.loadMarkets() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && localCoins.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            uiState.error?.let { error ->
                if (localCoins.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Something went wrong", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                listState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { info ->
                                        offset.y.toInt() in info.offset..(info.offset + info.size)
                                    }
                                    ?.let { info ->
                                        currentIndex = info.index
                                        dragDelta = 0f
                                        overscroll = 0f
                                        isDragging = true
                                    }
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragDelta += amount.y

                                // Check overlap with neighbors for swap
                                val items = listState.layoutInfo.visibleItemsInfo
                                val draggedInfo = items.firstOrNull { it.index == currentIndex }
                                    ?: return@detectDragGesturesAfterLongPress
                                val draggedCenter =
                                    draggedInfo.offset + dragDelta + draggedInfo.size / 2f

                                val target = items.firstOrNull { info ->
                                    info.index != currentIndex &&
                                            draggedCenter >= info.offset &&
                                            draggedCenter <= info.offset + info.size
                                }
                                if (target != null) {
                                    val from = currentIndex
                                    val to = target.index
                                    // Swap in the LOCAL list only — zero I/O
                                    val item = localCoins.removeAt(from)
                                    localCoins.add(to, item)
                                    // Adjust so the item stays under the finger
                                    dragDelta += (draggedInfo.offset - target.offset)
                                    currentIndex = to
                                }
                            },
                            onDragEnd = {
                                // Persist final order once on drop
                                viewModel.commitOrder(localCoins.map { it.id })
                                isDragging = false
                                currentIndex = -1
                                dragDelta = 0f
                                overscroll = 0f
                            },
                            onDragCancel = {
                                isDragging = false
                                currentIndex = -1
                                dragDelta = 0f
                                overscroll = 0f
                            }
                        )
                    },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = localCoins,
                    key = { _, coin -> coin.id }
                ) { index, coin ->
                    val isBeingDragged = currentIndex == index && isDragging

                    Box(
                        modifier = Modifier
                            .zIndex(if (isBeingDragged) 1f else 0f)
                            .graphicsLayer {
                                translationY = if (isBeingDragged) dragDelta else 0f
                                scaleX = if (isBeingDragged) 1.03f else 1f
                                scaleY = if (isBeingDragged) 1.03f else 1f
                                shadowElevation = if (isBeingDragged) 12f else 0f
                                alpha = if (isBeingDragged) 0.92f else 1f
                            }
                            .then(
                                if (isBeingDragged)
                                    Modifier.shadow(8.dp, MaterialTheme.shapes.medium)
                                else Modifier
                            )
                            .animateItemPlacement()
                    ) {
                        CoinListItem(
                            coin = coin,
                            onCoinClick = { if (!isDragging) onCoinClick(it) },
                            onFavoriteClick = { id, isFav ->
                                if (!isDragging) viewModel.toggleFavorite(id, isFav)
                            }
                        )
                    }
                }
            }

            if (uiState.isLoading && localCoins.isNotEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
