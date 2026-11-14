package my.hana.meena.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Three bars, thin outlines, the first [level] filled (1 weak to 3 strong). Square corners, one
 * colour. Decorative: the words next to it carry the meaning for screen readers.
 */
@Composable
fun SignalBars(level: Int, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 16.dp, height = 14.dp)) {
        val stroke = 1.dp.toPx()
        val gap = 2.dp.toPx()
        val barWidth = (size.width - gap * 2) / 3f
        for (i in 0 until 3) {
            val barHeight = size.height * (i + 1) / 3f
            val left = i * (barWidth + gap)
            val top = size.height - barHeight
            if (i < level) {
                drawRect(color, topLeft = Offset(left, top), size = Size(barWidth, barHeight))
            } else {
                drawRect(
                    color,
                    topLeft = Offset(left + stroke / 2, top + stroke / 2),
                    size = Size(barWidth - stroke, barHeight - stroke),
                    style = Stroke(width = stroke)
                )
            }
        }
    }
}
