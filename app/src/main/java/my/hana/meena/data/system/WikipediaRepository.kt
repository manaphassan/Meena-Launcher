package my.hana.meena.data.system

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import my.hana.meena.ui.widgets.WidgetState
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class WikiArticle(val title: String, val description: String, val extract: String, val url: String)

/**
 * A random English Wikipedia article for the Today page, from the Wikimedia REST API
 * (https://www.mediawiki.org/wiki/Wikimedia_REST_API, endpoint page/random/summary).
 * The featured-content feed is not used: Wikifeeds is scheduled for deprecation from July 2026.
 * Wikimedia's access policy requires an identifying User-Agent on every request.
 */
object WikipediaRepository {
    private const val TAG = "WikipediaRepository"
    private const val ENDPOINT = "https://en.wikipedia.org/api/rest_v1/page/random/summary"
    private const val CACHE_DURATION_MS = 3 * 60 * 60 * 1000L
    private const val MAX_TRIES = 3

    private val _state = MutableStateFlow<WidgetState<WikiArticle>>(WidgetState.Loading)
    val state: StateFlow<WidgetState<WikiArticle>> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var activeJob: Job? = null
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    @Serializable
    private data class Summary(
        val type: String = "",
        val title: String = "",
        val description: String = "",
        val extract: String = "",
        val content_urls: ContentUrls? = null
    )

    @Serializable
    private data class ContentUrls(val mobile: Page? = null, val desktop: Page? = null)

    @Serializable
    private data class Page(val page: String = "")

    /** Returns null for disambiguation pages, stubs without text, or bad JSON, so the caller tries again. */
    fun parse(body: String): WikiArticle? {
        val summary = runCatching { json.decodeFromString(Summary.serializer(), body) }.getOrNull() ?: return null
        if (summary.type != "standard" || summary.title.isBlank() || summary.extract.isBlank()) return null
        val url = summary.content_urls?.mobile?.page?.ifBlank { null } ?: summary.content_urls?.desktop?.page.orEmpty()
        if (!url.startsWith("https://")) return null
        return WikiArticle(summary.title.trim(), summary.description.trim(), summary.extract.trim(), url)
    }

    fun refresh(context: Context, force: Boolean = false) {
        val current = _state.value
        if (!force && current is WidgetState.Ready && System.currentTimeMillis() - current.updatedAt < CACHE_DURATION_MS) return
        if (activeJob?.isActive == true) return
        if (current is WidgetState.Ready) _state.value = WidgetState.Stale(current.data, current.updatedAt)
        else if (current !is WidgetState.Stale) _state.value = WidgetState.Loading

        val userAgent = "Meena/${versionName(context)} (https://github.com/manaphassan/Meena-Launcher; privacy@hana.my) okhttp/4.12.0"
        activeJob = scope.launch {
            try {
                repeat(MAX_TRIES) {
                    val request = Request.Builder().url(ENDPOINT).header("User-Agent", userAgent).build()
                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            fail("couldn't reach wikipedia (${response.code})")
                            return@launch
                        }
                        parse(response.body?.string().orEmpty())?.let {
                            _state.value = WidgetState.Ready(it, System.currentTimeMillis())
                            return@launch
                        }
                    }
                }
                fail("couldn't find an article")
            } catch (e: Exception) {
                Log.w(TAG, "Wikipedia fetch failed: ${e.javaClass.simpleName}")
                fail("couldn't reach wikipedia")
            }
        }
    }

    private fun fail(message: String) {
        if (_state.value !is WidgetState.Stale) _state.value = WidgetState.Error(message)
    }

    private fun versionName(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0"
}
