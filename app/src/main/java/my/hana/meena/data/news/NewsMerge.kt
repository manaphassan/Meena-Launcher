package my.hana.meena.data.news

/** Merging and trimming article lists so one busy feed can't push the others off the page. */
object NewsMerge {

    /** Newest first, duplicates removed, at most [max] items, and each source keeps up to [minPerSource] of its newest. */
    fun merge(articles: List<NewsArticle>, max: Int, minPerSource: Int): List<NewsArticle> {
        val unique = articles
            .distinctBy { it.link.ifBlank { it.title } }
            .distinctBy { it.id }
            .sortedByDescending { it.pubDateMs }
        return limitFair(unique, max, minPerSource)
    }

    /**
     * Keeps [sortedNewestFirst] order but guarantees each source up to [minPerSource] places
     * before the rest fill the remaining room up to [max].
     */
    fun limitFair(sortedNewestFirst: List<NewsArticle>, max: Int, minPerSource: Int): List<NewsArticle> {
        if (max <= 0) return emptyList()
        if (sortedNewestFirst.size <= max) return sortedNewestFirst
        val picked = LinkedHashSet<NewsArticle>()
        val perSource = sortedNewestFirst.groupBy { it.source }
        val sourceCount = perSource.size.coerceAtLeast(1)
        val guaranteed = minPerSource.coerceAtMost(max / sourceCount).coerceAtLeast(0)
        perSource.values.forEach { group -> picked.addAll(group.take(guaranteed)) }
        for (article in sortedNewestFirst) {
            if (picked.size >= max) break
            picked.add(article)
        }
        return sortedNewestFirst.filter { it in picked }
    }
}
