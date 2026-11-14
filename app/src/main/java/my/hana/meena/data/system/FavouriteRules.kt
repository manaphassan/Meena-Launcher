package my.hana.meena.data.system

/** Today shows at most [MAX] favourite tiles, in two rows of [PER_ROW]. */
object FavouriteRules {
    const val MAX = 6
    const val PER_ROW = 3

    /** Pins that still resolve to an app, in the user's order, capped at [MAX]. */
    fun <T : Any> visible(pinned: List<String>, lookup: (String) -> T?): List<T> =
        pinned.mapNotNull(lookup).take(MAX)

    /**
     * The pin list after tapping [pkg], or null when it would be one tile too many.
     * Unpinning always works; pins for apps that are gone don't count towards the limit.
     */
    fun toggle(pinned: List<String>, pkg: String, isInstalled: (String) -> Boolean = { true }): List<String>? =
        when {
            pkg in pinned -> pinned - pkg
            pinned.count(isInstalled) >= MAX -> null
            else -> pinned + pkg
        }
}
