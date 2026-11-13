package my.hana.meena

import my.hana.meena.data.news.ArticleBlock
import my.hana.meena.data.news.ArticleHtml
import my.hana.meena.data.news.InlineSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleHtmlTest {

    @Test
    fun decodeEntities_numericNamedAndUnknown() {
        assertEquals("Nvidia\u2019s & AMD", ArticleHtml.decodeEntities("Nvidia&#8217;s &amp; AMD"))
        assertEquals("a\u2014b", ArticleHtml.decodeEntities("a&#x2014;b"))
        assertEquals("\u201Chi\u201D \u2026 <x>", ArticleHtml.decodeEntities("&ldquo;hi&rdquo; &hellip; &lt;x&gt;"))
        assertEquals("&", ArticleHtml.decodeEntities("&#038;"))
        assertEquals("&bogus; stays", ArticleHtml.decodeEntities("&bogus; stays"))
    }

    @Test
    fun plainText_stripsTagsScriptsAndCollapsesSpace() {
        val html = "<p>Hello&nbsp;<b>world</b></p><script>alert(1)</script>\n<p>again [&#8230;]</p>"
        assertEquals("Hello world again ...", ArticleHtml.plainText(html))
    }

    @Test
    fun parse_paragraphsWithInlineStyles() {
        val blocks = ArticleHtml.parse(
            "<p>Read <strong>this</strong> and <a href=\"https://amanz.my/x\">that</a> <em>now</em>.</p>"
        )
        assertEquals(1, blocks.size)
        val p = blocks[0] as ArticleBlock.Paragraph
        assertEquals("Read this and that now.", p.text.text)
        val bold = p.text.spans.single { it.kind == InlineSpan.Kind.BOLD }
        assertEquals("this", p.text.text.substring(bold.start, bold.end))
        val link = p.text.spans.single { it.kind == InlineSpan.Kind.LINK }
        assertEquals("that", p.text.text.substring(link.start, link.end))
        assertEquals("https://amanz.my/x", link.url)
        val italic = p.text.spans.single { it.kind == InlineSpan.Kind.ITALIC }
        assertEquals("now", p.text.text.substring(italic.start, italic.end))
    }

    @Test
    fun parse_unsafeLinksBecomePlainText() {
        val p = ArticleHtml.parse("<p><a href=\"javascript:alert(1)\">x</a> <a href=\"/rel\">y</a></p>")
            .single() as ArticleBlock.Paragraph
        assertEquals("x y", p.text.text)
        assertTrue(p.text.spans.isEmpty())
    }

    @Test
    fun parse_headingsListsQuotes() {
        val blocks = ArticleHtml.parse(
            "<h2>Title</h2><ul><li>one</li><li>two</li></ul><ol><li>first</li><li>second</li></ol>" +
                "<blockquote><p>quoted</p></blockquote>"
        )
        assertEquals(ArticleBlock.Heading::class, blocks[0]::class)
        assertEquals(2, (blocks[0] as ArticleBlock.Heading).level)
        assertEquals("one", (blocks[1] as ArticleBlock.ListItem).text.text)
        assertNull((blocks[1] as ArticleBlock.ListItem).number)
        assertEquals("two", (blocks[2] as ArticleBlock.ListItem).text.text)
        assertEquals(1, (blocks[3] as ArticleBlock.ListItem).number)
        assertEquals(2, (blocks[4] as ArticleBlock.ListItem).number)
        assertEquals("quoted", (blocks[5] as ArticleBlock.Quote).text.text)
    }

    @Test
    fun parse_figureImagePicksSrcAndCaption() {
        val html = "<figure class=\"wp-block-image\"><img decoding=\"async\" width=\"1024\" height=\"576\" " +
            "src=\"https://cdn.amanz.my/a-1024x576.jpg?x=1&amp;y=2\" " +
            "srcset=\"https://cdn.amanz.my/a-300x169.jpg 300w, https://cdn.amanz.my/a.jpg 1200w\" alt=\"\" />" +
            "<figcaption>Photo: <em>Nvidia</em></figcaption></figure><p>Body</p>"
        val blocks = ArticleHtml.parse(html)
        val image = blocks[0] as ArticleBlock.Image
        assertEquals("https://cdn.amanz.my/a-1024x576.jpg?x=1&y=2", image.src)
        assertEquals("Photo: Nvidia", image.caption)
        assertEquals("Body", (blocks[1] as ArticleBlock.Paragraph).text.text)
    }

    @Test
    fun parse_skipsIframesScriptsHttpImagesAndPixels() {
        val html = "<p>a</p><iframe src=\"https://youtube.com/embed/x\"></iframe>" +
            "<style>p{color:red}</style><img src=\"http://plain.example/a.jpg\">" +
            "<img src=\"https://t.example/p.gif\" width=\"1\" height=\"1\"><p>b</p>"
        val blocks = ArticleHtml.parse(html)
        assertEquals(listOf("a", "b"), blocks.map { (it as ArticleBlock.Paragraph).text.text })
    }

    @Test
    fun parse_brBecomesLineBreakAndBareTextIsParagraph() {
        val p = ArticleHtml.parse("line one<br/>line two").single() as ArticleBlock.Paragraph
        assertEquals("line one\nline two", p.text.text)
    }

    @Test
    fun parse_brokenMarkupNeverThrows() {
        ArticleHtml.parse("<p>open <b>bold <a href='https://x.y'>link")
        ArticleHtml.parse("<<<>>> <img src=\"https://x.y/a.jpg\" alt=\"it's\">")
        assertTrue(ArticleHtml.parse("").isEmpty())
    }

    @Test
    fun sameImage_ignoresSizeVariantsAndQueries() {
        assertTrue(ArticleHtml.sameImage("https://cdn.x/a-1024x576.jpg?w=1", "https://cdn.x/a.jpg"))
        assertTrue(ArticleHtml.sameImage("https://cdn.x/a-scaled.jpg", "http://cdn.x/a.jpg"))
        assertFalse(ArticleHtml.sameImage("https://cdn.x/a.jpg", "https://cdn.x/b.jpg"))
        assertFalse(ArticleHtml.sameImage(null, "https://cdn.x/b.jpg"))
    }

    @Test
    fun readerBody_dropsHeroDuplicateAndKeepsItsCaption() {
        val html = "<figure><img src=\"https://cdn.x/hero-1024x576.jpg\"><figcaption>Cap</figcaption></figure>" +
            "<p>Text</p><img src=\"https://cdn.x/other.jpg\">"
        val body = ArticleHtml.readerBody(html, "desc", "https://cdn.x/hero.jpg")
        assertEquals("https://cdn.x/hero.jpg", body.heroUrl)
        assertEquals("Cap", body.heroCaption)
        assertEquals(2, body.blocks.size)
        assertEquals("https://cdn.x/other.jpg", (body.blocks[1] as ArticleBlock.Image).src)
    }

    @Test
    fun readerBody_fallsBackToDescriptionAndFirstImage() {
        val noContent = ArticleHtml.readerBody("", "Short summary", null)
        assertNull(noContent.heroUrl)
        assertEquals("Short summary", (noContent.blocks.single() as ArticleBlock.Paragraph).text.text)

        val firstImage = ArticleHtml.readerBody("<p>a</p><img src=\"https://cdn.x/1.jpg\">", "", null)
        assertEquals("https://cdn.x/1.jpg", firstImage.heroUrl)
        assertEquals(1, firstImage.blocks.size)
    }
}
