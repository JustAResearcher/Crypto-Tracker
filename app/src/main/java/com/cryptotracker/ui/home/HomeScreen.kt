package com.cryptotracker.ui.home

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.lazy.LazyListItemInfo
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.cryptotracker.ui.components.CoinListItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCoinClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Drag state — kept at the list level so it survives item recomposition
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var currentIndex by remember { mutableIntStateOf(-1) }
    var dragDelta by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    // Accumulated auto-scroll offset so the item tracks the finger
    var autoScrollAccum by remember { mutableFloatStateOf(0f) }

    // Auto-scroll when dragging near top/bottom edges
    LaunchedEffect(isDragging, dragDelta) {
        if (!isDragging) return@LaunchedEffect
        val viewportHeight = listState.layoutInfo.viewportEndOffset -
                listState.layoutInfo.viewportStartOffset
        val threshold = viewportHeight * 0.15f

        // Find the dragged item's current visual center
        val draggedInfo = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == currentIndex }
        if (draggedInfo != null) {
            val itemCenter = draggedInfo.offset + dragDelta + draggedInfo.size / 2f
            val scrollAmount = when {
                itemCenter < listState.layoutInfo.viewportStartOffset + threshold -> -8
                itemCenter > listState.layoutInfo.viewportEndOffset - threshold -> 8
                else -> 0
            }
            if (scrollAmount != 0) {
                while (isDragging) {
                    listState.dispatchRawDelta(scrollAmount.toFloat())
                    autoScrollAccum += scrollAmount
                    delay(8)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Crypto Tracker",
                        fontWeight = FontWeight.Bold
                    )
                },
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
            if (uiState.isLoading && uiState.coins.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            uiState.error?.let { error ->
                if (uiState.coins.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Something went wrong",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
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
                    .pointerInput(uiState.coins) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                // Find which item was long-pressed
                                listState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { info ->
                                        offset.y.toInt() in info.offset..(info.offset + info.size)
                                    }
                                    ?.let { info ->
                                        draggedIndex = info.index
                                        currentIndex = info.index
                                        dragDelta = 0f
                                        autoScrollAccum = 0f
                                        isDragging = true
                                    }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragDelta += dragAmount.y

                                // Check for swap with neighbors using overlap
                                val items = listState.layoutInfo.visibleItemsInfo
                                val draggedInfo = items.firstOrNull { it.index == currentIndex }
                                    ?: return@detectDragGesturesAfterLongPress

                                val draggedCenter =
                                    draggedInfo.offset + dragDelta + draggedInfo.size / 2f

                                // Find the item whose bounds contain the dragged center
                                val targetInfo = items.firstOrNull { info ->
                                    info.index != currentIndex &&
                                            draggedCenter >= info.offset &&
                                            draggedCenter <= info.offset + info.size
                                }

                                if (targetInfo != null) {
                                    val from = currentIndex
                                    val to = targetInfo.index
                                    viewModel.onReorder(from, to)

                                    // Adjust delta so the item stays under the finger
                                    dragDelta += (draggedInfo.offset - targetInfo.offset).toFloat()
                                    currentIndex = to
                                }
                            },
                            onDragEnd = {
                                isDragging = false
                                draggedIndex = -1
                                currentIndex = -1
                                dragDelta = 0f
                                autoScrollAccum = 0f
                            },
                            onDragCancel = {
                                isDragging = false
                                draggedIndex = -1
                                currentIndex = -1
                                dragDelta = 0f
                                autoScrollAccum = 0f
                            }
                        )
                    },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = uiState.coins,
                    key = { _, coin -> coin.id }
                ) { index, coin ->
                    val isBeingDragged = currentIndex == index && isDragging
                    val elevation by animateDpAsState(
                        targetValue = if (isBeingDragged) 8.dp else 0.dp,
                        label = "dragElevation"
                    )

                    Box(
                        modifier = Modifier
                            .zIndex(if (isBeingDragged) 1f else 0f)
                            .graphicsLayer {
                                translationY = if (isBeingDragged) dragDelta else 0f
                                scaleX = if (isBeingDragged) 1.02f else 1f
                                scaleY = if (isBeingDragged) 1.02f else 1f
                            }
                            .shadow(elevation, shape = MaterialTheme.shapes.medium)
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

            if (uiState.isLoading && uiState.coins.isNotEmpty()) {
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
