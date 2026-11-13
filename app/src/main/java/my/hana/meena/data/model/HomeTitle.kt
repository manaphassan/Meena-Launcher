package my.hana.meena.data.model

/** The title at the top of the home pages: lowercase, one line, short. Empty means hidden. */
object HomeTitle {
    const val DEFAULT = "meena"
    const val MAX_LENGTH = 20

    /** Applied while typing, so the space between two words survives. */
    fun clean(input: String): String =
        input.replace(Regex("[\\r\\n\\t]"), " ").lowercase().take(MAX_LENGTH)

    /** What the home pages draw. */
    fun display(saved: String): String = saved.trim()
}
