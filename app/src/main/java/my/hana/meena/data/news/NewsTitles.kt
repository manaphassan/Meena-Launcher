package my.hana.meena.data.news

/** Small text rules for feed items, kept free of Android so they can be unit-tested. */
object NewsTitles {
    /**
     * Google News titles end with " - Publisher" (for example "Tesla ... - Amanz"); the source is
     * already shown above the title, so drop the repeat. Other titles are returned unchanged.
     */
    fun stripPublisher(title: String, publisher: String?): String {
        val name = publisher?.trim().orEmpty()
        if (name.isEmpty()) return title
        for (dash in listOf(" - ", " – ", " — ")) {
            val suffix = dash + name
            if (title.endsWith(suffix, ignoreCase = true) && title.length > suffix.length) {
                return title.dropLast(suffix.length).trimEnd()
            }
        }
        return title
    }
}
