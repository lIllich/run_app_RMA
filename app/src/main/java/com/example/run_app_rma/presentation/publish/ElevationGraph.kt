package com.example.run_app_rma.presentation.publish

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun ElevationGraph(
    normalizedAlts: List<Float>,
    timestamps: List<Long>,
    startTime: Long,
    modifier: Modifier = Modifier
) {
    if (normalizedAlts.isEmpty() || timestamps.isEmpty()) {
        return
    }

    val minAlt = normalizedAlts.minOrNull() ?: 0f
    val maxAlt = normalizedAlts.maxOrNull() ?: 0f
    val elevationRange = if (maxAlt == minAlt) 1f else maxAlt - minAlt

    val useKilometers = elevationRange >= 1000
    val yUnitLabel = if (useKilometers) "km" else "m"

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        val width = size.width
        val height = size.height

        val yAxisOffset = 0f                            // left edge offset
        val yAxisLabelWidth = 80f                       // Y-axis numbers space
        val leftPadding = yAxisOffset + yAxisLabelWidth // graph padding to Y-axis
        val rightPadding = 20f
        val bottomPadding = 50f
        val topPadding = 50f

        val graphPaddingX = 20f                         // additional graph padding to Y-axis
        val graphPaddingY = 20f                         // graph padding to top/bottom

        val graphWidth = width - leftPadding - rightPadding
        val graphHeight = height - bottomPadding - topPadding

        val xStep = (graphWidth - 2 * graphPaddingX) / (normalizedAlts.size - 1).coerceAtLeast(1)

        val minY = floor((normalizedAlts.minOrNull() ?: 0f))
        val maxY = ceil((normalizedAlts.maxOrNull() ?: 0f))
        val yRange = maxY - minY

        val path = Path()
        path.moveTo(
            leftPadding + graphPaddingX,
            topPadding + graphPaddingY + (graphHeight - 2 * graphPaddingY) * (1f - (normalizedAlts.first() - minY) / yRange)
        )

        for (i in 1 until normalizedAlts.size) {
            val x = leftPadding + graphPaddingX + i * xStep
            val y = topPadding + graphPaddingY + (graphHeight - 2 * graphPaddingY) * (1f - (normalizedAlts[i] - minY) / yRange)
            path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = Color.Blue,
            style = Stroke(width = 3f)
        )

        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 34f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }

        val yLabelCount = 7
        for (i in 0 until yLabelCount) {
            val fraction = i / (yLabelCount - 1).toFloat()
            val value = maxY - fraction * yRange
            val y = topPadding + graphHeight * fraction
            val label = value.toInt().toString()

            drawContext.canvas.nativeCanvas.drawText(
                label,
                yAxisOffset,
                y + 10f,
                textPaint
            )
        }

        drawContext.canvas.nativeCanvas.drawText(
            yUnitLabel,
            yAxisOffset,
            topPadding - 40f,
            textPaint
        )

        val xLabelCount = 5
        val step = (normalizedAlts.size - 1) / (xLabelCount - 1).coerceAtLeast(1)

        for (i in 0 until xLabelCount) {
            val index = i * step
            if (index >= timestamps.size) continue

            val x = leftPadding + graphPaddingX + index * xStep
            val elapsedMillis = timestamps[index] - startTime
            val minutes = elapsedMillis / 60000
            val seconds = (elapsedMillis / 1000) % 60
            val timeLabel = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)

            drawContext.canvas.nativeCanvas.drawText(
                timeLabel,
                x - 30f,
                height - 10f,
                textPaint
            )
        }
    }
}