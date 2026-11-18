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

/** Today's screen time: the total, the apps used most, and minutes in each hour since midnight (24 slots). */
data class ScreenTimeToday(
    val totalMinutes: Long,
    val topApps: List<AppUsage>,
    val minutesByHour: List<Int> = List(24) { 0 }
)

/**
 * Works out how long each app was on screen from Android's usage events, so the numbers match
 * "since midnight" exactly instead of whole daily buckets. Pure Kotlin, so it is unit tested.
 */
object ScreenTimeCalc {

    enum class Kind { START, STOP, SCREEN_OFF }

    data class Event(val packageName: String, val className: String, val kind: Kind, val timeMillis: Long)

    /** One stretch of an app being in front, clipped to the window asked for. */
    data class Session(val packageName: String, val fromMillis: Long, val toMillis: Long)

    private const val HOUR = 3_600_000L

    /** Milliseconds each package was in front between [start] and [end]. See [sessions]. */
    fun foregroundMillis(events: List<Event>, start: Long, end: Long, ignore: Set<String> = emptySet()): Map<String, Long> =
        sessions(events, start, end, ignore)
            .groupBy { it.packageName }
            .mapValues { (_, list) -> list.sumOf { it.toMillis - it.fromMillis } }

    /**
     * Minutes of screen time in each hour from [dayStart], for [hours] hours. A session that
     * crosses the hour is split between both. Hours after [end] stay at zero.
     */
    fun minutesByHour(sessions: List<Session>, dayStart: Long, hours: Int = 24): List<Int> {
        val millis = LongArray(hours)
        for (s in sessions) {
            var from = s.fromMillis
            while (from < s.toMillis) {
                val slot = ((from - dayStart) / HOUR).toInt()
                if (slot !in 0 until hours) break
                val slotEnd = dayStart + (slot + 1) * HOUR
                val to = minOf(s.toMillis, slotEnd)
                millis[slot] += to - from
                from = to
            }
        }
        return millis.map { (it / 60_000L).toInt().coerceAtMost(60) }
    }

    /**
     * Each stretch an app was in front between [start] and [end], counting one app at a time:
     * a new app coming to the front (or the home screen, or the screen turning off) ends the
     * previous one, even if Android never sent its pause. An app with several activities stays in
     * front until the last one pauses. Only the very first event of the window may be a pause for
     * an app that was already open at [start]; it then counts from [start]. An app still in front
     * at [end] counts up to [end].
     */
    fun sessions(events: List<Event>, start: Long, end: Long, ignore: Set<String> = emptySet()): List<Session> {
        val result = ArrayList<Session>()
        var current: String? = null
        var since = 0L
        val activities = mutableSetOf<String>()
        var first = true

        fun add(pkg: String, from: Long, to: Long) {
            val clippedFrom = maxOf(from, start)
            val clippedTo = minOf(to, end)
            if (clippedTo > clippedFrom) result += Session(pkg, clippedFrom, clippedTo)
        }

        fun closeCurrent(at: Long) {
            val pkg = current ?: return
            add(pkg, since, at)
            current = null
            activities.clear()
        }

        for (e in events.sortedBy { it.timeMillis }) {
            when (e.kind) {
                Kind.SCREEN_OFF -> closeCurrent(e.timeMillis)
                Kind.START -> {
                    if (current != e.packageName) {
                        closeCurrent(e.timeMillis)
                        if (e.packageName !in ignore) {
                            current = e.packageName
                            since = e.timeMillis
                        }
                    }
                    if (current == e.packageName) activities += e.className
                }
                Kind.STOP -> {
                    if (first && current == null && e.packageName !in ignore) {
                        // Open since before the window started.
                        add(e.packageName, start, e.timeMillis)
                    } else if (current == e.packageName) {
                        activities -= e.className
                        if (activities.isEmpty()) closeCurrent(e.timeMillis)
                    }
                }
            }
            if (e.kind != Kind.SCREEN_OFF) first = false
        }
        closeCurrent(end)
        return result
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
        val sessions = ScreenTimeCalc.sessions(events, start, end, ignore = setOf(context.packageName))
        val millis = sessions.groupBy { it.packageName }.mapValues { (_, list) -> list.sumOf { it.toMillis - it.fromMillis } }
        val apps = millis.entries
            .filter { it.value >= 60_000L }
            .sortedByDescending { it.value }
        return ScreenTimeToday(
            totalMinutes = millis.values.sum() / 60_000L,
            topApps = apps.take(maxApps).map { (pkg, ms) ->
                val label = InstalledAppsRepository.getCachedApp(pkg)?.label ?: pkg.substringAfterLast('.')
                AppUsage(pkg, label, ms / 60_000L)
            },
            minutesByHour = ScreenTimeCalc.minutesByHour(sessions, start)
        )
    }

    @Suppress("DEPRECATION")
    private fun kindOf(type: Int): ScreenTimeCalc.Kind? = when (type) {
        UsageEvents.Event.MOVE_TO_FOREGROUND -> ScreenTimeCalc.Kind.START // ACTIVITY_RESUMED on Android 10+
        UsageEvents.Event.MOVE_TO_BACKGROUND -> ScreenTimeCalc.Kind.STOP // ACTIVITY_PAUSED on Android 10+
        UsageEvents.Event.SCREEN_NON_INTERACTIVE,
        18 /* KEYGUARD_SHOWN, Android 9+: the lock screen is not app time */,
        26 /* DEVICE_SHUTDOWN, Android 9+ */ -> ScreenTimeCalc.Kind.SCREEN_OFF
        else -> null
    }
}
