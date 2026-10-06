package my.hana.meena.ui.hubs

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import my.hana.meena.ui.components.metroClickable
import my.hana.meena.ui.components.metroFeatherIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.data.news.NewsArticle
import my.hana.meena.data.news.NewsRssRepository
import my.hana.meena.theme.MeenaBorder
import my.hana.meena.theme.MeenaProfitGreen
import my.hana.meena.theme.MeenaSurface
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextSecondary
import my.hana.meena.theme.MeenaTextWhite
import my.hana.meena.ui.components.CollapsibleWidget

@Composable
fun NewsHub(
    settings: MeenaUserSettings,
    listState: LazyListState
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val newsState by NewsRssRepository.newsFlow.collectAsState()
    val isLoading = newsState is my.hana.meena.ui.widgets.WidgetState.Loading
    val newsArticles = (newsState as? my.hana.meena.ui.widgets.WidgetState.Ready)?.data 
        ?: (newsState as? my.hana.meena.ui.widgets.WidgetState.Stale)?.data 
        ?: emptyList()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("all") } // "all", "amanz", "suami sihat"

    val filteredList = remember(newsArticles, selectedFilter, searchQuery) {
        val bySource = when (selectedFilter) {
            "amanz" -> newsArticles.filter { it.source.equals("Amanz", ignoreCase = true) }
            "suami sihat" -> newsArticles.filter { it.source.equals("Suami Sihat", ignoreCase = true) }
            else -> newsArticles
        }
        if (searchQuery.isNotBlank()) {
            bySource.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
            }
        } else {
            bySource
        }.take(6)
    }

    // Refresh spinner animation - only runs when actively fetching
    val refreshRotation = remember { Animatable(0f) }
    LaunchedEffect(isLoading) {
        if (isLoading) {
            refreshRotation.animateTo(
                targetValue = refreshRotation.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            refreshRotation.snapTo(0f)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = my.hana.meena.theme.MeenaSpacing.l),
        verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)
    ) {
        val newsWidgets = settings.widgetConfigs.entries
            .filter { it.value.hubId == "news" && it.value.isEnabled }
            .sortedBy { it.value.position }

        items(newsWidgets.size, key = { newsWidgets[it].key }) { index ->
            val widgetId = newsWidgets[index].key
            when (widgetId) {
                "widget-news-feed" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)) {
                        // 1. TOP HEADER & SEARCH
                        Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "local news · rss feeds",
                                        style = MaterialTheme.typography.displaySmall,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MeenaTextWhite
                                    )
                                    Text(
                                        text = "latest articles from amanz & suami sihat",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextMuted
                                    )
                                }

                                // Refresh Button (pure Metro 0dp, min 48dp)
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .metroClickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            kotlinx.coroutines.MainScope().launch {
                                                NewsRssRepository.refresh()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "refresh",
                                        tint = if (isLoading) MaterialTheme.colorScheme.primary else MeenaTextWhite,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .graphicsLayer {
                                                if (isLoading) {
                                                    rotationZ = refreshRotation.value
                                                }
                                            }
                                    )
                                }
                            }

                            // Search field
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("search articles", color = MeenaTextMuted, fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MeenaSurface,
                                    unfocusedContainerColor = MeenaSurface,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MeenaBorder,
                                    focusedTextColor = MeenaTextWhite,
                                    unfocusedTextColor = MeenaTextWhite
                                )
                            )

                            // Source Filter Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
                            ) {
                                NewsFilterPill(
                                    label = "all",
                                    isSelected = selectedFilter == "all",
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedFilter = "all"
                                    }
                                )
                                NewsFilterPill(
                                    label = "amanz",
                                    isSelected = selectedFilter == "amanz",
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedFilter = "amanz"
                                    }
                                )
                                NewsFilterPill(
                                    label = "suami sihat",
                                    isSelected = selectedFilter == "suami sihat",
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedFilter = "suami sihat"
                                    }
                                )
                            }
                        }

                        // 2. RSS FEED ARTICLE LIST (LATEST 6)
                        if (filteredList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(my.hana.meena.theme.MeenaSpacing.l),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) "no articles matching \"$searchQuery\"" else "fetching live feeds",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MeenaTextMuted
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)) {
                                filteredList.forEachIndexed { index, article ->
                                    NewsArticleCard(
                                        modifier = Modifier.metroFeatherIn(
                                            index = index,
                                            reducedMotion = settings.reducedMotion
                                        ),
                                        article = article,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.link)).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        }
                                    )
                                }
                            }
                        }

                        // 3. RSS FEED SOURCE MONITOR
                        CollapsibleWidget(title = "feed sources · live status") {
                            Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
                                FeedSourceStatusRow("Amanz.my", "https://amanz.my/feed/", MaterialTheme.colorScheme.primary)
                                FeedSourceStatusRow("SuamiSihat.com.my", "https://suamisihat.com.my/feed/", MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(64.dp)) }
    }
}

@Composable
private fun NewsArticleCard(
    modifier: Modifier = Modifier,
    article: NewsArticle,
    onClick: () -> Unit
) {
    val badgeColor = try {
        Color(android.graphics.Color.parseColor(article.sourceBadgeColorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .metroClickable(onClick = onClick)
            .padding(vertical = my.hana.meena.theme.MeenaSpacing.s)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
            // Source badge + time row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
                ) {
                    Box(
                        modifier = Modifier
                            .background(badgeColor, RoundedCornerShape(0.dp))
                            .padding(horizontal = my.hana.meena.theme.MeenaSpacing.s, vertical = 0.dp)
                    ) {
                        Text(
                            text = article.source.lowercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MeenaTextWhite
                        )
                    }

                    if (article.category.isNotBlank()) {
                        Text(
                            text = "· ${article.category.lowercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MeenaTextMuted
                        )
                    }
                }

                Text(
                    text = article.formattedDate.lowercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted
                )
            }

            // Headline
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MeenaTextWhite,
                lineHeight = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Snippet Description
            if (article.description.isNotBlank()) {
                Text(
                    text = article.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextSecondary,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Read article prompt
            Text(
                text = "read article",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = badgeColor,
                modifier = Modifier.padding(top = 0.dp)
            )
        }
    }
}

@Composable
private fun NewsFilterPill(
    label: String,
    isSelected: Boolean,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .background(
                if (isSelected) accentColor else MaterialTheme.colorScheme.primary,
                RoundedCornerShape(0.dp)
            )
            .metroClickable(onClick = onClick)
            .padding(horizontal = my.hana.meena.theme.MeenaSpacing.m, vertical = my.hana.meena.theme.MeenaSpacing.s),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.lowercase(),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Medium,
            color = if (isSelected) MeenaTextWhite else MeenaTextMuted
        )
    }
}

@Composable
private fun FeedSourceStatusRow(name: String, url: String, badgeColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MeenaProfitGreen, RoundedCornerShape(0.dp))
            )
            Column {
                Text(name.lowercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MeenaTextWhite)
                Text(url.lowercase(), style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted, fontSize = 12.sp)
            }
        }
        Text("active", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MeenaProfitGreen)
    }
}

