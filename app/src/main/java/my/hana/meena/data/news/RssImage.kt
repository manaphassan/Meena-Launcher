package my.hana.meena.data.news

/** Picks one thumbnail address for a feed item, in the order media:thumbnail, media:content, enclosure, body. */
object RssImage {

    /** media:content / enclosure count as images when medium="image" or the type starts with "image/". */
    fun isImage(medium: String?, type: String?): Boolean =
        medium?.trim().equals("image", ignoreCase = true) ||
            type?.trim()?.startsWith("image/", ignoreCase = true) == true

    /** First https candidate wins; HTML is only scanned when the feed gave no media address. */
    fun pick(
        mediaThumbnail: String?,
        mediaContent: String?,
        enclosure: String?,
        contentHtml: String?,
        descriptionHtml: String?
    ): String? =
        ArticleHtml.safeImageUrl(mediaThumbnail)
            ?: ArticleHtml.safeImageUrl(mediaContent)
            ?: ArticleHtml.safeImageUrl(enclosure)
            ?: contentHtml?.let { ArticleHtml.firstImageSrc(it) }
            ?: descriptionHtml?.let { ArticleHtml.firstImageSrc(it) }
}
