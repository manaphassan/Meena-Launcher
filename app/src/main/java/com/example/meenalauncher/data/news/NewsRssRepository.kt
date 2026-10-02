package com.example.meenalauncher.data.news

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
object NewsRssRepository {
    private const val TAG = "NewsRssRepository"

    private const val AMANZ_FEED_URL = "https://amanz.my/feed/"
    private const val SUAMISIHAT_FEED_URL = "https://suamisihat.com.my/feed/"

    private val rfc822DateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
    private val rfc822DateFormatFallback = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
    private val displayDateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    // High quality offline fallback articles based on live feeds
    private val initialFallbackArticles = listOf(
        NewsArticle(
            id = "suami-laser-circ",
            title = "Berapa Lama Sunat Laser Sembuh? Tips & Penjagaan Zakar",
            link = "https://suamisihat.com.my/zakar/tempoh-sembuh-sunat-laser/",
            source = "Suami Sihat",
            sourceBadgeColorHex = "#107C10",
            pubDateMs = System.currentTimeMillis() - 1000L * 60 * 45, // 45m ago
            formattedDate = "45m ago",
            category = "Kesihatan Lelaki",
            description = "Memahami sunat laser secara klinikal, tempoh pemulihan tisu dan tips penjagaan rapi selepas prosedur."
        ),
        NewsArticle(
            id = "amanz-suncatcher",
            title = "Project Suncatcher: Google Uji Pusat Data AI Di Orbit Angkasa Lepas",
            link = "https://amanz.my/2025541560",
            source = "Amanz",
            sourceBadgeColorHex = "#00A4EF",
            pubDateMs = System.currentTimeMillis() - 1000L * 60 * 90, // 1h 30m ago
            formattedDate = "1h ago",
            category = "Teknologi & AI",
            description = "Google melancarkan satelit prototaip dilengkapi empat cip TPU untuk meneroka pusat data berkuasa solar tanpa had."
        ),
        NewsArticle(
            id = "amanz-firstdate",
            title = "Singapura Perkenal Aplikasi Temu Janji FirstDate Atasi Penurunan Kelahiran",
            link = "https://firstdate.sandbox.gov.sg/",
            source = "Amanz",
            sourceBadgeColorHex = "#00A4EF",
            pubDateMs = System.currentTimeMillis() - 1000L * 60 * 180, // 3h ago
            formattedDate = "3h ago",
            category = "Aplikasi",
            description = "Langkah kerajaan Singapura menyediakan insentif dan aplikasi temu janji bagi menangani penurunan kadar kelahiran."
        ),
        NewsArticle(
            id = "amanz-rtm-f1",
            title = "RTM Sahkan Siaran Perlumbaan Formula 1 Malaysia Secara Percuma di TVOkey",
            link = "https://amanz.my/",
            source = "Amanz",
            sourceBadgeColorHex = "#00A4EF",
            pubDateMs = System.currentTimeMillis() - 1000L * 60 * 240, // 4h ago
            formattedDate = "4h ago",
            category = "Sukan & TV",
            description = "Peminat F1 tempatan boleh menyaksikan perlumbaan berprestij terus dari Litar Antarabangsa Sepang secara langsung."
        ),
        NewsArticle(
            id = "amanz-dji-cameras",
            title = "DJI & Insta360 Jual Lebih Banyak Kamera Berbanding Jenama Tradisional",
            link = "https://amanz.my/",
            source = "Amanz",
            sourceBadgeColorHex = "#00A4EF",
            pubDateMs = System.currentTimeMillis() - 1000L * 60 * 360, // 6h ago
            formattedDate = "6h ago",
            category = "Gajet",
            description = "Kamera gimbal kompak melonjak mendominasi pasaran dengan jualan lebih 10 juta unit mengatasi kamera konvensional."
        ),
        NewsArticle(
            id = "amanz-ilmucode",
            title = "ILMUcode: Platform Pembangunan Perisian AI Tempatan Dilancarkan di UM",
            link = "https://amanz.my/",
            source = "Amanz",
            sourceBadgeColorHex = "#00A4EF",
            pubDateMs = System.currentTimeMillis() - 1000L * 60 * 480, // 8h ago
            formattedDate = "8h ago",
            category = "Inovasi Tempatan",
            description = "Dikuasakan enjin ILMU GLM 5.3, platform ini mempercepatkan pembangunan perisian dan pemodenan sistem negara."
        )
    )

    private val _newsFlow = MutableStateFlow<List<NewsArticle>>(initialFallbackArticles)
    val newsFlow: StateFlow<List<NewsArticle>> = _newsFlow.asStateFlow()

    private val _isLoadingFlow = MutableStateFlow(false)
    val isLoadingFlow: StateFlow<Boolean> = _isLoadingFlow.asStateFlow()

    private val _lastUpdatedFlow = MutableStateFlow<Long>(System.currentTimeMillis())
    val lastUpdatedFlow: StateFlow<Long> = _lastUpdatedFlow.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Fetch feeds in background upon repository init
        refresh(force = false)
    }

    fun refresh(force: Boolean = false) {
        val now = System.currentTimeMillis()
        // Throttle refreshes to at most once per 60 seconds unless forced
        if (!force && now - _lastUpdatedFlow.value < 60_000 && _newsFlow.value.size >= 6) {
            return
        }

        scope.launch {
            _isLoadingFlow.value = true
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
                    // Always show the 6 latest items
                    _newsFlow.value = merged.take(6)
                    _lastUpdatedFlow.value = System.currentTimeMillis()
                    Log.d(TAG, "Successfully refreshed RSS feeds: ${merged.size} total items, showing top 6.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching RSS feeds, retaining cached articles", e)
            } finally {
                _isLoadingFlow.value = false
            }
        }
    }

    private suspend fun fetchFeed(
        urlStr: String,
        sourceName: String,
        sourceBadgeColor: String
    ): List<NewsArticle> = withContext(Dispatchers.IO) {
        val items = mutableListOf<NewsArticle>()
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlStr)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36 MeenaLauncher/1.0"
                )
                setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml, */*")
            }

            if (connection.responseCode in 200..299) {
                connection.inputStream.use { input ->
                    items.addAll(parseRssXml(input, sourceName, sourceBadgeColor))
                }
            } else {
                Log.w(TAG, "Failed to fetch $urlStr: HTTP ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching feed from $urlStr: ${e.message}")
        } finally {
            connection?.disconnect()
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
