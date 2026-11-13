package my.hana.meena.data.news

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Loads news pictures without an image library: one shared OkHttp client with a small disk cache,
 * BitmapFactory sampling down to the size on screen, and a byte-sized memory cache.
 * Addresses are never logged.
 */
object NewsImageLoader {
    private const val DISK_CACHE_BYTES = 20L * 1024 * 1024
    private const val MAX_DOWNLOAD_BYTES = 10L * 1024 * 1024
    private const val MAX_MEMORY_CACHE_BYTES = 48L * 1024 * 1024

    private val memory: LruCache<String, Bitmap> by lazy {
        val size = (Runtime.getRuntime().maxMemory() / 8).coerceAtMost(MAX_MEMORY_CACHE_BYTES).toInt()
        object : LruCache<String, Bitmap>(size) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
        }
    }

    @Volatile
    private var client: OkHttpClient? = null

    private fun client(context: Context): OkHttpClient =
        client ?: synchronized(this) {
            client ?: OkHttpClient.Builder()
                .cache(Cache(File(context.applicationContext.cacheDir, "news-images"), DISK_CACHE_BYTES))
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
                .also { client = it }
        }

    private fun key(url: String, targetPx: Int, fillSquare: Boolean) =
        "$targetPx:${if (fillSquare) "fill" else "width"}:$url"

    /** A bitmap already in memory, or null. Cheap enough to call during composition. */
    fun cached(url: String, targetPx: Int, fillSquare: Boolean): Bitmap? =
        memory.get(key(url, targetPx, fillSquare))

    /**
     * Downloads and decodes [url] so its width (and its height too when [fillSquare]) is at least [targetPx].
     * Returns null on any failure. Cancelling the caller cancels the download.
     */
    suspend fun load(context: Context, url: String, targetPx: Int, fillSquare: Boolean): Bitmap? {
        val key = key(url, targetPx, fillSquare)
        memory.get(key)?.let { return it }
        if (ArticleHtml.safeImageUrl(url) != url) return null
        val bytes = download(context, url) ?: return null
        val bitmap = withContext(Dispatchers.Default) { decode(bytes, targetPx, fillSquare) } ?: return null
        memory.put(key, bitmap)
        return bitmap
    }

    private suspend fun download(context: Context, url: String): ByteArray? {
        val request = try {
            Request.Builder().url(url).build()
        } catch (_: IllegalArgumentException) {
            return null
        }
        val call = withContext(Dispatchers.IO) { client(context) }.newCall(request)
        return suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    cont.resume(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    val bytes = try {
                        response.use { r ->
                            val body = r.body
                            if (!r.isSuccessful || body == null || body.contentLength() > MAX_DOWNLOAD_BYTES) null
                            else readCapped(body.byteStream())
                        }
                    } catch (_: IOException) {
                        null
                    }
                    cont.resume(bytes)
                }
            })
        }
    }

    private fun readCapped(input: java.io.InputStream): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_DOWNLOAD_BYTES) return null
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private fun decode(bytes: ByteArray, targetPx: Int, fillSquare: Boolean): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = inSampleSizeFor(
                width = bounds.outWidth,
                height = bounds.outHeight,
                reqWidth = targetPx,
                reqHeight = if (fillSquare) targetPx else 0
            )
        }
        return try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (_: OutOfMemoryError) {
            null
        }
    }
}

/**
 * Largest power-of-two sample size that keeps the image at least [reqWidth] wide and [reqHeight] tall
 * (0 = height doesn't matter), then halved further while it would exceed [maxPixels].
 */
internal fun inSampleSizeFor(
    width: Int,
    height: Int,
    reqWidth: Int,
    reqHeight: Int,
    maxPixels: Long = 4_000_000L
): Int {
    if (width <= 0 || height <= 0 || reqWidth <= 0) return 1
    var sample = 1
    while (width / (sample * 2) >= reqWidth && height / (sample * 2) >= reqHeight) sample *= 2
    while ((width.toLong() / sample) * (height.toLong() / sample) > maxPixels) sample *= 2
    return sample
}
