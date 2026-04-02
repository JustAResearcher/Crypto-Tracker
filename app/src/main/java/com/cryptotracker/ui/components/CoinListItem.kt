package com.cryptotracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cryptotracker.domain.model.Coin
import com.cryptotracker.ui.theme.PriceGreen
import com.cryptotracker.ui.theme.PriceRed
import com.cryptotracker.ui.theme.StarYellow
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Composable
fun CoinListItem(
    coin: Coin,
    onCoinClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US).apply {
        currency = Currency.getInstance("USD")
        maximumFractionDigits = if (coin.currentPrice < 1.0) 6 else 2
    }
    val percentFormat = NumberFormat.getNumberInstance().apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCoinClick(coin.id) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Text(
                text = "${coin.marketCapRank}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp)
            )

            // Coin icon
            AsyncImage(
                model = coin.imageUrl,
                contentDescription = coin.name,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Name and symbol
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = coin.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = coin.symbol.uppercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Sparkline
            if (coin.sparkline.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(32.dp)
                ) {
                    SparklineChart(
                        prices = coin.sparkline,
                        isPositive = coin.priceChangePercentage24h >= 0
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Price and change
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = currencyFormat.format(coin.currentPrice),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                val changeColor = if (coin.priceChangePercentage24h >= 0) PriceGreen else PriceRed
                val changePrefix = if (coin.priceChangePercentage24h >= 0) "+" else ""
                Text(
                    text = "${changePrefix}${percentFormat.format(coin.priceChangePercentage24h)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = changeColor,
                    fontWeight = FontWeight.Medium
                )
            }

            // Favorite star
            IconButton(
                onClick = { onFavoriteClick(coin.id, coin.isFavorite) }
            ) {
                Icon(
                    imageVector = if (coin.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Favorite",
                    tint = if (coin.isFavorite) StarYellow else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
