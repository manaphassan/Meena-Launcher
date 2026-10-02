package com.example.meenalauncher.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.meenalauncher.data.news.NewsArticle
import com.example.meenalauncher.data.news.NewsRssRepository
import com.example.meenalauncher.theme.MeenaBorder
import com.example.meenalauncher.theme.MeenaSurface
import com.example.meenalauncher.theme.MeenaSurfaceElevated
import com.example.meenalauncher.theme.MeenaTextMuted
import com.example.meenalauncher.theme.MeenaTextSecondary
import com.example.meenalauncher.theme.MeenaTextWhite

/**
 * Authentic Windows Phone / Metro style widget displaying the 6 latest local news
 * fetched in real-time from Amanz.my and SuamiSihat.com.my RSS feeds.
 */
@Composable
fun NewsFeedWidget(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val newsArticles by NewsRssRepository.newsFlow.collectAsState()
    val isLoading by NewsRssRepository.isLoadingFlow.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "AMANZ", "SUAMI SIHAT"

    val filteredList = remember(newsArticles, selectedFilter) {
        when (selectedFilter) {
            "AMANZ" -> newsArticles.filter { it.source.equals("Amanz", ignoreCase = true) }.take(6)
            "SUAMI SIHAT" -> newsArticles.filter { it.source.equals("Suami Sihat", ignoreCase = true) }.take(6)
            else -> newsArticles.take(6)
        }
    }

    // Refresh spinner animation - only runs when actively fetching feeds
    val refreshRotation = remember { Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(isLoading) {
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

    CollapsibleWidget(
        title = "local news • 6 latest",
        collapsedSummary = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color(0xFF00A4EF), RoundedCornerShape(0.dp))
                )
                Text(
                    text = "Amanz & Suami Sihat • ${newsArticles.size} articles",
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted
                )
            }
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Filter Chips & Refresh Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NewsFilterChip(
                        label = "ALL (6)",
                        isSelected = selectedFilter == "ALL",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedFilter = "ALL"
                        }
                    )
                    NewsFilterChip(
                        label = "AMANZ",
                        isSelected = selectedFilter == "AMANZ",
                        accentColor = Color(0xFF00A4EF),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedFilter = "AMANZ"
                        }
                    )
                    NewsFilterChip(
                        label = "SUAMI SIHAT",
                        isSelected = selectedFilter == "SUAMI SIHAT",
                        accentColor = Color(0xFF107C10),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedFilter = "SUAMI SIHAT"
                        }
                    )
                }

                // Refresh button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            NewsRssRepository.refresh(force = true)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Feeds",
                        tint = if (isLoading) MaterialTheme.colorScheme.primary else MeenaTextMuted,
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer {
                                if (isLoading) {
                                    rotationZ = refreshRotation.value
                                }
                            }
                    )
                }
            }

            // Article List (6 items)
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MeenaSurface)
                        .border(1.dp, MeenaBorder)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isLoading) "Loading latest headlines..." else "No articles found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MeenaTextMuted
                    )
                }
            } else {
                filteredList.forEach { article ->
                    NewsArticleRow(
                        article = article,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            NewsRssRepository.openArticle(context, article)
                        }
                    )
                }
            }

            // Footer note
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RSS feeds: amanz.my • suamisihat.com.my",
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted,
                    fontSize = 10.sp
                )
                Text(
                    text = "Tap article to read",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun NewsArticleRow(
    article: NewsArticle,
    onClick: () -> Unit
) {
    val sourceColor = when {
        article.source.contains("amanz", ignoreCase = true) -> Color(0xFF00A4EF)
        else -> Color(0xFF107C10)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MeenaSurface)
            .border(1.dp, MeenaBorder)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Left Accent Source Indicator Bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
                .background(sourceColor)
        )

        // Article Content
        Column(modifier = Modifier.weight(1f)) {
            // Badges: Source + Category + Time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Source Tag
                Box(
                    modifier = Modifier
                        .background(sourceColor.copy(alpha = 0.2f))
                        .border(1.dp, sourceColor.copy(alpha = 0.6f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = article.source.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = sourceColor
                    )
                }

                // Category Tag
                if (article.category.isNotBlank()) {
                    Text(
                        text = "• ${article.category}",
                        fontSize = 10.sp,
                        color = MeenaTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Time
                Text(
                    text = article.formattedDate,
                    fontSize = 10.sp,
                    color = MeenaTextMuted
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Headline
            Text(
                text = article.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MeenaTextWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            // Short Description snippet if available
            if (article.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = article.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }
        }

        // Open in browser indicator icon
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = "Open in browser",
            tint = MeenaTextMuted,
            modifier = Modifier
                .size(14.dp)
                .padding(top = 2.dp)
        )
    }
}

@Composable
private fun NewsFilterChip(
    label: String,
    isSelected: Boolean,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(if (isSelected) accentColor else MeenaSurfaceElevated)
            .border(1.dp, if (isSelected) accentColor else MeenaBorder)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else MeenaTextMuted
        )
    }
}
