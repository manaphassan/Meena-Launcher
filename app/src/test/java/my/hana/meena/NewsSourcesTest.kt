package my.hana.meena

import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.data.news.NewsRssRepository
import my.hana.meena.data.news.NewsTitles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun amanz_usesItsOwnFeed_withGoogleNewsOnlyAsAFallback() {
        assertEquals(MeenaUserSettings.AMANZ_FEED, NewsRssRepository.AMANZ_FEED_URL)
        assertEquals("https://cms.amanz.my/feed/", NewsRssRepository.AMANZ_FEED_URL)
        assertTrue(NewsRssRepository.AMANZ_FEED_URL in NewsRssRepository.DEFAULT_FEEDS)
        assertEquals("Amanz", NewsRssRepository.sourceNameFor(NewsRssRepository.AMANZ_FEED_URL))
        assertEquals(MeenaUserSettings.AMANZ_GOOGLE_NEWS_FEED, NewsRssRepository.fallbackFor("https://cms.amanz.my/feed"))
        assertEquals(MeenaUserSettings.AMANZ_GOOGLE_NEWS_FEED, NewsRssRepository.fallbackFor("https://amanz.my/feed/"))
        assertNull(NewsRssRepository.fallbackFor("https://suamisihat.com.my/feed/"))
    }

    @Test
    fun aFeedAddressTheUserTyped_isKeptAsItIs() {
        // The bug: a typed amanz.my/feed was turned back into the Google News address on every load.
        val typed = MeenaUserSettings(
            newsFeeds = listOf("https://cms.amanz.my/feed", "https://suamisihat.com.my/feed/"),
            nsfwFeeds = listOf("https://cms.amanz.my/feed")
        ).migrate()
        assertEquals(listOf("https://cms.amanz.my/feed", "https://suamisihat.com.my/feed/"), typed.newsFeeds)
        assertEquals(listOf("https://cms.amanz.my/feed"), typed.nsfwFeeds)
        assertEquals(listOf("https://amanz.my/feed/"), MeenaUserSettings(newsFeeds = listOf("https://amanz.my/feed/")).migrate().newsFeeds)
    }

    @Test
    fun googleNewsCopyAndHomePage_moveToTheRealFeed_includingTheNsfwMark() {
        val old = MeenaUserSettings(
            newsFeeds = listOf(MeenaUserSettings.AMANZ_GOOGLE_NEWS_FEED, "https://amanz.my", "https://suamisihat.com.my/feed/"),
            nsfwFeeds = listOf(MeenaUserSettings.AMANZ_GOOGLE_NEWS_FEED)
        ).migrate()
        assertEquals(listOf(MeenaUserSettings.AMANZ_FEED, "https://suamisihat.com.my/feed/"), old.newsFeeds)
        assertEquals(listOf(MeenaUserSettings.AMANZ_FEED), old.nsfwFeeds)
    }

    @Test
    fun nsfw_isOffForEveryFeedByDefault() {
        assertTrue(MeenaUserSettings().nsfwFeeds.isEmpty())
    }
}
