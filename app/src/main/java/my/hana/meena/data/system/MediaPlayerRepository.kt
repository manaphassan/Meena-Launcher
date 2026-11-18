package my.hana.meena.data.system

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NowPlaying(
    val title: String,
    val artist: String,
    val album: String,
    val isPlaying: Boolean,
    val packageName: String,
    /** Album art the player shares, if any. */
    val art: androidx.compose.ui.graphics.ImageBitmap? = null,
    /** Track length in ms; 0 when the player doesn't say (live radio, some podcasts). */
    val durationMs: Long = 0L,
    /** Position in ms at [positionAtElapsed] (SystemClock.elapsedRealtime). */
    val positionMs: Long = 0L,
    val positionAtElapsed: Long = 0L,
    val speed: Float = 1f,
    val canSeek: Boolean = false
) {
    /** Where the track is now, moving on from the last report while it plays. */
    fun positionNow(nowElapsed: Long): Long {
        val moved = if (isPlaying) ((nowElapsed - positionAtElapsed) * speed).toLong() else 0L
        val at = (positionMs + moved).coerceAtLeast(0L)
        return if (durationMs > 0) at.coerceAtMost(durationMs) else at
    }
}

/**
 * Monitors active media sessions so the inbox can show a music player widget.
 * Requires notification listener access (already granted for the inbox to work).
 */
object MediaPlayerRepository {
    private const val TAG = "MediaPlayerRepo"

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private var activeController: MediaController? = null
    private var sessionListener: MediaSessionManager.OnActiveSessionsChangedListener? = null

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateFromController()
        }
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateFromController()
        }
        override fun onSessionDestroyed() {
            detach()
            _nowPlaying.value = null
        }
    }

    /** Call once when the notification listener connects. */
    fun start(context: Context) {
        try {
            val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val component = ComponentName(context, my.hana.meena.service.MeenaNotificationListenerService::class.java)
            val listener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
                pickController(controllers)
            }
            sessionListener = listener
            manager.addOnActiveSessionsChangedListener(listener, component)
            // Initial check
            pickController(manager.getActiveSessions(component))
        } catch (e: Exception) {
            Log.w(TAG, "Could not start media session listener", e)
        }
    }

    /** Call when the notification listener disconnects. */
    fun stop(context: Context) {
        try {
            val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            sessionListener?.let { manager.removeOnActiveSessionsChangedListener(it) }
        } catch (_: Exception) { }
        sessionListener = null
        detach()
        _nowPlaying.value = null
    }

    fun playPause() {
        val ctrl = activeController ?: return
        val state = ctrl.playbackState?.state
        if (state == PlaybackState.STATE_PLAYING) {
            ctrl.transportControls.pause()
        } else {
            ctrl.transportControls.play()
        }
    }

    fun next() { activeController?.transportControls?.skipToNext() }
    fun previous() { activeController?.transportControls?.skipToPrevious() }
    fun seekTo(positionMs: Long) { activeController?.transportControls?.seekTo(positionMs.coerceAtLeast(0L)) }

    /** Opens the player app at its own now playing screen when it offers one. */
    fun openPlayer(context: Context): Boolean {
        val ctrl = activeController ?: return false
        val session = ctrl.sessionActivity
        if (session != null && runCatching { session.send() }.isSuccess) return true
        val launch = context.packageManager.getLaunchIntentForPackage(ctrl.packageName) ?: return false
        return runCatching {
            context.startActivity(launch.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
    }

    private fun pickController(controllers: List<MediaController>?) {
        // Prefer the one that is currently playing, else the first one.
        val playing = controllers?.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        }
        val best = playing ?: controllers?.firstOrNull()
        if (best == null) {
            detach()
            _nowPlaying.value = null
            return
        }
        if (best != activeController) {
            detach()
            activeController = best
            best.registerCallback(controllerCallback)
        }
        updateFromController()
    }

    private fun updateFromController() {
        val ctrl = activeController ?: return
        val meta = ctrl.metadata
        val state = ctrl.playbackState
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
        val album = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty()
        if (title.isBlank() && artist.isBlank() && !isPlaying) {
            _nowPlaying.value = null
            return
        }
        _nowPlaying.value = NowPlaying(
            title = title.ifBlank { "unknown" },
            artist = artist,
            album = album,
            isPlaying = isPlaying,
            packageName = ctrl.packageName.orEmpty(),
            art = artOf(meta),
            durationMs = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0L) ?: 0L,
            positionMs = state?.position?.coerceAtLeast(0L) ?: 0L,
            positionAtElapsed = state?.lastPositionUpdateTime?.takeIf { it > 0 } ?: android.os.SystemClock.elapsedRealtime(),
            speed = state?.playbackSpeed?.takeIf { it > 0f } ?: 1f,
            canSeek = (state?.actions ?: 0L) and PlaybackState.ACTION_SEEK_TO != 0L
        )
    }

    private var lastArtSource: android.graphics.Bitmap? = null
    private var lastArt: androidx.compose.ui.graphics.ImageBitmap? = null

    /** The player's album art, kept between updates so play and pause don't copy it again. */
    private fun artOf(meta: MediaMetadata?): androidx.compose.ui.graphics.ImageBitmap? {
        val bmp = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            ?: return null.also { lastArtSource = null; lastArt = null }
        if (bmp === lastArtSource) return lastArt
        return runCatching { bmp.asImageBitmapSafe() }.getOrNull().also {
            lastArtSource = bmp
            lastArt = it
        }
    }

    private fun android.graphics.Bitmap.asImageBitmapSafe(): androidx.compose.ui.graphics.ImageBitmap {
        // Hardware bitmaps can't always be drawn as given; make a plain copy when needed.
        val usable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && config == android.graphics.Bitmap.Config.HARDWARE) {
            copy(android.graphics.Bitmap.Config.ARGB_8888, false)
        } else this
        return usable.asImageBitmap()
    }

    private fun detach() {
        runCatching { activeController?.unregisterCallback(controllerCallback) }
        activeController = null
    }
}
