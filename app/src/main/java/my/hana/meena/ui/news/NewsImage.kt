package my.hana.meena.ui.news

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import my.hana.meena.R
import my.hana.meena.data.news.NewsArticle
import my.hana.meena.data.news.NewsImageLoader
import my.hana.meena.data.news.NewsTime
import my.hana.meena.ui.components.MetroMessage
import my.hana.meena.ui.widgets.AgeUnit
import my.hana.meena.ui.widgets.Freshness

sealed interface NewsImageState {
    data object Loading : NewsImageState
    data class Ready(val image: ImageBitmap, val aspectRatio: Float) : NewsImageState
    data object Failed : NewsImageState
}

private fun Bitmap.toReady(): NewsImageState.Ready =
    NewsImageState.Ready(asImageBitmap(), if (height > 0) width.toFloat() / height else 1f)

/** Loads [url] sized for [targetPx]; starts as Ready when the picture is already in memory. */
@Composable
fun rememberNewsImage(url: String?, targetPx: Int, fillSquare: Boolean): NewsImageState {
    val context = LocalContext.current.applicationContext
    val state = remember(url, targetPx, fillSquare) {
        val cached = url?.let { NewsImageLoader.cached(it, targetPx, fillSquare) }
        mutableStateOf(
            when {
                url == null -> NewsImageState.Failed
                cached != null -> cached.toReady()
                else -> NewsImageState.Loading
            }
        )
    }
    LaunchedEffect(url, targetPx, fillSquare) {
        if (url != null && state.value is NewsImageState.Loading) {
            val bitmap = NewsImageLoader.load(context, url, targetPx, fillSquare)
            state.value = bitmap?.toReady() ?: NewsImageState.Failed
        }
    }
    return state.value
}

/**
 * A news picture. Shows nothing while loading or when it can't load.
 * With [fitWidth] it fills the width at its own aspect ratio; otherwise it crops into [modifier]'s size.
 */
/**
 * A heavily blurred copy for pictures from feeds marked nsfw: shrunk to a few pixels, then smoothed
 * when drawn. Works on every Android version, unlike Modifier.blur (Android 12 and later only).
 */
fun blurredCopy(image: ImageBitmap): ImageBitmap {
    val src = image.asAndroidBitmap()
    val w = (src.width / BLUR_FACTOR).coerceAtLeast(2)
    val h = (src.height / BLUR_FACTOR).coerceAtLeast(2)
    return src.scale(w, h).asImageBitmap()
}

private const val BLUR_FACTOR = 40

@Composable
fun NewsImage(
    url: String?,
    modifier: Modifier = Modifier,
    targetPx: Int,
    fitWidth: Boolean = false,
    blurred: Boolean = false
) {
    val state = rememberNewsImage(url, targetPx, fillSquare = !fitWidth)
    if (state is NewsImageState.Ready) {
        val shown = remember(state.image, blurred) { if (blurred) blurredCopy(state.image) else state.image }
        Image(
            bitmap = shown,
            filterQuality = androidx.compose.ui.graphics.FilterQuality.Low,
            contentDescription = null,
            modifier = if (fitWidth) {
                modifier.fillMaxWidth().aspectRatio(state.aspectRatio.coerceAtLeast(0.2f))
            } else {
                modifier
            },
            contentScale = if (fitWidth) ContentScale.FillWidth else ContentScale.Crop
        )
    }
}

/** "8h ago", "yesterday", "12 nov"; empty when the article has no date. */
@Composable
fun newsAgeText(pubDateMs: Long, nowMs: Long): String {
    if (pubDateMs <= 0L) return ""
    val age = Freshness.ageOf(pubDateMs, nowMs)
    return when (age.unit) {
        AgeUnit.JUST_NOW -> stringResource(R.string.news_age_just_now)
        AgeUnit.MINUTES -> stringResource(R.string.news_age_minutes, age.amount)
        AgeUnit.HOURS -> stringResource(R.string.news_age_hours, age.amount)
        AgeUnit.DAYS -> when {
            age.amount == 1L -> stringResource(R.string.news_age_yesterday)
            age.amount < 7L -> stringResource(R.string.news_age_days, age.amount)
            else -> NewsTime.shortDate(pubDateMs, nowMs)
        }
    }
}

/** Host shown in "open on ...", without "www."; null when the link has none. */
fun linkHost(link: String): String? =
    runCatching { Uri.parse(link).host }.getOrNull()
        ?.lowercase()
        ?.removePrefix("www.")
        ?.ifBlank { null }

/** Opens [url] in the user's browser; shows [failedMessage] when nothing can open it. */
fun openInBrowser(context: Context, url: String, failedMessage: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        MetroMessage.show(failedMessage)
    }
}

/** "amanz · nvidia · 8h ago" parts for a list row, lowercase. */
fun rowMetaParts(article: NewsArticle, age: String): List<String> =
    listOf(article.source.lowercase(), article.category.lowercase(), age).filter { it.isNotBlank() }
