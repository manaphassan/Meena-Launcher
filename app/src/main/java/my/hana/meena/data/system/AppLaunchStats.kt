package my.hana.meena.data.system

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Counts apps opened from Meena, for the frequent apps widget. Kept on the phone only,
 * never sent anywhere, and cleared with "clear app history". Uses no system usage data.
 */
object AppLaunchStats {
    private const val PREFS_NAME = "meena_app_launches"

    data class Entry(val count: Int, val lastUsed: Long)

    private val _version = MutableStateFlow(0)
    /** Bumps on every recorded launch or clear, so the widget can re-rank. */
    val version: StateFlow<Int> = _version.asStateFlow()

    fun record(context: Context, packageName: String, now: Long = System.currentTimeMillis()) {
        if (packageName == context.packageName) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = decode(prefs.getString(packageName, null))
        prefs.edit().putString(packageName, encode(Entry((current?.count ?: 0) + 1, now))).apply()
        _version.update { it + 1 }
    }

    fun load(context: Context): Map<String, Entry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.all.mapNotNull { (key, value) -> decode(value as? String)?.let { key to it } }.toMap()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        _version.update { it + 1 }
    }

    /** Most used first, then most recent. Skips apps that are gone or already pinned. */
    fun rank(stats: Map<String, Entry>, installed: Set<String>, exclude: Set<String>, limit: Int): List<String> =
        stats.entries
            .filter { it.key in installed && it.key !in exclude }
            .sortedWith(compareByDescending<Map.Entry<String, Entry>> { it.value.count }.thenByDescending { it.value.lastUsed })
            .take(limit)
            .map { it.key }

    internal fun encode(entry: Entry): String = "${entry.count},${entry.lastUsed}"

    internal fun decode(raw: String?): Entry? {
        val parts = raw?.split(",") ?: return null
        val count = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val last = parts.getOrNull(1)?.toLongOrNull() ?: return null
        return Entry(count, last)
    }
}
