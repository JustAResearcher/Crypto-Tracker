package com.cryptotracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptotracker.domain.model.ChartData
import com.cryptotracker.ui.theme.ChartLineColor
import com.cryptotracker.ui.theme.ChartFillColor
import com.cryptotracker.ui.theme.PriceGreen
import com.cryptotracker.ui.theme.PriceRed
import com.cryptotracker.ui.theme.ChartGreenFill
import com.cryptotracker.ui.theme.ChartRedFill
import java.text.NumberFormat
import java.util.Currency

@Composable
fun PriceChart(
    chartData: List<ChartData>,
    modifier: Modifier = Modifier
) {
    if (chartData.isEmpty()) return

    val prices = chartData.map { it.price }
    val first = prices.first()
    val last = prices.last()
    val isPositive = last >= first
    val lineColor = if (isPositive) PriceGreen else PriceRed
    val fillColor = if (isPositive) ChartGreenFill else ChartRedFill

    var selectedIndex by remember { mutableStateOf(-1) }
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance().apply {
            currency = Currency.getInstance("USD")
            maximumFractionDigits = 2
        }
    }

    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(modifier = modifier) {
        if (selectedIndex in prices.indices) {
            Text(
                text = currencyFormat.format(prices[selectedIndex]),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val stepX = size.width.toFloat() / (prices.size - 1).coerceAtLeast(1)
                            val index = (offset.x / stepX).toInt().coerceIn(0, prices.size - 1)
                            selectedIndex = index
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val min = prices.min()
                val max = prices.max()
                val range = if (max - min == 0.0) 1.0 else max - min
                val padding = 4f

                val stepX = width / (prices.size - 1).coerceAtLeast(1)

                val path = Path()
                val fillPath = Path()

                prices.forEachIndexed { index, price ->
                    val x = index * stepX
                    val y = (height - padding) - ((price - min) / range * (height - 2 * padding)).toFloat()

                    if (index == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }

                fillPath.lineTo(width, height)
                fillPath.lineTo(0f, height)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(fillColor, fillColor.copy(alpha = 0f)),
                        startY = 0f,
                        endY = height
                    )
                )

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )

                // Draw selection indicator
                if (selectedIndex in prices.indices) {
                    val selX = selectedIndex * stepX
                    val selPrice = prices[selectedIndex]
                    val selY = (height - padding) - ((selPrice - min) / range * (height - 2 * padding)).toFloat()

                    // Vertical line
                    drawLine(
                        color = lineColor.copy(alpha = 0.5f),
                        start = Offset(selX, 0f),
                        end = Offset(selX, height),
                        strokeWidth = 1f
                    )

                    // Dot
                    drawCircle(
                        color = lineColor,
                        radius = 6f,
                        center = Offset(selX, selY)
                    )
                    drawCircle(
                        color = surfaceColor,
                        radius = 3f,
                        center = Offset(selX, selY)
                    )
                }
            }
        }
    }
}
