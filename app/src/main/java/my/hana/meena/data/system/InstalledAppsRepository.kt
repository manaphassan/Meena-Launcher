package my.hana.meena.data.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

data class DeviceAppInfo(
    val packageName: String,
    val label: String,
    val firstLetter: Char,
    val version: String,
    val iconBitmap: ImageBitmap? = null,
    /** Single-colour glyph for Metro tiles (themed icon, else the adaptive foreground); null = use [iconBitmap]. */
    val glyphBitmap: ImageBitmap? = null,
    val isRealApp: Boolean = true,
    val componentName: android.content.ComponentName? = null,
    val userHandle: android.os.UserHandle? = null,
    /** ApplicationInfo.category (Android 8+); -1 when the app doesn't say. */
    val category: Int = -1,
    /** Non-null for shortcut entries (e.g. a WhatsApp conversation). */
    val shortcutId: String? = null,
    /** The package that owns the shortcut; null for regular apps. */
    val shortcutPackage: String? = null,
    /** Dominant colour extracted from the icon, ARGB int; 0 when not available. */
    val dominantColor: Int = 0
)

object InstalledAppsRepository {

    @Volatile
    private var cachedApps: List<DeviceAppInfo>? = null

    /** Icon pack the cached icons were drawn with, and the one that should be used ("" = system). */
    @Volatile
    private var cachedIconPack: String = ""
    @Volatile
    private var iconPack: String = ""

    private val loadMutex = Mutex()
    private val reloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Goes up each time a new list is published. Snapshot state, so composables reading it redraw. */
    private val revisionState = mutableIntStateOf(0)
    val revision: Int get() = revisionState.intValue

    fun invalidateCache() {
        cachedApps = null
    }

    fun getCachedApp(packageName: String): DeviceAppInfo? {
        revisionState.intValue // read so callers in composition pick up new icons
        return cachedApps?.firstOrNull { it.packageName == packageName }
    }

    /** Everything loaded so far, a to z; empty until the first load finishes. */
    fun cachedAppList(): List<DeviceAppInfo> {
        revisionState.intValue
        return cachedApps.orEmpty()
    }

    /** Switches the icon pack ("" = system icons) and redraws the icons if it changed. */
    suspend fun setIconPack(context: Context, packageName: String) {
        val previous = iconPack
        iconPack = packageName
        if (previous.isNotEmpty() && previous != packageName) IconPackRepository.forget(previous)
        loadInstalledApps(context)
    }

    suspend fun loadInstalledApps(context: Context, forceRefresh: Boolean = false): List<DeviceAppInfo> = withContext(Dispatchers.IO) {
        loadMutex.withLock { loadLocked(context, forceRefresh) }
    }

    private fun loadLocked(context: Context, forceRefresh: Boolean): List<DeviceAppInfo> {
        val pack = iconPack
        val cached = cachedApps
        if (!forceRefresh && cached != null && cachedIconPack == pack) {
            return cached
        }

        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as android.content.pm.LauncherApps
        val userManager = context.getSystemService(Context.USER_SERVICE) as android.os.UserManager

        ensureCallbackRegistered(context.applicationContext)

        val profiles = userManager.userProfiles
        val resultList = mutableListOf<DeviceAppInfo>()

        for (profile in profiles) {
            val activities = launcherApps.getActivityList(null, profile)
            for (info in activities) {
                val pkg = info.applicationInfo.packageName
            // Skip Meena Launcher itself in apps list if desired, or keep it
                val label = try {
                    info.label.toString()
                } catch (e: Exception) {
                    pkg
                }

                val letter = label.firstOrNull()?.uppercaseChar()?.let {
                    if (it in 'A'..'Z') it else '#'
                } ?: '#'

                var version = "v1.0"
                try {
                    val pkgInfo = context.packageManager.getPackageInfo(pkg, 0)
                    version = "v${pkgInfo.versionName ?: "1.0"}"
                } catch (e: Exception) {
                    // Ignore
                }

                var imageBitmap: ImageBitmap? = null
                var glyph: ImageBitmap? = null
                var dominant = 0
                try {
                    val packIcon = if (pack.isEmpty()) null else runCatching {
                        IconPackRepository.iconFor(context, pack, info.componentName.packageName, info.componentName.className)
                            ?.let { if (profile == android.os.Process.myUserHandle()) it else context.packageManager.getUserBadgedIcon(it, profile) }
                            ?.let { drawableToBitmap(it) }
                    }.getOrNull()
                    val bmp = packIcon ?: drawableToBitmap(info.getBadgedIcon(0))
                    imageBitmap = bmp.asImageBitmap()
                    dominant = DominantColor.extract(bmp)
                    // A chosen icon pack keeps its own look; otherwise find a glyph for white tiles.
                    if (packIcon == null) {
                        glyph = runCatching { glyphOf(info.getIcon(0)) }.getOrNull()?.asImageBitmap()
                    }
                } catch (e: Exception) {
                    // Ignore icon error
                }

                resultList.add(
                    DeviceAppInfo(
                        packageName = pkg,
                        label = label,
                        firstLetter = letter,
                        version = version,
                        iconBitmap = imageBitmap,
                        glyphBitmap = glyph,
                        isRealApp = true,
                        componentName = info.componentName,
                        userHandle = profile,
                        category = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) info.applicationInfo.category else -1,
                        dominantColor = dominant
                    )
                )
            }
        }

        // Sort alphabetically
        resultList.sortBy { it.label.lowercase() }
        cachedIconPack = pack
        cachedApps = resultList
        revisionState.intValue++
        return resultList
    }

    private var callbackRegistered = false

    private fun ensureCallbackRegistered(context: Context) {
        if (callbackRegistered) return
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as android.content.pm.LauncherApps
        launcherApps.registerCallback(object : android.content.pm.LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String, user: android.os.UserHandle) { onAppsChanged(context, packageName) }
            override fun onPackageChanged(packageName: String, user: android.os.UserHandle) { onAppsChanged(context, packageName) }
            override fun onPackageRemoved(packageName: String, user: android.os.UserHandle) { onAppsChanged(context, packageName) }
            override fun onPackagesAvailable(packageNames: Array<out String>, user: android.os.UserHandle, replacing: Boolean) { onAppsChanged(context, *packageNames) }
            override fun onPackagesUnavailable(packageNames: Array<out String>, user: android.os.UserHandle, replacing: Boolean) { onAppsChanged(context, *packageNames) }
        }, android.os.Handler(android.os.Looper.getMainLooper()))
        callbackRegistered = true
    }

    /** When apps change, refresh the cache in the background. The old cache stays visible until
     * the reload finishes so favourite tiles never flash empty between changes. */
    private fun onAppsChanged(context: Context, vararg packageNames: String) {
        val pack = iconPack
        if (pack.isNotEmpty() && pack in packageNames) {
            IconPackRepository.forget(pack)
        }
        reloadScope.launch { runCatching { loadInstalledApps(context, forceRefresh = true) } }
    }

    /**
     * The app's single-colour glyph: the themed icon layer (Android 13+), else the adaptive
     * foreground when it is a real shape and not a full-bleed picture. Null for legacy icons.
     * Layers are 108dp with a 72dp viewport, so they are drawn past the edges by the extra inset
     * (https://developer.android.com/reference/android/graphics/drawable/AdaptiveIconDrawable).
     */
    private fun glyphOf(icon: Drawable): Bitmap? {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return null
        val adaptive = icon as? android.graphics.drawable.AdaptiveIconDrawable ?: return null
        val themed = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) adaptive.monochrome else null
        val layer = themed ?: adaptive.foreground ?: return null
        val size = 96 // tiles draw the glyph at 40dp
        val inset = (size * android.graphics.drawable.AdaptiveIconDrawable.getExtraInsetFraction()).toInt()
        val bitmap = createBitmap(size, size)
        layer.setBounds(-inset, -inset, size + inset, size + inset)
        layer.draw(Canvas(bitmap))
        if (themed == null && !looksLikeGlyph(bitmap)) return null
        return bitmap
    }

    /** A glyph leaves most of the tile empty; a foreground that covers it would tint to a white block. */
    private fun looksLikeGlyph(bitmap: Bitmap): Boolean {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val solid = pixels.count { (it ushr 24) > 128 }
        return solid in 1 until (pixels.size * 0.6f).toInt()
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val targetSize = 144
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val bmp = drawable.bitmap
            if (bmp.width <= targetSize && bmp.height <= targetSize) {
                return bmp
            }
            return bmp.scale(targetSize, targetSize)
        }
        val width = if (drawable.intrinsicWidth in 1..targetSize) drawable.intrinsicWidth else targetSize
        val height = if (drawable.intrinsicHeight in 1..targetSize) drawable.intrinsicHeight else targetSize
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}

