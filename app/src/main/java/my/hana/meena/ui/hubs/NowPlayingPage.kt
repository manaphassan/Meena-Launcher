package my.hana.meena.ui.hubs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import my.hana.meena.R
import my.hana.meena.data.system.MediaPlayerRepository
import my.hana.meena.data.system.NowPlaying
import my.hana.meena.theme.MeenaSpacing
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextWhite
import my.hana.meena.ui.components.metroClickable

/** "3:07", or "1:02:45" for long tracks. */
internal fun trackTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/**
 * Full-page now playing, in the Zune style: the album art fills the screen behind a dim layer,
 * the title is huge and runs off the edge (and drifts across when it doesn't fit), with artist,
 * album, a line to seek along and big controls underneath.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingPage(reducedMotion: Boolean, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val nowPlaying by MediaPlayerRepository.nowPlaying.collectAsState()
    val track = nowPlaying
    val background = MaterialTheme.colorScheme.background
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .clipToBounds()
    ) {
        val art = track?.art
        if (art != null) {
            Image(
                bitmap = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Same idea as the wallpaper dim: the art sets the mood, the text stays readable.
            Box(modifier = Modifier.fillMaxSize().background(background.copy(alpha = 0.8f)))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = MeenaSpacing.l, vertical = MeenaSpacing.l)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .metroClickable(onClick = onNavigateBack)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_thin_back),
                    contentDescription = null,
                    tint = MeenaTextMuted,
                    modifier = Modifier.size(MeenaSpacing.l)
                )
                Text(
                    text = stringResource(R.string.hub_inbox),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MeenaTextMuted,
                    modifier = Modifier.padding(start = MeenaSpacing.s)
                )
            }
            Text(
                text = stringResource(R.string.widget_name_music),
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                modifier = Modifier.padding(top = MeenaSpacing.m)
            )

            Spacer(modifier = Modifier.weight(1f))

            if (track == null) {
                Text(
                    text = stringResource(R.string.music_nothing),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MeenaTextMuted
                )
                Spacer(modifier = Modifier.weight(1f))
                return@Column
            }

            Text(
                text = track.title.lowercase(),
                style = MaterialTheme.typography.displayMedium,
                color = MeenaTextWhite,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = if (reducedMotion) Modifier else Modifier.basicMarquee(initialDelayMillis = 1500)
            )
            if (track.artist.isNotBlank()) {
                Text(
                    text = track.artist.lowercase(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MeenaTextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (track.album.isNotBlank()) {
                Text(
                    text = track.album.lowercase(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MeenaTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = MeenaSpacing.s)
                )
            }

            Spacer(modifier = Modifier.height(MeenaSpacing.l))
            if (track.durationMs > 0) TrackProgress(track, accent)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MeenaSpacing.l),
                horizontalArrangement = Arrangement.spacedBy(MeenaSpacing.l),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlButton(R.drawable.ic_thin_skip_prev, stringResource(R.string.now_playing_previous)) {
                    MediaPlayerRepository.previous()
                }
                ControlButton(
                    if (track.isPlaying) R.drawable.ic_thin_pause else R.drawable.ic_thin_play,
                    stringResource(if (track.isPlaying) R.string.now_playing_pause else R.string.now_playing_play)
                ) { MediaPlayerRepository.playPause() }
                ControlButton(R.drawable.ic_thin_skip_next, stringResource(R.string.now_playing_next)) {
                    MediaPlayerRepository.next()
                }
            }

            val appLabel = remember(track.packageName) {
                runCatching {
                    val pm = context.packageManager
                    pm.getApplicationLabel(pm.getApplicationInfo(track.packageName, 0)).toString()
                }.getOrNull()
            }
            if (appLabel != null) {
                Text(
                    text = stringResource(R.string.now_playing_open, appLabel.lowercase()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = accent,
                    modifier = Modifier
                        .padding(top = MeenaSpacing.m)
                        .heightIn(min = 48.dp)
                        .metroClickable { MediaPlayerRepository.openPlayer(context) }
                        .padding(vertical = MeenaSpacing.m)
                )
            }
        }
    }
}

@Composable
private fun ControlButton(icon: Int, description: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(72.dp)
            .metroClickable(onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description }
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MeenaTextWhite,
            modifier = Modifier.size(36.dp)
        )
    }
}

/** A thin line that fills as the track plays; tap anywhere on it to jump there. */
@Composable
private fun TrackProgress(track: NowPlaying, accent: androidx.compose.ui.graphics.Color) {
    var now by remember { mutableLongStateOf(android.os.SystemClock.elapsedRealtime()) }
    LaunchedEffect(track.isPlaying, track.positionAtElapsed) {
        now = android.os.SystemClock.elapsedRealtime()
        while (track.isPlaying) {
            delay(500L)
            now = android.os.SystemClock.elapsedRealtime()
        }
    }
    val position = track.positionNow(now)
    val fraction = (position.toFloat() / track.durationMs).coerceIn(0f, 1f)
    val muted = MeenaTextMuted
    val positionText = trackTime(position)
    val durationText = trackTime(track.durationMs)
    val description = stringResource(R.string.now_playing_position, positionText, durationText)

    Column(modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = description }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .then(
                    if (track.canSeek) Modifier.pointerInput(track.durationMs) {
                        detectTapGestures { offset ->
                            val at = (offset.x / size.width).coerceIn(0f, 1f)
                            MediaPlayerRepository.seekTo((at * track.durationMs).toLong())
                        }
                    } else Modifier
                )
                .drawBehind {
                    val line = 2.dp.toPx()
                    val y = size.height / 2f - line / 2f
                    drawRect(muted.copy(alpha = 0.4f), Offset(0f, y), Size(size.width, line))
                    drawRect(accent, Offset(0f, y), Size(size.width * fraction, line))
                    // A small square marks where the track is.
                    val mark = 6.dp.toPx()
                    drawRect(accent, Offset((size.width * fraction - mark / 2f).coerceIn(0f, size.width - mark), size.height / 2f - mark / 2f), Size(mark, mark))
                }
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = positionText, style = MaterialTheme.typography.labelSmall, color = muted)
            Text(text = durationText, style = MaterialTheme.typography.labelSmall, color = muted)
        }
    }
}
