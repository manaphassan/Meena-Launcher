package my.hana.meena.data.news

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import my.hana.meena.ui.widgets.RefreshPolicy
import my.hana.meena.ui.widgets.WidgetSpec
import my.hana.meena.ui.widgets.WidgetState
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data model for a local news article from RSS feeds (Amanz.my & SuamiSihat.com.my).
 */
data class NewsArticle(
    val id: String,
    val title: String,
    val link: String,
    val source: String,             // "Amanz" or "Suami Sihat"
    val sourceBadgeColorHex: String,// "#00A4EF" (Amanz Cyan) or "#107C10" (Suami Sihat Emerald)
    /** 0 when the feed gave no readable date. */
    val pubDateMs: Long,
    val formattedDate: String,      // e.g. "15m ago", "2h ago", "23 aug, 09:43 am"; empty when the date is unknown
    val category: String = "",
    /** Plain text, tags removed. */
    val description: String = "",
    /** https thumbnail picked by [RssImage.pick], or null. */
    val imageUrl: String? = null,
    /** Raw content:encoded HTML for the reader; empty when the feed has none. */
    val contentHtml: String = "",
    /** The feed this came from, so per-feed choices (such as blurring pictures) can apply. */
    val feedUrl: String = ""
)

/** What the last fetch of one feed found. Shown in settings, never invented. */
data class FeedStatus(
    val url: String,
    val name: String,
    val articleCount: Int,
    val checkedAt: Long,
    /** null when the fetch worked; otherwise a short reason such as "http 404". */
    val problem: String? = null
)

/**
 * Fetches, parses and caches the latest articles from the user's feeds
 * (Amanz and Suami Sihat until the user picks their own).
 */
object NewsRssRepository : WidgetSpec<List<NewsArticle>> {
    override val id = "widget-news"
    override val title = "news"
    override val source = "RSS feeds you pick"
    override val refreshPolicy = RefreshPolicy.PERIODIC_MINUTES
    override val defaultRefreshMinutes = 60

    private const val TAG = "NewsRssRepository"

    /**
     * Amanz's own feed: it showed a bot-check page instead of RSS on 14 Nov 2026 and worked again
     * later that day. If it fails or isn't RSS, Google News' copy (no pictures) is used instead,
     * without changing the saved address.
     */
    const val AMANZ_FEED_URL = my.hana.meena.data.model.MeenaUserSettings.AMANZ_FEED
    private const val AMANZ_FALLBACK_URL = my.hana.meena.data.model.MeenaUserSettings.AMANZ_GOOGLE_NEWS_FEED

    /** The feed to try when [feedUrl] gives nothing, or null. Only Amanz has one. */
    fun fallbackFor(feedUrl: String): String? =
        if (hostOf(feedUrl)?.removePrefix("www.")?.endsWith("amanz.my") == true) AMANZ_FALLBACK_URL else null
    private const val SUAMISIHAT_FEED_URL = "https://suamisihat.com.my/feed/"

    /** Used while the user has not picked any feeds. */
    val DEFAULT_FEEDS = listOf(AMANZ_FEED_URL, SUAMISIHAT_FEED_URL)

    /** The feeds actually fetched for a saved list: the user's own, or the built-in ones when empty. */
    fun effectiveFeeds(saved: List<String>): List<String> = saved.ifEmpty { DEFAULT_FEEDS }

    /** How many merged articles are kept; the UI decides how many to show. */
    const val MAX_ARTICLES = 40
    /** Each feed keeps at least this many of its newest articles in the merged list. */
    const val MIN_PER_SOURCE = 5
    private const val MAX_CONTENT_CHARS = 200_000

    private val displayDateFormat get() = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH)

    // No hardcoded demo data as per rules; empty until fetched
    private val initialFallbackArticles = emptyList<NewsArticle>()

    private val _newsFlow = MutableStateFlow<WidgetState<List<NewsArticle>>>(WidgetState.NeedsSetup)
    val newsFlow: StateFlow<WidgetState<List<NewsArticle>>> = _newsFlow.asStateFlow()

    private val _lastUpdatedFlow = MutableStateFlow<Long>(System.currentTimeMillis())
    val lastUpdatedFlow: StateFlow<Long> = _lastUpdatedFlow.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    /** True while a fetch runs. Articles already on screen stay in [newsFlow] meanwhile. */
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _feedStatuses = MutableStateFlow<List<FeedStatus>>(emptyList())
    /** One entry per feed from the last fetch, in feed order. Empty until the first fetch finishes. */
    val feedStatuses: StateFlow<List<FeedStatus>> = _feedStatuses.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    @Volatile
    private var customFeeds: List<String> = emptyList()

    /** One fetch at a time, so a slow earlier fetch can't overwrite a newer one. */
    private val refreshMutex = kotlinx.coroutines.sync.Mutex()

    /** False until the saved list arrives, so the built-in feeds are never fetched in its place. */
    @Volatile
    private var feedsKnown = false

    /** Called with the saved list at start and on every edit; the first call and any change refetch. */
    fun updateFeeds(feeds: List<String>) {
        if (!feedsKnown || customFeeds != feeds) {
            customFeeds = feeds
            feedsKnown = true
            scope.launch { refresh(force = true) }
        }
    }

    override fun fetchInitialState(): WidgetState<List<NewsArticle>> = _newsFlow.value

    override suspend fun refresh(): WidgetState<List<NewsArticle>> = refresh(force = false)

    suspend fun refresh(force: Boolean): WidgetState<List<NewsArticle>> =
        refreshMutex.withLock { refreshLocked(force) }

    private suspend fun refreshLocked(force: Boolean): WidgetState<List<NewsArticle>> {
        if (!feedsKnown) return _newsFlow.value
        val now = System.currentTimeMillis()
        val currentData = _newsFlow.value
        
        if (!force && currentData is WidgetState.Ready && now - _lastUpdatedFlow.value < 60_000) {
            return currentData
        }

        val previous = (currentData as? WidgetState.Ready)?.data ?: (currentData as? WidgetState.Stale)?.data
        // Keep what is on screen while refreshing; only show loading when there is nothing yet.
        if (previous.isNullOrEmpty()) _newsFlow.value = WidgetState.Loading
        _isRefreshing.value = true
        try {
            val feedList = effectiveFeeds(customFeeds)
            val allArticles = mutableListOf<NewsArticle>()
            val statuses = mutableListOf<FeedStatus>()

            for (feedUrl in feedList) {
                val name = sourceNameFor(feedUrl)
                val color = when {
                    feedUrl.contains("amanz", ignoreCase = true) -> "#00A4EF"
                    feedUrl.contains("suamisihat", ignoreCase = true) -> "#107C10"
                    feedUrl.contains("thestar", ignoreCase = true) -> "#E51400"
                    feedUrl.contains("bernama", ignoreCase = true) -> "#0050EF"
                    feedUrl.contains("malaysiakini", ignoreCase = true) -> "#FA6800"
                    feedUrl.contains("techcrunch", ignoreCase = true) -> "#008A00"
                    feedUrl.contains("ycombinator", ignoreCase = true) -> "#FA6800"
                    else -> "#00A4EF"
                }
                val result = fetchFeed(feedUrl, name, color).let { first ->
                    val fallback = fallbackFor(feedUrl)
                    if (first.first.isEmpty() && fallback != null) {
                        Log.w(TAG, "No articles from $feedUrl (${first.second ?: "not a feed"}); trying its fallback")
                        val second = fetchFeed(fallback, name, color)
                        // Articles keep the saved address, so per-feed settings such as nsfw still apply.
                        if (second.first.isNotEmpty()) second.first.map { it.copy(feedUrl = feedUrl) } to null else first
                    } else first
                }
                allArticles.addAll(result.first)
                statuses += FeedStatus(feedUrl, name, result.first.size, System.currentTimeMillis(), result.second)
            }
            // The feed list changed while fetching; a fetch for the new list is already queued.
            if (feedList != effectiveFeeds(customFeeds)) return _newsFlow.value
            _feedStatuses.value = statuses

            val merged = NewsMerge.merge(allArticles, MAX_ARTICLES, MIN_PER_SOURCE)

            if (merged.isNotEmpty()) {
                _newsFlow.value = WidgetState.Ready(merged, now)
                _lastUpdatedFlow.value = now
                Log.d(TAG, "Refreshed RSS feeds: ${allArticles.size} items, keeping ${merged.size}.")
            } else if (statuses.isNotEmpty() && statuses.all { it.problem != null }) {
                // Every feed failed: that's an error, not "no stories".
                _newsFlow.value = if (!previous.isNullOrEmpty()) {
                    WidgetState.Stale(previous, _lastUpdatedFlow.value)
                } else {
                    WidgetState.Error("couldn't load any feed")
                }
            } else {
                _newsFlow.value = WidgetState.Empty
            }
            return _newsFlow.value
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching RSS feeds, retaining cached articles", e)
            if (!previous.isNullOrEmpty()) {
                _newsFlow.value = WidgetState.Stale(previous, _lastUpdatedFlow.value)
            } else {
                _newsFlow.value = WidgetState.Error("Failed to fetch news", e)
            }
            return _newsFlow.value
        } finally {
            _isRefreshing.value = false
        }
    }

    /** Display name for a feed URL: a known source name, or the site's host. */
    fun sourceNameFor(feedUrl: String): String = when {
        feedUrl.contains("amanz", ignoreCase = true) -> "Amanz"
        feedUrl.contains("suamisihat", ignoreCase = true) -> "Suami Sihat"
        feedUrl.contains("thestar", ignoreCase = true) -> "The Star"
        feedUrl.contains("bernama", ignoreCase = true) -> "Bernama"
        feedUrl.contains("malaysiakini", ignoreCase = true) -> "Malaysiakini"
        feedUrl.contains("techcrunch", ignoreCase = true) -> "TechCrunch"
        feedUrl.contains("ycombinator", ignoreCase = true) -> "Hacker News"
        else -> hostOf(feedUrl)?.removePrefix("www.")?.substringBefore(".") ?: "News"
    }

    private fun hostOf(url: String): String? =
        url.substringAfter("://", "").substringBefore("/").substringBefore("?").ifBlank { null }

    /** Returns the articles found and, when the fetch failed, a short reason. */
    private suspend fun fetchFeed(
        urlStr: String,
        sourceName: String,
        sourceBadgeColor: String
    ): Pair<List<NewsArticle>, String?> = withContext(Dispatchers.IO) {
        val items = mutableListOf<NewsArticle>()
        var problem: String? = null
        try {
            val client = okhttp3.OkHttpClient()
            val request = okhttp3.Request.Builder()
                .url(urlStr)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36 MeenaLauncher/1.0")
                .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.byteStream()?.use { input ->
                    items.addAll(parseRssXml(input, sourceName, sourceBadgeColor).map { it.copy(feedUrl = urlStr) })
                }
                // A page that isn't a feed (a bot check, a home page) parses to nothing; say so.
                if (items.isEmpty() && response.header("Content-Type")?.contains("html", ignoreCase = true) == true) {
                    problem = "not a feed"
                }
            } else {
                Log.w(TAG, "Failed to fetch $urlStr: HTTP ${response.code}")
                problem = "http ${response.code}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching feed from $urlStr: ${e.message}")
            problem = "couldn't connect"
        }
        items to problem
    }

    private fun parseRssXml(
        inputStream: InputStream,
        sourceName: String,
        sourceBadgeColor: String
    ): List<NewsArticle> {
        val list = mutableListOf<NewsArticle>()
        try {
            val factory = XmlPullParserFactory.newInstance().apply {
                isNamespaceAware = false
            }
            val parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            var inItem = false

            var curTitle = ""
            var curLink = ""
            var curPubDate = ""
            var curDescription = ""
            var curCategory = ""
            var curContent = ""
            var curThumbnail: String? = null
            var curMediaContent: String? = null
            var curEnclosure: String? = null
            var curPublisher: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name?.lowercase(Locale.ROOT)

                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName == "item") {
                            inItem = true
                            curTitle = ""
                            curLink = ""
                            curPubDate = ""
                            curDescription = ""
                            curCategory = ""
                            curContent = ""
                            curThumbnail = null
                            curMediaContent = null
                            curEnclosure = null
                            curPublisher = null
                        } else if (inItem) {
                            when (tagName) {
                                "title" -> curTitle = readText(parser)
                                "link" -> curLink = readText(parser)
                                "pubdate" -> curPubDate = readText(parser)
                                "description" -> curDescription = readText(parser)
                                "content:encoded" -> curContent = readText(parser)
                                "media:thumbnail" -> if (curThumbnail == null) {
                                    curThumbnail = ArticleHtml.safeImageUrl(parser.getAttributeValue(null, "url"))
                                }
                                "media:content" -> if (curMediaContent == null && RssImage.isImage(
                                        parser.getAttributeValue(null, "medium"),
                                        parser.getAttributeValue(null, "type")
                                    )
                                ) {
                                    curMediaContent = ArticleHtml.safeImageUrl(parser.getAttributeValue(null, "url"))
                                }
                                "enclosure" -> if (curEnclosure == null &&
                                    RssImage.isImage(null, parser.getAttributeValue(null, "type"))
                                ) {
                                    curEnclosure = ArticleHtml.safeImageUrl(parser.getAttributeValue(null, "url"))
                                }
                                "source" -> curPublisher = readText(parser)
                                "category" -> {
                                    if (curCategory.isBlank()) {
                                        curCategory = readText(parser)
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName == "item" && inItem) {
                            inItem = false
                            val cleanTitle = NewsTitles.stripPublisher(cleanHtml(curTitle), curPublisher)
                            if (cleanTitle.isNotBlank()) {
                                val pubDateMs = parseDateToMillis(curPubDate)
                                val formattedDate = formatRelativeTime(pubDateMs)
                                val cleanDesc = cleanHtml(curDescription).take(4_000)
                                val content = if (curContent.length > MAX_CONTENT_CHARS) "" else curContent

                                list.add(
                                    NewsArticle(
                                        id = "${sourceName.lowercase()}-${curLink.trim().ifBlank { cleanTitle }.hashCode()}",
                                        title = cleanTitle,
                                        link = curLink.trim(),
                                        source = sourceName,
                                        sourceBadgeColorHex = sourceBadgeColor,
                                        pubDateMs = pubDateMs,
                                        formattedDate = formattedDate,
                                        category = cleanHtml(curCategory),
                                        description = cleanDesc,
                                        imageUrl = RssImage.pick(curThumbnail, curMediaContent, curEnclosure, content, curDescription),
                                        contentHtml = content
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "XML parse error for $sourceName", e)
        }
        return list
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text ?: ""
            parser.nextTag()
        }
        return result
    }

    /** 0 when the date is missing or unreadable, so no made-up "just now" is shown. */
    private fun parseDateToMillis(dateStr: String): Long {
        if (dateStr.isBlank()) return 0L
        // New formatters per call: SimpleDateFormat isn't thread-safe and refreshes can overlap.
        for (pattern in listOf("EEE, dd MMM yyyy HH:mm:ss Z", "EEE, dd MMM yyyy HH:mm:ss zzz")) {
            try {
                SimpleDateFormat(pattern, Locale.US).parse(dateStr.trim())?.let { return it.time }
            } catch (_: Exception) {
            }
        }
        return 0L
    }

    private fun formatRelativeTime(timestampMs: Long): String {
        if (timestampMs <= 0L) return ""
        val diffMs = System.currentTimeMillis() - timestampMs
        if (diffMs < 0) return "just now"

        val seconds = diffMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "yesterday"
            days < 7 -> "${days}d ago"
            else -> displayDateFormat.format(Date(timestampMs)).lowercase(Locale.ENGLISH)
        }
    }

    private fun cleanHtml(raw: String): String = ArticleHtml.plainText(raw)

    /**
     * Helper to open an article link in the browser.
     */
    fun openArticle(context: Context, article: NewsArticle) {
        try {
            val uri = Uri.parse(article.link)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open article url: ${article.link}", e)
        }
    }
}


