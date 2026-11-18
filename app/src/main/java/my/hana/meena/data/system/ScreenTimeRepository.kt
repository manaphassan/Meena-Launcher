package my.hana.meena.data.system

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.util.Calendar

data class AppUsage(val packageName: String, val label: String, val minutes: Long)

/** Today's screen time: the total, and the apps used most. */
data class ScreenTimeToday(val totalMinutes: Long, val topApps: List<AppUsage>)

/**
 * Works out how long each app was on screen from Android's usage events, so the numbers match
 * "since midnight" exactly instead of whole daily buckets. Pure Kotlin, so it is unit tested.
 */
object ScreenTimeCalc {

    enum class Kind { START, STOP, SCREEN_OFF }

    data class Event(val packageName: String, val className: String, val kind: Kind, val timeMillis: Long)

    /**
     * Milliseconds each package was in front between [start] and [end]. An app counts as in front
     * while any of its activities is resumed. An app already in front at [start] (its first event
     * is a stop) counts from [start]; one still in front at [end] counts up to [end].
     */
    fun foregroundMillis(events: List<Event>, start: Long, end: Long, ignore: Set<String> = emptySet()): Map<String, Long> {
        val open = HashMap<String, MutableSet<String>>()
        val since = HashMap<String, Long>()
        val seen = HashSet<String>()
        val total = HashMap<String, Long>()

        fun close(pkg: String, at: Long) {
            val from = since.remove(pkg) ?: return
            val span = (minOf(at, end) - maxOf(from, start)).coerceAtLeast(0)
            if (span > 0) total[pkg] = (total[pkg] ?: 0L) + span
        }

        for (e in events.sortedBy { it.timeMillis }) {
            if (e.kind == Kind.SCREEN_OFF) {
                for (pkg in open.keys.toList()) close(pkg, e.timeMillis)
                open.clear()
                continue
            }
            val pkg = e.packageName
            if (pkg in ignore) continue
            val activities = open.getOrPut(pkg) { mutableSetOf() }
            when (e.kind) {
                Kind.START -> {
                    if (activities.isEmpty()) since[pkg] = e.timeMillis
                    activities += e.className
                }
                Kind.STOP -> {
                    if (pkg !in seen && activities.isEmpty()) {
                        // In front since before the window opened.
                        since[pkg] = start
                        activities += e.className
                    }
                    activities -= e.className
                    if (activities.isEmpty()) close(pkg, e.timeMillis)
                }
                Kind.SCREEN_OFF -> Unit
            }
            seen += pkg
        }
        for (pkg in since.keys.toList()) close(pkg, end)
        return total
    }
}

object ScreenTimeRepository {

    /** True once the person has turned on usage access for Meena in Android's settings. */
    fun hasAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return when (mode) {
            AppOpsManager.MODE_ALLOWED -> true
            AppOpsManager.MODE_DEFAULT ->
                context.checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) ==
                    PackageManager.PERMISSION_GRANTED
            else -> false
        }
    }

    fun openAccessSettings(context: Context) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /** Reads today's usage events. Call off the main thread. Null when Android won't say. */
    fun today(context: Context, maxApps: Int = 8): ScreenTimeToday? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = System.currentTimeMillis()
        val raw = runCatching { usm.queryEvents(start, end) }.getOrNull() ?: return null

        val events = ArrayList<ScreenTimeCalc.Event>()
        val event = UsageEvents.Event()
        while (raw.hasNextEvent()) {
            raw.getNextEvent(event)
            val kind = kindOf(event.eventType) ?: continue
            events += ScreenTimeCalc.Event(event.packageName.orEmpty(), event.className.orEmpty(), kind, event.timeStamp)
        }

        // The home screen itself isn't app time.
        val millis = ScreenTimeCalc.foregroundMillis(events, start, end, ignore = setOf(context.packageName))
        val apps = millis.entries
            .filter { it.value >= 60_000L }
            .sortedByDescending { it.value }
        return ScreenTimeToday(
            totalMinutes = millis.values.sum() / 60_000L,
            topApps = apps.take(maxApps).map { (pkg, ms) ->
                val label = InstalledAppsRepository.getCachedApp(pkg)?.label ?: pkg.substringAfterLast('.')
                AppUsage(pkg, label, ms / 60_000L)
            }
        )
    }

    @Suppress("DEPRECATION")
    private fun kindOf(type: Int): ScreenTimeCalc.Kind? = when (type) {
        UsageEvents.Event.MOVE_TO_FOREGROUND -> ScreenTimeCalc.Kind.START // ACTIVITY_RESUMED on Android 10+
        UsageEvents.Event.MOVE_TO_BACKGROUND -> ScreenTimeCalc.Kind.STOP // ACTIVITY_PAUSED on Android 10+
        UsageEvents.Event.SCREEN_NON_INTERACTIVE, 26 /* DEVICE_SHUTDOWN, Android 9+ */ -> ScreenTimeCalc.Kind.SCREEN_OFF
        else -> null
    }
}
