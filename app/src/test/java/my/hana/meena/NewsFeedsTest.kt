package my.hana.meena

import my.hana.meena.data.news.NewsRssRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NewsFeedsTest {

    @Test
    fun feeds_fallBackToDefaultsOnlyWhenEmpty() {
        assertEquals(NewsRssRepository.DEFAULT_FEEDS, NewsRssRepository.effectiveFeeds(emptyList()))
        val own = listOf("https://example.org/feed")
        assertEquals(own, NewsRssRepository.effectiveFeeds(own))
    }

    @Test
    fun sourceName_knownNamesThenHost() {
        assertEquals("Amanz", NewsRssRepository.sourceNameFor("https://amanz.my/feed/"))
        assertEquals("example", NewsRssRepository.sourceNameFor("https://www.example.org/rss?x=1"))
        assertFalse(NewsRssRepository.sourceNameFor("not a url").isBlank())
    }
}
