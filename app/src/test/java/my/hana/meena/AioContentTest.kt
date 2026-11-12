package my.hana.meena

import my.hana.meena.data.system.AppLaunchStats
import my.hana.meena.data.system.BluetoothDevicesRepository
import my.hana.meena.data.system.ConnectedDevice
import my.hana.meena.data.system.WikipediaRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AioContentTest {

    @Test
    fun launchStats_rankByCountThenRecency_skippingGoneAndPinned() {
        val stats = mapOf(
            "a" to AppLaunchStats.Entry(5, 100),
            "b" to AppLaunchStats.Entry(9, 50),
            "c" to AppLaunchStats.Entry(5, 200),
            "gone" to AppLaunchStats.Entry(50, 300),
            "pinned" to AppLaunchStats.Entry(40, 300)
        )
        val ranked = AppLaunchStats.rank(stats, installed = setOf("a", "b", "c", "pinned"), exclude = setOf("pinned"), limit = 8)
        assertEquals(listOf("b", "c", "a"), ranked)
        assertEquals(listOf("b"), AppLaunchStats.rank(stats, setOf("a", "b", "c"), emptySet(), limit = 1))
    }

    @Test
    fun launchStats_encodeDecodeRoundTripAndRejectsJunk() {
        val entry = AppLaunchStats.Entry(3, 1_800_000_000_000L)
        assertEquals(entry, AppLaunchStats.decode(AppLaunchStats.encode(entry)))
        assertNull(AppLaunchStats.decode("not,a number"))
        assertNull(AppLaunchStats.decode(null))
    }

    @Test
    fun bluetoothMerge_dedupesByNameAndPrefersAudio() {
        val merged = BluetoothDevicesRepository.merge(
            wearables = listOf("smart band 9" to "wearable", "buds" to "wearable"),
            audio = listOf("buds" to "audio", "buds" to "audio")
        )
        assertEquals(
            listOf(ConnectedDevice("buds", "audio"), ConnectedDevice("smart band 9", "wearable")),
            merged
        )
    }

    @Test
    fun wikipediaParse_readsStandardArticle() {
        val body = """{"type":"standard","title":"Kevin Dunn","description":"American actor",
            "extract":"Kevin Dunn is an American actor.","unknown":1,
            "content_urls":{"desktop":{"page":"https://en.wikipedia.org/wiki/Kevin_Dunn"},
            "mobile":{"page":"https://en.m.wikipedia.org/wiki/Kevin_Dunn"}}}"""
        val article = WikipediaRepository.parse(body)!!
        assertEquals("Kevin Dunn", article.title)
        assertEquals("American actor", article.description)
        assertEquals("https://en.m.wikipedia.org/wiki/Kevin_Dunn", article.url)
    }

    @Test
    fun wikipediaParse_rejectsDisambiguationEmptyAndBadInput() {
        assertNull(WikipediaRepository.parse("""{"type":"disambiguation","title":"X","extract":"x","content_urls":{"mobile":{"page":"https://a"}}}"""))
        assertNull(WikipediaRepository.parse("""{"type":"standard","title":"X","extract":"","content_urls":{"mobile":{"page":"https://a"}}}"""))
        assertNull(WikipediaRepository.parse("""{"type":"standard","title":"X","extract":"x","content_urls":{"mobile":{"page":"http://a"}}}"""))
        assertNull(WikipediaRepository.parse("not json"))
    }
}
