package my.hana.meena

import my.hana.meena.data.system.NowPlaying
import my.hana.meena.ui.hubs.trackTime
import org.junit.Assert.assertEquals
import org.junit.Test

class NowPlayingTest {

    private fun track(isPlaying: Boolean, position: Long, at: Long, duration: Long = 200_000L, speed: Float = 1f) =
        NowPlaying("song", "artist", "album", isPlaying, "music.app",
            durationMs = duration, positionMs = position, positionAtElapsed = at, speed = speed)

    @Test
    fun trackTime_readsLikeAPlayer() {
        assertEquals("0:00", trackTime(0))
        assertEquals("0:07", trackTime(7_400))
        assertEquals("3:07", trackTime(187_000))
        assertEquals("1:02:45", trackTime(3_765_000))
        assertEquals("0:00", trackTime(-5))
    }

    @Test
    fun position_movesOnWhilePlaying() {
        assertEquals(15_000L, track(isPlaying = true, position = 10_000, at = 1_000).positionNow(6_000))
        assertEquals(20_000L, track(isPlaying = true, position = 10_000, at = 1_000, speed = 2f).positionNow(6_000))
    }

    @Test
    fun position_staysPutWhenPaused_andNeverPassesTheEnd() {
        assertEquals(10_000L, track(isPlaying = false, position = 10_000, at = 1_000).positionNow(60_000))
        assertEquals(200_000L, track(isPlaying = true, position = 199_000, at = 0).positionNow(10_000))
    }
}
