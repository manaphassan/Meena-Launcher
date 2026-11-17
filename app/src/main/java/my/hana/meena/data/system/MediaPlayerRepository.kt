package my.hana.meena.data.system

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NowPlaying(
    val title: String,
    val artist: String,
    val album: String,
    val isPlaying: Boolean,
    val packageName: String
)

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
            packageName = ctrl.packageName.orEmpty()
        )
    }

    private fun detach() {
        runCatching { activeController?.unregisterCallback(controllerCallback) }
        activeController = null
    }
}
