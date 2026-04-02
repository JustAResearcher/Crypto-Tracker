package com.cryptotracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.cryptotracker.ui.theme.PriceGreen
import com.cryptotracker.ui.theme.PriceRed
import com.cryptotracker.ui.theme.ChartGreenFill
import com.cryptotracker.ui.theme.ChartRedFill

@Composable
fun SparklineChart(
    prices: List<Double>,
    modifier: Modifier = Modifier,
    isPositive: Boolean = true
) {
    if (prices.isEmpty()) return

    val lineColor = if (isPositive) PriceGreen else PriceRed
    val fillColor = if (isPositive) ChartGreenFill else ChartRedFill

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val min = prices.min()
        val max = prices.max()
        val range = if (max - min == 0.0) 1.0 else max - min

        val stepX = width / (prices.size - 1).coerceAtLeast(1)

        val path = Path()
        val fillPath = Path()

        prices.forEachIndexed { index, price ->
            val x = index * stepX
            val y = height - ((price - min) / range * height).toFloat()

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        // Draw fill
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

        // Draw line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2f, cap = StrokeCap.Round)
        )
    }
}
