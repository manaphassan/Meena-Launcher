package my.hana.meena

import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.data.news.NewsRssRepository
import my.hana.meena.data.news.NewsTitles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsSourcesTest {

    @Test
    fun googleNewsTitles_dropTheRepeatedPublisher() {
        assertEquals(
            "Tesla Tamatkan Pengeluaran Cybertruck",
            NewsTitles.stripPublisher("Tesla Tamatkan Pengeluaran Cybertruck - Amanz", "Amanz")
        )
        assertEquals("Honor 700 Hadir", NewsTitles.stripPublisher("Honor 700 Hadir – Amanz", "Amanz"))
        // A dash in the middle of a title, or no publisher, leaves it alone.
        assertEquals("GTA VI – 320GB", NewsTitles.stripPublisher("GTA VI – 320GB", "Amanz"))
        assertEquals("Plain title - Amanz", NewsTitles.stripPublisher("Plain title - Amanz", null))
        assertEquals("Amanz", NewsTitles.stripPublisher("Amanz", "Amanz"))
    }

    @Test
    fun amanz_usesTheWorkingFeed_andKeepsItsName() {
        assertEquals(MeenaUserSettings.AMANZ_FEED, NewsRssRepository.AMANZ_FEED_URL)
        assertTrue(NewsRssRepository.AMANZ_FEED_URL in NewsRssRepository.DEFAULT_FEEDS)
        assertEquals("Amanz", NewsRssRepository.sourceNameFor(NewsRssRepository.AMANZ_FEED_URL))
    }

    @Test
    fun savedOldAmanzFeed_movesToTheWorkingOne_includingItsNsfwMark() {
        val old = MeenaUserSettings(
            newsFeeds = listOf("https://amanz.my/feed/", "https://suamisihat.com.my/feed/"),
            nsfwFeeds = listOf("https://amanz.my/feed/")
        ).migrate()
        assertEquals(listOf(MeenaUserSettings.AMANZ_FEED, "https://suamisihat.com.my/feed/"), old.newsFeeds)
        assertEquals(listOf(MeenaUserSettings.AMANZ_FEED), old.nsfwFeeds)
    }

    @Test
    fun amanzHomePage_alsoMovesToTheFeed() {
        val s = MeenaUserSettings(newsFeeds = listOf("https://amanz.my", "https://cms.amanz.my/feed", MeenaUserSettings.AMANZ_FEED)).migrate()
        assertEquals(listOf(MeenaUserSettings.AMANZ_FEED), s.newsFeeds)
    }

    @Test
    fun nsfw_isOffForEveryFeedByDefault() {
        assertTrue(MeenaUserSettings().nsfwFeeds.isEmpty())
    }
}
