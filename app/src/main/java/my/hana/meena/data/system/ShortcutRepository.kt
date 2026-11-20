package my.hana.meena.data.system

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap

/**
 * Queries and caches app shortcuts (conversation shortcuts from WhatsApp, etc.)
 * so they can live in the favourite tiles alongside regular apps.
 *
 * Requires API 25+; on API 24 every call returns empty / null gracefully.
 */
object ShortcutRepository {

    /** Cached shortcut tiles keyed by their favourite-rules key ("shortcut:pkg:id"). */
    @Volatile
    private var cache: Map<String, DeviceAppInfo> = emptyMap()

    private val _revision = kotlinx.coroutines.flow.MutableStateFlow(0)
    /** Goes up whenever the cache changes, so tiles redraw with a newly loaded shortcut. */
    val revision: kotlinx.coroutines.flow.StateFlow<Int> = _revision

    private fun remember(key: String, entry: DeviceAppInfo) {
        cache = cache + (key to entry)
        _revision.value++
    }

    /** Returns the cached [DeviceAppInfo] for a shortcut key, or null when not loaded. */
    fun getCachedShortcut(key: String): DeviceAppInfo? = cache[key]

    /** Adds a shortcut another app just asked to pin (for example a WhatsApp chat). */
    fun rememberPinned(context: Context, info: ShortcutInfo): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return null
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val key = FavouriteRules.shortcutKey(info.`package`, info.id)
        remember(key, toDeviceAppInfo(context, launcherApps, info, key))
        return key
    }

    /**
     * Keeps Android's list of shortcuts pinned by Meena the same as the shortcut tiles, so a chat
     * tile keeps working even after the app drops it from its own shortcut list, and a removed
     * tile is unpinned. Only works while Meena is the home app; otherwise Android refuses quietly.
     */
    fun syncPinned(context: Context, pinnedAppIds: List<String>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        if (!runCatching { launcherApps.hasShortcutHostPermission() }.getOrDefault(false)) return
        val user = Process.myUserHandle()
        val wanted = pinnedAppIds.flatMap { FavouriteRules.members(it) }
            .mapNotNull { FavouriteRules.parseShortcut(it) }
            .groupBy({ it.first }, { it.second })
        val alreadyPinned = runCatching {
            launcherApps.getShortcuts(
                LauncherApps.ShortcutQuery().setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED),
                user
            )
        }.getOrNull().orEmpty().groupBy({ it.`package` }, { it.id })
        for (pkg in wanted.keys + alreadyPinned.keys) {
            val ids = wanted[pkg].orEmpty().distinct()
            if (ids.toSet() == alreadyPinned[pkg].orEmpty().toSet()) continue
            runCatching { launcherApps.pinShortcuts(pkg, ids, user) }
        }
    }

    /**
     * Loads the [DeviceAppInfo] for a single shortcut key that is already pinned.
     * Called once per pinned shortcut at startup so tiles show immediately.
     */
    fun loadPinnedShortcut(context: Context, key: String): DeviceAppInfo? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return null
        val (pkg, shortcutId) = FavouriteRules.parseShortcut(key) ?: return null
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val query = LauncherApps.ShortcutQuery()
            .setPackage(pkg)
            .setShortcutIds(listOf(shortcutId))
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    // Shortcuts Meena pinned itself; a regular launcher can't see other launchers' pins.
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
            )
        val info = runCatching { launcherApps.getShortcuts(query, Process.myUserHandle()) }
            .getOrNull()?.firstOrNull() ?: return null
        val entry = toDeviceAppInfo(context, launcherApps, info, key)
        remember(key, entry)
        return entry
    }

    /**
     * Returns every launchable shortcut published by [packageName], suitable for a picker.
     * Each entry's [DeviceAppInfo.packageName] is the full shortcut key.
     */
    fun queryShortcuts(context: Context, packageName: String): List<DeviceAppInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return emptyList()
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val query = LauncherApps.ShortcutQuery()
            .setPackage(packageName)
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
            )
        val shortcuts = runCatching { launcherApps.getShortcuts(query, Process.myUserHandle()) }
            .getOrNull().orEmpty()
        return shortcuts.mapNotNull { info ->
            val key = FavouriteRules.shortcutKey(packageName, info.id)
            val entry = toDeviceAppInfo(context, launcherApps, info, key)
            remember(key, entry)
            entry
        }
    }

    /** Returns apps that publish at least one shortcut, sorted by label. */
    fun appsWithShortcuts(context: Context): List<DeviceAppInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return emptyList()
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val query = LauncherApps.ShortcutQuery()
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
            )
        val shortcuts = runCatching { launcherApps.getShortcuts(query, Process.myUserHandle()) }
            .getOrNull().orEmpty()
        val packages = shortcuts.map { it.`package` }.distinct()
        return packages.mapNotNull { InstalledAppsRepository.getCachedApp(it) }
            .sortedBy { it.label.lowercase() }
    }

    // ---- Internal ----

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun toDeviceAppInfo(
        context: Context,
        launcherApps: LauncherApps,
        info: ShortcutInfo,
        key: String
    ): DeviceAppInfo {
        val icon = runCatching {
            launcherApps.getShortcutIconDrawable(info, context.resources.displayMetrics.densityDpi)
        }.getOrNull()
        val bitmap = icon?.let { drawableToBitmap(it) }?.asImageBitmap()
        val label = info.shortLabel?.toString()
            ?: info.longLabel?.toString()
            ?: info.id
        return DeviceAppInfo(
            packageName = key,
            label = label,
            firstLetter = label.firstOrNull()?.uppercaseChar()?.let { if (it in 'A'..'Z') it else '#' } ?: '#',
            version = "",
            iconBitmap = bitmap,
            glyphBitmap = null,
            isRealApp = false,
            componentName = info.activity,
            userHandle = Process.myUserHandle(),
            shortcutId = info.id,
            shortcutPackage = info.`package`
        )
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val size = 144
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val bmp = drawable.bitmap
            return if (bmp.width <= size && bmp.height <= size) bmp
            else bmp.scale(size, size)
        }
        val w = if (drawable.intrinsicWidth in 1..size) drawable.intrinsicWidth else size
        val h = if (drawable.intrinsicHeight in 1..size) drawable.intrinsicHeight else size
        val bmp = createBitmap(w, h)
        drawable.setBounds(0, 0, w, h)
        drawable.draw(Canvas(bmp))
        return bmp
    }
}
