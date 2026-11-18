package my.hana.meena.data.system

/** One favourite slot on Today: a single app, or a group of up to four small tiles. */
sealed interface FavouriteSlot<T> {
    val id: String

    data class App<T>(override val id: String, val app: T) : FavouriteSlot<T>

    /** [apps] are the members that are still installed, in the group's order. */
    data class Group<T>(override val id: String, val apps: List<T>) : FavouriteSlot<T>
}

/**
 * Today shows at most [MAX] favourite slots, in two rows of [PER_ROW]. A slot is either one app
 * or a group of up to [GROUP_MAX] apps, saved in the same list as "group:pkg1|pkg2|...", so old
 * settings and backups read as before.
 */
object FavouriteRules {
    const val MAX = 8
    const val PER_ROW = 3
    const val PER_ROW_SMALL = 4
    const val GROUP_MAX = 4
    private const val GROUP_PREFIX = "group:"
    private const val SEPARATOR = "|"
    /** Shortcut entries are stored as "shortcut:packageName:shortcutId". */
    private const val SHORTCUT_PREFIX = "shortcut:"

    fun isGroup(id: String): Boolean = id.startsWith(GROUP_PREFIX)
    fun isShortcut(id: String): Boolean = id.startsWith(SHORTCUT_PREFIX)

    /** Builds a shortcut key for storage in pinnedAppIds. */
    fun shortcutKey(packageName: String, shortcutId: String): String =
        "$SHORTCUT_PREFIX$packageName:$shortcutId"

    /** Returns (packageName, shortcutId) or null when [id] is not a shortcut key. */
    fun parseShortcut(id: String): Pair<String, String>? {
        if (!isShortcut(id)) return null
        val rest = id.removePrefix(SHORTCUT_PREFIX)
        val colon = rest.indexOf(':')
        if (colon <= 0 || colon == rest.lastIndex) return null
        return rest.substring(0, colon) to rest.substring(colon + 1)
    }

    fun members(id: String): List<String> =
        if (!isGroup(id)) listOf(id) else id.removePrefix(GROUP_PREFIX).split(SEPARATOR).filter { it.isNotBlank() }

    fun groupId(members: List<String>): String = GROUP_PREFIX + members.distinct().take(GROUP_MAX).joinToString(SEPARATOR)

    /** Every app in favourites, groups opened up. */
    fun allPackages(pinned: List<String>): List<String> = pinned.flatMap(::members)

    fun contains(pinned: List<String>, pkg: String): Boolean = pinned.any { pkg in members(it) }

    /**
     * Splits tiles into rows of [perRow] cells, keeping their order. A wide tile takes two cells;
     * if it doesn't fit at the end of a row it starts the next one, leaving a gap. [cells] gives
     * each tile's width in cells (1 or 2). Returns the tile indices on each row.
     */
    fun packRows(cells: List<Int>, perRow: Int): List<List<Int>> {
        val rows = mutableListOf<MutableList<Int>>()
        var used = perRow
        cells.forEachIndexed { index, wanted ->
            val width = wanted.coerceIn(1, perRow)
            if (used + width > perRow) {
                rows += mutableListOf<Int>()
                used = 0
            }
            rows.last() += index
            used += width
        }
        return rows
    }

    /** Slots that still have an installed app, in the user's order, capped at [MAX]. */
    fun <T : Any> visible(pinned: List<String>, lookup: (String) -> T?): List<FavouriteSlot<T>> =
        pinned.mapNotNull { id ->
            if (isGroup(id)) {
                members(id).mapNotNull(lookup).takeIf { it.isNotEmpty() }?.let { FavouriteSlot.Group(id, it) }
            } else {
                lookup(id)?.let { FavouriteSlot.App(id, it) }
            }
        }.take(MAX)

    /**
     * The list after tapping [pkg] from the apps page, or null when it would be one slot too many.
     * Unpinning always works, from a group too; slots with nothing installed don't count.
     */
    fun toggle(pinned: List<String>, pkg: String, isInstalled: (String) -> Boolean = { true }): List<String>? =
        when {
            contains(pinned, pkg) -> remove(pinned, pkg)
            pinned.count { id -> members(id).any(isInstalled) } >= MAX -> null
            else -> pinned + pkg
        }

    /** Takes [pkg] out wherever it is; a group left with one app becomes a plain tile, with none it goes. */
    fun remove(pinned: List<String>, pkg: String): List<String> = pinned.mapNotNull { id ->
        when {
            !isGroup(id) -> id.takeIf { it != pkg }
            else -> {
                val left = members(id) - pkg
                when (left.size) {
                    0 -> null
                    1 -> left.single()
                    else -> groupId(left)
                }
            }
        }
    }

    /** Turns the single app at [index] into a group holding just it, ready for more. */
    fun makeGroup(pinned: List<String>, index: Int): List<String> {
        val id = pinned.getOrNull(index) ?: return pinned
        if (isGroup(id)) return pinned
        return pinned.toMutableList().also { it[index] = groupId(listOf(id)) }
    }

    /**
     * Adds [pkg] to the group at [index]. If it was already a favourite elsewhere it moves in, so
     * an app is only ever in one place. Null when the group is full.
     */
    fun addToGroup(pinned: List<String>, index: Int, pkg: String): List<String>? {
        val id = pinned.getOrNull(index)?.takeIf(::isGroup) ?: return null
        val current = members(id)
        if (pkg in current) return pinned
        if (current.size >= GROUP_MAX) return null
        val updated = pinned.toMutableList().also { it[index] = groupId(current + pkg) }
        // Drop it from anywhere else, but leave this group alone.
        return updated.mapIndexedNotNull { i, other ->
            if (i == index) other else remove(listOf(other), pkg).singleOrNull()
        }
    }
}
