package my.hana.meena.data.model

/** Ways the apps page can be laid out. Unknown saved values fall back to [AZ]. */
object AppsSort {
    const val AZ = "az"
    const val CATEGORY = "category"
    const val RECENT = "recent"
    const val GRID = "grid"

    val ALL = listOf(AZ, CATEGORY, RECENT, GRID)

    fun of(saved: String): String = if (saved in ALL) saved else AZ
}
