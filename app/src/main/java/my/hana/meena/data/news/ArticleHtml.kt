package my.hana.meena.data.news

/** An inline style over `[start, end)` of [RichText.text]. */
data class InlineSpan(val start: Int, val end: Int, val kind: Kind, val url: String? = null) {
    enum class Kind { BOLD, ITALIC, LINK }
}

/** Plain text plus inline styles; turned into an AnnotatedString by the UI. */
data class RichText(val text: String, val spans: List<InlineSpan> = emptyList())

/** One readable block of an article body. */
sealed class ArticleBlock {
    data class Paragraph(val text: RichText) : ArticleBlock()
    data class Heading(val level: Int, val text: RichText) : ArticleBlock()
    /** [number] is set for ordered lists, null for bullets. */
    data class ListItem(val text: RichText, val number: Int? = null) : ArticleBlock()
    data class Quote(val text: RichText) : ArticleBlock()
    data class Image(val src: String, val caption: String? = null) : ArticleBlock()
}

/** What the reader shows below the title: a hero image, its caption and the body. */
data class ReaderBody(
    val heroUrl: String?,
    val heroCaption: String?,
    val blocks: List<ArticleBlock>
)

/**
 * Small, forgiving HTML reader for feed content (content:encoded / description).
 * Pure Kotlin so it runs in JVM unit tests; it does not use android.text.Html.
 */
object ArticleHtml {

    private val NAMED_ENTITIES = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'",
        "nbsp" to "\u00A0", "hellip" to "\u2026",
        "rsquo" to "\u2019", "lsquo" to "\u2018", "rdquo" to "\u201D", "ldquo" to "\u201C",
        "ndash" to "\u2013", "mdash" to "\u2014"
    )

    private val ENTITY = Regex("&(#[0-9]{1,7}|#[xX][0-9a-fA-F]{1,6}|[a-zA-Z][a-zA-Z0-9]{1,31});")

    private val DROPPED_ELEMENTS = Regex(
        "<(script|style|iframe|noscript|svg|video|audio|object|template|form|button|select)\\b[^>]*>.*?</\\1\\s*>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val LONE_DROPPED_TAGS = Regex(
        "<(iframe|embed|source|track|input)\\b[^>]*/?>",
        RegexOption.IGNORE_CASE
    )
    private val COMMENT = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
    private val ATTR = Regex("([^\\s\"'>/=]+)(?:\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s\"'>]+)))?")
    private val BREAKING_TAG = Regex("<(?:br|/?p|/?div|/?li|/?h[1-6]|/?blockquote|/?figcaption)\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val ANY_TAG = Regex("<[^>]*>")
    private val WHITESPACE = Regex("[\\s\\u00A0]+")
    private val SIZE_SUFFIX = Regex("-\\d+x\\d+(?=\\.[A-Za-z0-9]+$)")
    private val SCALED_SUFFIX = Regex("-scaled(?=\\.[A-Za-z0-9]+$)")

    /** Decodes numeric (decimal and hex) and the common named entities. Unknown ones are left as they are. */
    fun decodeEntities(input: String): String {
        if (input.indexOf('&') < 0) return input
        return ENTITY.replace(input) { m ->
            val body = m.groupValues[1]
            when {
                body.startsWith("#x") || body.startsWith("#X") -> fromCodePoint(body.substring(2).toIntOrNull(16)) ?: m.value
                body.startsWith("#") -> fromCodePoint(body.substring(1).toIntOrNull()) ?: m.value
                else -> NAMED_ENTITIES[body] ?: m.value
            }
        }
    }

    private fun fromCodePoint(cp: Int?): String? {
        if (cp == null || cp <= 0 || cp > 0x10FFFF || cp in 0xD800..0xDFFF) return null
        return String(Character.toChars(cp))
    }

    /** Tags removed, entities decoded, whitespace collapsed. Used for titles, categories and descriptions. */
    fun plainText(html: String): String {
        val stripped = stripUnwanted(html)
            .replace(BREAKING_TAG, " ")
            .replace(ANY_TAG, "")
        return decodeEntities(stripped)
            .replace(WHITESPACE, " ")
            .replace("[\u2026]", "...")
            .replace("[...]", "...")
            .trim()
    }

    private fun stripUnwanted(html: String): String =
        html.replace(COMMENT, "")
            .replace(DROPPED_ELEMENTS, "")
            .replace(LONE_DROPPED_TAGS, "")

    /** An https image address, or null. Protocol-relative addresses become https. */
    fun safeImageUrl(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        val url = (if (trimmed.startsWith("//")) "https:$trimmed" else trimmed).replace(" ", "%20")
        if (!url.startsWith("https://", ignoreCase = true) || url.length <= "https://".length) return null
        if (url.any { it.isWhitespace() }) return null
        return url
    }

    /** An http(s) link the browser can open, or null for anything else (mailto, javascript, relative). */
    fun safeLinkUrl(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        val url = if (trimmed.startsWith("//")) "https:$trimmed" else trimmed
        val ok = url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)
        return if (ok && url.none { it.isWhitespace() }) url else null
    }

    /** True when two addresses point at the same picture, ignoring query strings and WordPress size variants. */
    fun sameImage(a: String?, b: String?): Boolean {
        if (a.isNullOrBlank() || b.isNullOrBlank()) return false
        return imageKey(a) == imageKey(b)
    }

    private fun imageKey(url: String): String =
        url.substringAfter("://")
            .substringBefore('#')
            .substringBefore('?')
            .replace(SIZE_SUFFIX, "")
            .replace(SCALED_SUFFIX, "")
            .lowercase()

    /** The first usable https `<img src>` in [html], or null. */
    fun firstImageSrc(html: String): String? {
        if (html.isBlank()) return null
        for (token in tokenize(stripUnwanted(html))) {
            if (token is Token.Start && token.name == "img") {
                imageSrc(token.attrs)?.let { return it }
            }
        }
        return null
    }

    internal fun imageSrc(attrs: Map<String, String>): String? {
        val w = attrs["width"]?.trim()?.toIntOrNull()
        val h = attrs["height"]?.trim()?.toIntOrNull()
        if ((w != null && w <= 2) || (h != null && h <= 2)) return null // tracking pixels
        return listOf(attrs["src"], attrs["data-src"], attrs["data-lazy-src"])
            .firstNotNullOfOrNull { safeImageUrl(it) }
    }

    /** Splits [html] into readable blocks. Never throws; odd markup degrades to plain paragraphs. */
    fun parse(html: String): List<ArticleBlock> {
        if (html.isBlank()) return emptyList()
        val builder = BlockBuilder()
        for (token in tokenize(stripUnwanted(html))) builder.accept(token)
        builder.finish()
        return builder.blocks
    }

    /**
     * Body for the reader. The hero is [heroUrl] (from the feed) or else the first image in the body.
     * Body images that repeat the hero are dropped; the first one's caption moves under the hero.
     * With no usable HTML, [fallbackText] becomes a single paragraph.
     */
    fun readerBody(contentHtml: String, fallbackText: String, heroUrl: String?): ReaderBody {
        val parsed = parse(contentHtml).ifEmpty {
            if (fallbackText.isBlank()) emptyList() else listOf(ArticleBlock.Paragraph(RichText(fallbackText.trim())))
        }
        val hero = heroUrl ?: parsed.firstNotNullOfOrNull { (it as? ArticleBlock.Image)?.src }
        var heroCaption: String? = null
        val blocks = parsed.filter { block ->
            if (block is ArticleBlock.Image && sameImage(block.src, hero)) {
                if (heroCaption == null) heroCaption = block.caption
                false
            } else {
                true
            }
        }
        return ReaderBody(hero, heroCaption, blocks)
    }

    // ---- tokenizer ----

    private sealed class Token {
        data class Text(val raw: String) : Token()
        data class Start(val name: String, val attrs: Map<String, String>) : Token()
        data class End(val name: String) : Token()
    }

    private fun tokenize(html: String): List<Token> {
        val out = ArrayList<Token>()
        val text = StringBuilder()
        var i = 0
        val n = html.length
        while (i < n) {
            val c = html[i]
            val next = if (i + 1 < n) html[i + 1] else ' '
            if (c == '<' && (next.isLetter() || next == '/' || next == '!')) {
                var close = findTagEnd(html, i + 1)
                if (close < 0) close = html.indexOf('>', i + 1)
                if (close < 0) {
                    text.append(html, i, n)
                    break
                }
                if (text.isNotEmpty()) {
                    out += Token.Text(text.toString())
                    text.setLength(0)
                }
                parseTag(html.substring(i + 1, close))?.let { out += it }
                i = close + 1
            } else {
                text.append(c)
                i++
            }
        }
        if (text.isNotEmpty()) out += Token.Text(text.toString())
        return out
    }

    /** Index of the `>` that ends a tag, skipping `>` inside quoted attribute values. */
    private fun findTagEnd(s: String, from: Int): Int {
        var quote = '\u0000'
        var lastNonSpace = '\u0000'
        var i = from
        while (i < s.length) {
            val c = s[i]
            if (quote != '\u0000') {
                if (c == quote) quote = '\u0000'
            } else if ((c == '"' || c == '\'') && lastNonSpace == '=') {
                quote = c
            } else if (c == '>') {
                return i
            }
            if (!c.isWhitespace()) lastNonSpace = c
            i++
        }
        return -1
    }

    private fun parseTag(inner: String): Token? {
        if (inner.startsWith("!") || inner.startsWith("?")) return null
        if (inner.startsWith("/")) {
            val name = inner.substring(1).trim().takeWhile { it.isLetterOrDigit() }.lowercase()
            return if (name.isEmpty()) null else Token.End(name)
        }
        val name = inner.takeWhile { it.isLetterOrDigit() || it == '-' || it == ':' }.lowercase()
        if (name.isEmpty()) return null
        val attrs = HashMap<String, String>()
        for (m in ATTR.findAll(inner.substring(name.length))) {
            val key = m.groupValues[1].lowercase()
            val value = m.groups[2]?.value ?: m.groups[3]?.value ?: m.groups[4]?.value ?: ""
            if (key !in attrs) attrs[key] = decodeEntities(value)
        }
        return Token.Start(name, attrs)
    }

    // ---- block builder ----

    private class OpenSpan(val tag: String, val kind: InlineSpan.Kind, val url: String?, var start: Int)
    private class ListContext(val ordered: Boolean, var counter: Int)

    private class BlockBuilder {
        val blocks = ArrayList<ArticleBlock>()
        private val text = StringBuilder()
        private val spans = ArrayList<InlineSpan>()
        private val open = ArrayList<OpenSpan>()
        private val lists = ArrayList<ListContext>()
        private var headingLevel = 0
        private var quoteDepth = 0
        private var listItemDepth = 0
        private var listNumber: Int? = null
        private var figureDepth = 0
        private var figureImageIndex = -1
        private var inCaption = false
        private val caption = StringBuilder()

        fun accept(token: Token) {
            when (token) {
                is Token.Text -> {
                    val decoded = decodeEntities(token.raw)
                    if (inCaption) appendCollapsed(caption, decoded) else appendCollapsed(text, decoded)
                }
                is Token.Start -> start(token.name, token.attrs)
                is Token.End -> end(token.name)
            }
        }

        private fun start(name: String, attrs: Map<String, String>) {
            when (name) {
                "br" -> if (inCaption) appendCollapsed(caption, " ") else lineBreak()
                "p", "div", "section", "article", "header", "footer", "main", "aside",
                "table", "tr", "pre", "dl", "dt", "dd", "center", "hr" -> flush()
                "h1", "h2", "h3", "h4", "h5", "h6" -> {
                    flush()
                    headingLevel = name[1] - '0'
                }
                "ul", "ol" -> {
                    flush()
                    val first = attrs["start"]?.trim()?.toIntOrNull() ?: 1
                    lists += ListContext(ordered = name == "ol", counter = first - 1)
                }
                "li" -> {
                    flush()
                    listItemDepth++
                    val ctx = lists.lastOrNull()
                    listNumber = if (ctx != null && ctx.ordered) ++ctx.counter else null
                }
                "blockquote" -> {
                    flush()
                    quoteDepth++
                }
                "figure" -> {
                    flush()
                    figureDepth++
                    figureImageIndex = -1
                }
                "figcaption" -> {
                    flush()
                    inCaption = true
                    caption.setLength(0)
                }
                "img" -> imageSrc(attrs)?.let { src ->
                    flush()
                    blocks += ArticleBlock.Image(src)
                    if (figureDepth > 0) figureImageIndex = blocks.lastIndex
                }
                "td", "th" -> appendCollapsed(text, " ")
                "strong", "b" -> open += OpenSpan(name, InlineSpan.Kind.BOLD, null, text.length)
                "em", "i" -> open += OpenSpan(name, InlineSpan.Kind.ITALIC, null, text.length)
                "a" -> safeLinkUrl(attrs["href"])?.let { url ->
                    open += OpenSpan(name, InlineSpan.Kind.LINK, url, text.length)
                }
            }
        }

        private fun end(name: String) {
            when (name) {
                "p", "div", "section", "article", "header", "footer", "main", "aside",
                "table", "tr", "pre", "dl", "dt", "dd", "center" -> flush()
                "h1", "h2", "h3", "h4", "h5", "h6" -> {
                    flush()
                    headingLevel = 0
                }
                "li" -> {
                    flush()
                    listItemDepth = (listItemDepth - 1).coerceAtLeast(0)
                    listNumber = null
                }
                "ul", "ol" -> {
                    flush()
                    if (lists.isNotEmpty()) lists.removeAt(lists.lastIndex)
                }
                "blockquote" -> {
                    flush()
                    quoteDepth = (quoteDepth - 1).coerceAtLeast(0)
                }
                "figcaption" -> {
                    inCaption = false
                    val cap = caption.toString().trim()
                    caption.setLength(0)
                    if (cap.isNotEmpty()) {
                        val image = blocks.getOrNull(figureImageIndex) as? ArticleBlock.Image
                        if (image != null) {
                            blocks[figureImageIndex] = image.copy(caption = cap)
                        } else {
                            blocks += ArticleBlock.Paragraph(RichText(cap))
                        }
                    }
                }
                "figure" -> {
                    flush()
                    figureDepth = (figureDepth - 1).coerceAtLeast(0)
                    figureImageIndex = -1
                }
                "strong", "b", "em", "i", "a" -> {
                    val index = open.indexOfLast { it.tag == name }
                    if (index >= 0) {
                        val span = open.removeAt(index)
                        if (span.start < text.length) spans += InlineSpan(span.start, text.length, span.kind, span.url)
                    }
                }
            }
        }

        private fun lineBreak() {
            trimTrailing(text)
            if (text.isNotEmpty()) text.append('\n')
        }

        fun finish() {
            if (inCaption) end("figcaption")
            flush()
        }

        private fun flush() {
            trimTrailing(text)
            for (o in open) {
                if (o.start < text.length) spans += InlineSpan(o.start, text.length, o.kind, o.url)
            }
            val value = text.toString()
            if (value.isNotBlank()) {
                val clamped = spans.mapNotNull { s ->
                    val e = minOf(s.end, value.length)
                    if (s.start < e) s.copy(end = e) else null
                }
                val rich = RichText(value, clamped)
                blocks += when {
                    headingLevel > 0 -> ArticleBlock.Heading(headingLevel, rich)
                    listItemDepth > 0 -> ArticleBlock.ListItem(rich, listNumber)
                    quoteDepth > 0 -> ArticleBlock.Quote(rich)
                    else -> ArticleBlock.Paragraph(rich)
                }
            }
            text.setLength(0)
            spans.clear()
            open.forEach { it.start = 0 }
        }

        private fun appendCollapsed(target: StringBuilder, s: String) {
            for (c in s) {
                if (c.isWhitespace() || c == '\u00A0') {
                    val last = if (target.isEmpty()) '\n' else target[target.length - 1]
                    if (last != ' ' && last != '\n') target.append(' ')
                } else {
                    target.append(c)
                }
            }
        }

        private fun trimTrailing(target: StringBuilder) {
            while (target.isNotEmpty() && (target[target.length - 1] == ' ' || target[target.length - 1] == '\n')) {
                target.setLength(target.length - 1)
            }
        }
    }
}
