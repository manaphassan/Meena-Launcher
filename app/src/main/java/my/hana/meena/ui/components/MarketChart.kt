package my.hana.meena.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import my.hana.meena.data.finance.Markets
import my.hana.meena.data.finance.PricePoint

/** One line on the chart: its points and the colour its name is written in. */
data class ChartSeries(val points: List<PricePoint>, val color: Color)

/**
 * AIO-style chart: several assets on one shared scale, each drawn as its percent change since the
 * start, so a RM 16,000 coin and a RM 200 one fit together. A dotted line marks no change. Thin
 * strokes, no fill, no grid. On paper (one colour) every line after the first is dashed.
 */
@Composable
fun MarketChart(
    series: List<ChartSeries>,
    zeroLineColor: Color,
    monochrome: Boolean,
    description: String,
    modifier: Modifier = Modifier
) {
    val prepared = series.mapNotNull { line ->
        val days = line.points.map { Markets.dayNumber(it.date) }
        if (days.any { it == null } || line.points.size < 2) return@mapNotNull null
        Triple(days.filterNotNull(), Markets.relativeChange(line.points), line.color)
    }
    Canvas(modifier = modifier.semantics { contentDescription = description }) {
        if (prepared.isEmpty()) return@Canvas
        val firstDay = prepared.minOf { it.first.first() }
        val lastDay = prepared.maxOf { it.first.last() }
        val span = (lastDay - firstDay).coerceAtLeast(1L).toFloat()
        var low = prepared.minOf { it.second.min() }.coerceAtMost(0.0)
        var high = prepared.maxOf { it.second.max() }.coerceAtLeast(0.0)
        if (high - low < 2.0) {
            high += 1.0
            low -= 1.0
        }
        val stroke = 1.5.dp.toPx()
        val top = stroke
        val usable = size.height - stroke * 2
        fun y(value: Double): Float = top + (usable * (1.0 - (value - low) / (high - low))).toFloat()
        fun x(day: Long): Float = size.width * ((day - firstDay) / span)

        val zero = y(0.0)
        drawLine(
            color = zeroLineColor,
            start = Offset(0f, zero),
            end = Offset(size.width, zero),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 4.dp.toPx()))
        )

        prepared.forEachIndexed { index, (days, values, color) ->
            val path = Path()
            days.forEachIndexed { i, day ->
                val px = x(day)
                val py = y(values[i])
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            val dash = if (monochrome && index > 0) {
                PathEffect.dashPathEffect(floatArrayOf((index * 3).dp.toPx(), 3.dp.toPx()))
            } else {
                null
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = dash)
            )
        }
    }
}
