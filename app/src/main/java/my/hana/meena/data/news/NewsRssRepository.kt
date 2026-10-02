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
    val pubDateMs: Long,
    val formattedDate: String,      // e.g. "15m ago", "2h ago", "Today 09:43 AM", "23 Aug"
    val category: String = "News",
    val description: String = ""
)

/**
 * Repository to fetch, parse, and cache the 6 latest local news items
 * from Amanz (https://amanz.my/feed/) and Suami Sihat (https://suamisihat.com.my/feed/).
 */
object NewsRssRepository : WidgetSpec<List<NewsArticle>> {
    override val id = "widget-news"
    override val title = "news"
    override val source = "Amanz & Suami Sihat RSS"
    override val refreshPolicy = RefreshPolicy.PERIODIC_MINUTES
    override val defaultRefreshMinutes = 60

    private const val TAG = "NewsRssRepository"

    private const val AMANZ_FEED_URL = "https://amanz.my/feed/"
    private const val SUAMISIHAT_FEED_URL = "https://suamisihat.com.my/feed/"

    private val rfc822DateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
    private val rfc822DateFormatFallback = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
    private val displayDateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    // No hardcoded demo data as per rules; empty until fetched
    private val initialFallbackArticles = emptyList<NewsArticle>()

    private val _newsFlow = MutableStateFlow<WidgetState<List<NewsArticle>>>(WidgetState.NeedsSetup)
    val newsFlow: StateFlow<WidgetState<List<NewsArticle>>> = _newsFlow.asStateFlow()

    private val _lastUpdatedFlow = MutableStateFlow<Long>(System.currentTimeMillis())
    val lastUpdatedFlow: StateFlow<Long> = _lastUpdatedFlow.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Fetch feeds in background upon repository init
        scope.launch { refresh() }
    }

    override fun fetchInitialState(): WidgetState<List<NewsArticle>> = _newsFlow.value

    override suspend fun refresh(): WidgetState<List<NewsArticle>> {
        val now = System.currentTimeMillis()
        val currentData = _newsFlow.value
        
        if (currentData is WidgetState.Ready && now - _lastUpdatedFlow.value < 60_000) {
            return currentData
        }

        _newsFlow.value = WidgetState.Loading
        try {
            val amanzArticles = fetchFeed(
                urlStr = AMANZ_FEED_URL,
                sourceName = "Amanz",
                sourceBadgeColor = "#00A4EF"
            )
            val suamiArticles = fetchFeed(
                urlStr = SUAMISIHAT_FEED_URL,
                sourceName = "Suami Sihat",
                sourceBadgeColor = "#107C10"
            )

            val merged = (amanzArticles + suamiArticles)
                .distinctBy { it.link.ifBlank { it.title } }
                .sortedByDescending { it.pubDateMs }

            if (merged.isNotEmpty()) {
                val top6 = merged.take(6)
                _newsFlow.value = WidgetState.Ready(top6, now)
                _lastUpdatedFlow.value = now
                Log.d(TAG, "Successfully refreshed RSS feeds: ${merged.size} total items, showing top 6.")
                return _newsFlow.value
            } else {
                _newsFlow.value = WidgetState.Empty
                return WidgetState.Empty
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching RSS feeds, retaining cached articles", e)
            val prevState = if (currentData is WidgetState.Ready) currentData.data else emptyList()
            if (prevState.isNotEmpty()) {
                _newsFlow.value = WidgetState.Stale(prevState, _lastUpdatedFlow.value)
            } else {
                _newsFlow.value = WidgetState.Error("Failed to fetch news", e)
            }
            return _newsFlow.value
        }
    }

    private suspend fun fetchFeed(
        urlStr: String,
        sourceName: String,
        sourceBadgeColor: String
    ): List<NewsArticle> = withContext(Dispatchers.IO) {
        val items = mutableListOf<NewsArticle>()
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
                    items.addAll(parseRssXml(input, sourceName, sourceBadgeColor))
                }
            } else {
                Log.w(TAG, "Failed to fetch $urlStr: HTTP ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching feed from $urlStr: ${e.message}")
        }
        items
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
                        } else if (inItem) {
                            when (tagName) {
                                "title" -> curTitle = readText(parser)
                                "link" -> curLink = readText(parser)
                                "pubdate" -> curPubDate = readText(parser)
                                "description" -> curDescription = readText(parser)
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
                            val cleanTitle = cleanHtml(curTitle)
                            if (cleanTitle.isNotBlank()) {
                                val pubDateMs = parseDateToMillis(curPubDate)
                                val formattedDate = formatRelativeTime(pubDateMs)
                                val cleanDesc = cleanHtml(curDescription).take(140)

                                list.add(
                                    NewsArticle(
                                        id = "${sourceName.lowercase()}-${curLink.hashCode()}",
                                        title = cleanTitle,
                                        link = curLink.trim(),
                                        source = sourceName,
                                        sourceBadgeColorHex = sourceBadgeColor,
                                        pubDateMs = pubDateMs,
                                        formattedDate = formattedDate,
                                        category = if (curCategory.isNotBlank()) cleanHtml(curCategory) else if (sourceName == "Amanz") "Teknologi" else "Kesihatan",
                                        description = cleanDesc
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

    private fun parseDateToMillis(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        return try {
            rfc822DateFormat.parse(dateStr.trim())?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            try {
                rfc822DateFormatFallback.parse(dateStr.trim())?.time ?: System.currentTimeMillis()
            } catch (e2: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    private fun formatRelativeTime(timestampMs: Long): String {
        val diffMs = System.currentTimeMillis() - timestampMs
        if (diffMs < 0) return "Just now"

        val seconds = diffMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            days < 7 -> "${days}d ago"
            else -> displayDateFormat.format(Date(timestampMs))
        }
    }

    private fun cleanHtml(raw: String): String {
        return raw
            .replace(Regex("<[^>]*>"), "") // strip HTML tags
            .replace("&#038;", "&")
            .replace("&amp;", "&")
            .replace("&#8217;", "'")
            .replace("&#8216;", "'")
            .replace("&#8220;", "\"")
            .replace("&#8221;", "\"")
            .replace("&#8211;", "–")
            .replace("&#8212;", "—")
            .replace("&quot;", "\"")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace(Regex("\\[&#8230;\\]"), "...")
            .replace(Regex("\\[\\.\\.\\.\\]"), "...")
            .trim()
    }

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


