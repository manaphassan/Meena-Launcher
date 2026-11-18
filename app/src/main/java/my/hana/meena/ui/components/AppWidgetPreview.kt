package my.hana.meena.ui.components

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.widget.RemoteViews
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.hana.meena.data.system.AppWidgetRepository

private const val PREVIEW_MAX_HEIGHT_DP = 160

/**
 * What a widget will look like, for the "add a widget" list: the app's preview picture, or on
 * Android 12+ its live preview layout when there is no picture, or failing both the app's icon.
 * Taps fall through to the row, so tapping the preview picks the widget.
 */
@Composable
fun AppWidgetPreview(info: AppWidgetProviderInfo, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val densityDpi = context.resources.displayMetrics.densityDpi
    val usesLayout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.previewImage == 0 && info.previewLayout != 0

    if (usesLayout) {
        val density = LocalDensity.current.density
        val height = AppWidgetRepository.heightDp(info, density).coerceAtMost(PREVIEW_MAX_HEIGHT_DP)
        Box(modifier = modifier.fillMaxWidth().height(height.dp)) {
            AndroidView(
                factory = { ctx ->
                    AppWidgetHostView(ctx).apply {
                        runCatching {
                            setAppWidget(-1, info)
                            updateAppWidget(RemoteViews(info.provider.packageName, info.previewLayout))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(height.dp)
            )
            // Sits on top so the preview never takes the tap; the row underneath picks the widget.
            Box(modifier = Modifier.matchParentSize().pointerInput(Unit) {})
        }
        return
    }

    val picture by produceState<Pair<ImageBitmap, Boolean>?>(null, info) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val preview = info.loadPreviewImage(context, densityDpi)
                val drawable = preview ?: info.loadIcon(context, densityDpi)
                drawable?.let { d ->
                    val maxSide = 720
                    val w = d.intrinsicWidth.takeIf { it > 0 } ?: 192
                    val h = d.intrinsicHeight.takeIf { it > 0 } ?: 192
                    val scale = minOf(1f, maxSide.toFloat() / maxOf(w, h))
                    val bmp = Bitmap.createBitmap((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                    d.setBounds(0, 0, bmp.width, bmp.height)
                    d.draw(Canvas(bmp))
                    bmp.asImageBitmap() to (preview != null)
                }
            }.getOrNull()
        }
    }
    val loaded = picture
    if (loaded == null) {
        Box(modifier = modifier.fillMaxWidth().height(48.dp))
        return
    }
    val (image, isPreview) = loaded
    if (isPreview) {
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterStart,
            modifier = modifier.fillMaxWidth().heightIn(max = PREVIEW_MAX_HEIGHT_DP.dp)
        )
    } else {
        Image(bitmap = image, contentDescription = null, modifier = modifier.size(48.dp))
    }
}
