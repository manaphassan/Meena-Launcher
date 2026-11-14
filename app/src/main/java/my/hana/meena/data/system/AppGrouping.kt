package my.hana.meena.data.system

/** One block of the apps page: a header and its apps. [letter] is set only for a to z blocks. */
data class AppSection(val key: String, val title: String, val letter: Char?, val apps: List<DeviceAppInfo>)

/** Lays the apps page out a to z, by category, or by when each app was last opened from Meena. */
object AppGrouping {
    const val BUCKET_TODAY = "today"
    const val BUCKET_WEEK = "week"
    const val BUCKET_EARLIER = "earlier"
    const val BUCKET_NEVER = "never"

    private const val WEEK_MS = 7 * 24 * 60 * 60 * 1000L

    private val byLabel = compareBy<DeviceAppInfo> { it.label.lowercase() }

    fun byLetter(apps: List<DeviceAppInfo>): List<AppSection> =
        apps.groupBy { it.firstLetter }.toSortedMap().map { (letter, list) ->
            AppSection("letter-$letter", letter.lowercase(), letter, list.sortedWith(byLabel))
        }

    /**
     * Groups by the category the app declares. [titleOf] gives the system's name for a category,
     * or null when the app has none; those go last under [otherTitle].
     */
    fun byCategory(apps: List<DeviceAppInfo>, titleOf: (Int) -> String?, otherTitle: String): List<AppSection> {
        val grouped = apps.groupBy { titleOf(it.category)?.trim()?.lowercase()?.ifEmpty { null } }
        val named = grouped.filterKeys { it != null }.toSortedMap(compareBy { it }).map { (title, list) ->
            AppSection("category-$title", title!!, null, list.sortedWith(byLabel))
        }
        val other =
            grouped[null]?.let { listOf(AppSection("category-other", otherTitle, null, it.sortedWith(byLabel))) }
        return named + other.orEmpty()
    }

    /**
     * Newest first, in blocks: opened today, in the last 7 days, earlier, and never opened from
     * Meena (a to z). [lastUsed] is package to time; only launches from Meena are known.
     */
    fun byRecent(
        apps: List<DeviceAppInfo>,
        lastUsed: Map<String, Long>,
        nowMillis: Long,
        startOfTodayMillis: Long,
        titleOf: (String) -> String
    ): List<AppSection> {
        fun bucketOf(app: DeviceAppInfo): String {
            val last = lastUsed[app.packageName] ?: return BUCKET_NEVER
            return when {
                last >= startOfTodayMillis -> BUCKET_TODAY
                last >= nowMillis - WEEK_MS -> BUCKET_WEEK
                else -> BUCKET_EARLIER
            }
        }

        val grouped = apps.groupBy(::bucketOf)
        return listOf(BUCKET_TODAY, BUCKET_WEEK, BUCKET_EARLIER, BUCKET_NEVER).mapNotNull { bucket ->
            val list = grouped[bucket] ?: return@mapNotNull null
            val sorted = if (bucket == BUCKET_NEVER) list.sortedWith(byLabel)
            else list.sortedWith(compareByDescending<DeviceAppInfo> { lastUsed[it.packageName] ?: 0L }.then(byLabel))
            AppSection("recent-$bucket", titleOf(bucket), null, sorted)
        }
    }
}
