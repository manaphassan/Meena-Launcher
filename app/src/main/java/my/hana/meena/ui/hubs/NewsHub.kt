package my.hana.meena.ui.hubs

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import my.hana.meena.R
import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.data.news.NewsArticle
import my.hana.meena.data.news.NewsMerge
import my.hana.meena.data.news.NewsRssRepository
import my.hana.meena.theme.MeenaSpacing
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextWhite
import my.hana.meena.ui.components.metroClickable
import my.hana.meena.ui.components.metroFeatherIn
import my.hana.meena.ui.news.NewsImageState
import my.hana.meena.ui.news.NewsReader
import my.hana.meena.ui.news.newsAgeText
import my.hana.meena.ui.news.rememberNewsImage
import my.hana.meena.ui.news.rowMetaParts
import my.hana.meena.ui.widgets.WidgetState
import my.hana.meena.ui.widgets.rememberNow
import my.hana.meena.ui.widgets.updatedAgoText

/** Filter key for "every source"; empty so it can't clash with a feed called "all". */
private const val FILTER_ALL = ""
/** Stories shown when the user hasn't picked a count in settings. */
internal const val DEFAULT_VISIBLE_STORIES = 20
/** In the "all" view each source keeps at least this many places, so a busy feed can't hide a quiet one. */
private const val MIN_VISIBLE_PER_SOURCE = 3
/** Only the first screenful feathers in; later rows appear without a delay while scrolling. */
private const val FEATHER_ROWS = 8
private val ThumbSize = 72.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewsHub(
    settings: MeenaUserSettings,
    listState: LazyListState
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val newsState by NewsRssRepository.newsFlow.collectAsState()
    val isRefreshing by NewsRssRepository.isRefreshing.collectAsState()
    val newsArticles = (newsState as? WidgetState.Ready)?.data
        ?: (newsState as? WidgetState.Stale)?.data
        ?: emptyList()
    val staleSince = (newsState as? WidgetState.Stale)?.updatedAt

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(FILTER_ALL) }
    var reading by remember { mutableStateOf<NewsArticle?>(null) }

    // Saved feeds must reach the fetcher at start, not only when edited in settings.
    LaunchedEffect(settings.newsFeeds) { NewsRssRepository.updateFeeds(settings.newsFeeds) }

    val feedNames = remember(settings.newsFeeds) {
        NewsRssRepository.effectiveFeeds(settings.newsFeeds)
            .map { NewsRssRepository.sourceNameFor(it).lowercase() }
            .distinct()
    }
    // Filters follow the feeds that actually returned articles, not a fixed list.
    val sourceNames = remember(newsArticles) { newsArticles.map { it.source.lowercase() }.distinct() }
    val activeFilter = if (selectedFilter in sourceNames) selectedFilter else FILTER_ALL

    val storyLimit = settings.widgetConfigs["widget-news-feed"]?.customSettings?.get("max_stories")
        ?.toIntOrNull()?.coerceIn(1, NewsRssRepository.MAX_ARTICLES)
        ?: DEFAULT_VISIBLE_STORIES

    val shownArticles = remember(newsArticles, activeFilter, searchQuery, storyLimit) {
        val bySource = if (activeFilter == FILTER_ALL) newsArticles
        else newsArticles.filter { it.source.equals(activeFilter, ignoreCase = true) }
        val matched = if (searchQuery.isBlank()) bySource else bySource.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
        }
        NewsMerge.limitFair(matched, storyLimit, MIN_VISIBLE_PER_SOURCE)
    }

    val nowMs = rememberNow()
    val separator = stringResource(R.string.news_meta_separator)

    reading?.let { article -> NewsReader(article = article, onClose = { reading = null }) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MeenaSpacing.l),
        verticalArrangement = Arrangement.spacedBy(MeenaSpacing.m)
    ) {
        val newsWidgets = settings.widgetConfigs.entries
            .filter { it.value.hubId == "news" && it.value.isEnabled }
            .sortedBy { it.value.position }

        newsWidgets.forEach { widget ->
            when (widget.key) {
                "widget-news-feed" -> {
                    // 1. header: sources and refresh (the pager already shows the big "news" title)
                    item(key = "news-header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MeenaSpacing.m),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.news_from, feedNames.joinToString(", ")),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MeenaTextMuted
                                )
                                if (staleSince != null) {
                                    Text(
                                        text = updatedAgoText(staleSince),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextMuted
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .widthIn(min = 48.dp)
                                    .metroClickable(enabled = !isRefreshing) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        scope.launch { NewsRssRepository.refresh(force = true) }
                                    },
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text(
                                    text = stringResource(R.string.news_refresh),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isRefreshing) MeenaTextMuted else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // 2. search: plain text with a thin muted underline, no box
                    item(key = "news-search") {
                        NewsSearchField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it }
                        )
                    }

                    // 3. source pivots (Zune style), only when there is something to choose between
                    if (sourceNames.size > 1) {
                        item(key = "news-filter") {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(MeenaSpacing.l)
                            ) {
                                NewsPivot(
                                    label = stringResource(R.string.news_filter_all),
                                    selected = activeFilter == FILTER_ALL,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedFilter = FILTER_ALL
                                    }
                                )
                                sourceNames.forEach { name ->
                                    NewsPivot(
                                        label = name,
                                        selected = activeFilter == name,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedFilter = name
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 4. articles
                    if (shownArticles.isEmpty()) {
                        item(key = "news-empty") {
                            val message = when {
                                newsArticles.isNotEmpty() && searchQuery.isNotBlank() ->
                                    stringResource(R.string.news_empty_query, searchQuery)
                                newsArticles.isNotEmpty() || newsState is WidgetState.Empty ->
                                    stringResource(R.string.news_empty_stories)
                                newsState is WidgetState.Error -> stringResource(R.string.news_error)
                                else -> stringResource(R.string.news_loading)
                            }
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MeenaTextMuted,
                                modifier = Modifier.padding(vertical = MeenaSpacing.l)
                            )
                        }
                    } else {
                        itemsIndexed(shownArticles, key = { _, article -> "article-${article.id}" }) { index, article ->
                            NewsRow(
                                article = article,
                                nowMs = nowMs,
                                separator = separator,
                                modifier = if (index < FEATHER_ROWS) {
                                    Modifier.metroFeatherIn(index = index, reducedMotion = settings.reducedMotion)
                                } else {
                                    Modifier
                                },
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    reading = article
                                }
                            )
                        }
                    }
                }
            }
        }

        item(key = "news-bottom-space") { Spacer(modifier = Modifier.height(64.dp)) }
    }
}

@Composable
private fun NewsSearchField(value: String, onValueChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    val lineColor = MeenaTextMuted
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = MeenaTextWhite)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .drawBehind {
                val stroke = 1.dp.toPx()
                val y = size.height - stroke / 2
                drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
            },
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.news_search_hint),
                        style = textStyle.copy(color = MeenaTextMuted)
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun NewsPivot(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .metroClickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = label.lowercase(),
            style = MaterialTheme.typography.headlineSmall,
            color = if (selected) MeenaTextWhite else MeenaTextMuted
        )
    }
}

@Composable
private fun NewsRow(
    article: NewsArticle,
    nowMs: Long,
    separator: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val thumbPx = with(LocalDensity.current) { ThumbSize.roundToPx() }
    val thumb = rememberNewsImage(article.imageUrl, thumbPx, fillSquare = true)
    val meta = rowMetaParts(article, newsAgeText(article.pubDateMs, nowMs)).joinToString(separator)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .metroClickable(onClick = onClick)
            .padding(vertical = MeenaSpacing.s),
        horizontalArrangement = Arrangement.spacedBy(MeenaSpacing.m),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MeenaSpacing.s)
        ) {
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                color = MeenaTextWhite,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        // Space is kept only while the picture loads; no address or a failed load gives the title the full width.
        when (thumb) {
            NewsImageState.Loading -> if (article.imageUrl != null) Spacer(modifier = Modifier.size(ThumbSize))
            is NewsImageState.Ready -> Image(
                bitmap = thumb.image,
                contentDescription = null,
                modifier = Modifier.size(ThumbSize),
                contentScale = ContentScale.Crop
            )
            NewsImageState.Failed -> Unit
        }
    }
}
