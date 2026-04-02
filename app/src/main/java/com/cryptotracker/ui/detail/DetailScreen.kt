package com.cryptotracker.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cryptotracker.ui.components.PriceChart
import com.cryptotracker.ui.theme.PriceGreen
import com.cryptotracker.ui.theme.PriceRed
import com.cryptotracker.ui.theme.StarYellow
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBackClick: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAlertDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        uiState.coin?.let { coin ->
                            AsyncImage(
                                model = coin.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(coin.name, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                coin.symbol.uppercase(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAlertDialog = true }) {
                        Icon(Icons.Default.Notifications, "Set Alert")
                    }
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (uiState.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Favorite",
                            tint = if (uiState.isFavorite) StarYellow else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val coin = uiState.coin ?: return@Scaffold

        val currencyFormat = remember {
            NumberFormat.getCurrencyInstance(Locale.US).apply {
                currency = Currency.getInstance("USD")
            }
        }
        val percentFormat = remember {
            NumberFormat.getNumberInstance().apply {
                maximumFractionDigits = 2
                minimumFractionDigits = 2
            }
        }
        val compactFormat = remember {
            NumberFormat.getCurrencyInstance(Locale.US).apply {
                currency = Currency.getInstance("USD")
                maximumFractionDigits = 0
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Price header
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    currencyFormat.maximumFractionDigits = if (coin.currentPrice < 1.0) 6 else 2
                    Text(
                        text = currencyFormat.format(coin.currentPrice),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    val changeColor = if (coin.priceChangePercentage24h >= 0) PriceGreen else PriceRed
                    val prefix = if (coin.priceChangePercentage24h >= 0) "+" else ""
                    Text(
                        text = "${prefix}${percentFormat.format(coin.priceChangePercentage24h)}% (24h)",
                        style = MaterialTheme.typography.titleMedium,
                        color = changeColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Chart period selector
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("7" to "7D", "30" to "30D", "365" to "1Y").forEach { (days, label) ->
                        FilterChip(
                            selected = uiState.selectedPeriod == days,
                            onClick = { viewModel.loadChart(days) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // Chart
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    if (uiState.isChartLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(vertical = 60.dp)
                        )
                    } else {
                        PriceChart(
                            chartData = uiState.chartData,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Stats
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        StatRow("Market Cap", compactFormat.format(coin.marketCap))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        StatRow("24h Volume", compactFormat.format(coin.totalVolume))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        currencyFormat.maximumFractionDigits = if (coin.high24h < 1.0) 6 else 2
                        StatRow("24h High", currencyFormat.format(coin.high24h))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        currencyFormat.maximumFractionDigits = if (coin.low24h < 1.0) 6 else 2
                        StatRow("24h Low", currencyFormat.format(coin.low24h))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        currencyFormat.maximumFractionDigits = if (coin.ath < 1.0) 6 else 2
                        StatRow("All-Time High", currencyFormat.format(coin.ath))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        StatRow("Circulating Supply", NumberFormat.getNumberInstance().format(coin.circulatingSupply) + " ${coin.symbol.uppercase()}")
                    }
                }
            }

            // Alerts section
            if (uiState.alerts.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Price Alerts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(uiState.alerts, key = { it.id }) { alert ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (alert.isAbove) "Above" else "Below",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (alert.isAbove) PriceGreen else PriceRed
                                )
                                currencyFormat.maximumFractionDigits = if (alert.targetPrice < 1.0) 6 else 2
                                Text(
                                    text = currencyFormat.format(alert.targetPrice),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(onClick = { viewModel.deleteAlert(alert.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete alert",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        // Alert dialog
        if (showAlertDialog) {
            PriceAlertDialog(
                currentPrice = coin.currentPrice,
                onDismiss = { showAlertDialog = false },
                onConfirm = { targetPrice, isAbove ->
                    viewModel.addAlert(targetPrice, isAbove)
                    showAlertDialog = false
                }
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PriceAlertDialog(
    currentPrice: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, Boolean) -> Unit
) {
    var targetPriceText by remember { mutableStateOf("") }
    var isAbove by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Price Alert") },
        text = {
            Column {
                Text(
                    text = "Current price: $${String.format("%.2f", currentPrice)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = targetPriceText,
                    onValueChange = { targetPriceText = it },
                    label = { Text("Target price (USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isAbove,
                        onClick = { isAbove = true },
                        label = { Text("Above") }
                    )
                    FilterChip(
                        selected = !isAbove,
                        onClick = { isAbove = false },
                        label = { Text("Below") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    targetPriceText.toDoubleOrNull()?.let { price ->
                        onConfirm(price, isAbove)
                    }
                },
                enabled = targetPriceText.toDoubleOrNull() != null
            ) {
                Text("Set Alert")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
