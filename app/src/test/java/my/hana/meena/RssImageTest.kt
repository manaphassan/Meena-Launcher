package my.hana.meena

import my.hana.meena.data.news.ArticleHtml
import my.hana.meena.data.news.NewsArticle
import my.hana.meena.data.news.NewsMerge
import my.hana.meena.data.news.NewsTime
import my.hana.meena.data.news.RssImage
import my.hana.meena.data.news.inSampleSizeFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class RssImageTest {

    @Test
    fun isImage_mediumOrImageType() {
        assertTrue(RssImage.isImage("image", null))
        assertTrue(RssImage.isImage(null, "image/jpeg"))
        assertFalse(RssImage.isImage("video", "video/mp4"))
        assertFalse(RssImage.isImage(null, null))
    }

    @Test
    fun pick_followsPriorityAndHttpsOnly() {
        val html = "<p>x</p><img src=\"https://cdn.x/body.jpg\">"
        assertEquals("https://cdn.x/t.jpg", RssImage.pick("https://cdn.x/t.jpg", "https://cdn.x/m.jpg", "https://cdn.x/e.jpg", html, null))
        assertEquals("https://cdn.x/m.jpg", RssImage.pick(null, "https://cdn.x/m.jpg", "https://cdn.x/e.jpg", html, null))
        assertEquals("https://cdn.x/e.jpg", RssImage.pick("http://cdn.x/t.jpg", null, "https://cdn.x/e.jpg", html, null))
        assertEquals("https://cdn.x/body.jpg", RssImage.pick(null, null, null, html, null))
        assertEquals("https://cdn.x/d.jpg", RssImage.pick(null, null, null, "<p>none</p>", "<img src='https://cdn.x/d.jpg'>"))
        assertNull(RssImage.pick(null, null, "http://cdn.x/e.jpg", "<img src=\"http://cdn.x/a.jpg\">", null))
    }

    @Test
    fun safeImageUrl_protocolRelativeBecomesHttps() {
        assertEquals("https://cdn.x/a.jpg", ArticleHtml.safeImageUrl("//cdn.x/a.jpg"))
        assertNull(ArticleHtml.safeImageUrl("data:image/gif;base64,R0lG"))
        assertNull(ArticleHtml.safeImageUrl("  "))
    }

    @Test
    fun inSampleSize_keepsAtLeastTargetSize() {
        assertEquals(8, inSampleSizeFor(1200, 1200, 144, 144))
        assertEquals(4, inSampleSizeFor(1600, 900, 216, 216))
        assertEquals(1, inSampleSizeFor(1080, 608, 1032, 0))
        assertEquals(2, inSampleSizeFor(2400, 1350, 1032, 0))
        assertEquals(1, inSampleSizeFor(0, 0, 100, 100))
        // a very tall picture is sampled down further to stay under the pixel cap
        assertTrue(inSampleSizeFor(1080, 20_000, 1032, 0) >= 4)
    }

    private fun article(id: String, source: String, time: Long) = NewsArticle(
        id = id, title = "t$id", link = "https://$source.example/$id", source = source,
        sourceBadgeColorHex = "#000000", pubDateMs = time, formattedDate = ""
    )

    @Test
    fun merge_newestFirstDedupedAndEverySourceKept() {
        val busy = (1..50).map { article("a$it", "Amanz", 10_000L + it) }
        val quiet = (1..10).map { article("s$it", "Suami Sihat", it.toLong()) }
        val dup = busy.first().copy(id = "copy")
        val merged = NewsMerge.merge(busy + quiet + dup, max = 40, minPerSource = 5)
        assertEquals(40, merged.size)
        assertEquals(5, merged.count { it.source == "Suami Sihat" })
        assertEquals(merged.sortedByDescending { it.pubDateMs }, merged)
        assertEquals(merged.size, merged.map { it.link }.distinct().size)
    }

    @Test
    fun limitFair_shortListUntouched() {
        val list = listOf(article("1", "A", 2), article("2", "B", 1))
        assertEquals(list, NewsMerge.limitFair(list, 20, 3))
        assertTrue(NewsMerge.limitFair(list, 0, 3).isEmpty())
    }

    @Test
    fun readerDate_dayMonthLowercase() {
        val utc = TimeZone.getTimeZone("UTC")
        val t = 1_794_493_260_000L // 12 nov 2026, 14:21 utc
        assertEquals("12 november, 2:21 pm", NewsTime.readerDate(t, t + 1_000, utc))
        assertEquals("12 november 2026, 2:21 pm", NewsTime.readerDate(t, t + 400L * 86_400_000L, utc))
        assertEquals("12 nov", NewsTime.shortDate(t, t, utc))
        assertEquals("", NewsTime.readerDate(0L, t, utc))
    }
}
