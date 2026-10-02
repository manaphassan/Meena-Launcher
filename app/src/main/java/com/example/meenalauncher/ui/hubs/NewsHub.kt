package com.example.meenalauncher.ui.hubs

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.example.meenalauncher.data.model.MeenaUserSettings
import com.example.meenalauncher.data.news.NewsArticle
import com.example.meenalauncher.data.news.NewsRssRepository
import com.example.meenalauncher.theme.MeenaBorder
import com.example.meenalauncher.theme.MeenaProfitGreen
import com.example.meenalauncher.theme.MeenaSurface
import com.example.meenalauncher.theme.MeenaTextMuted
import com.example.meenalauncher.theme.MeenaTextSecondary
import com.example.meenalauncher.theme.MeenaTextWhite
import com.example.meenalauncher.ui.components.CollapsibleWidget

@Composable
fun NewsHub(
    settings: MeenaUserSettings,
    listState: LazyListState
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val newsArticles by NewsRssRepository.newsFlow.collectAsState()
    val isLoading by NewsRssRepository.isLoadingFlow.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "AMANZ", "SUAMI SIHAT"

    val filteredList = remember(newsArticles, selectedFilter, searchQuery) {
        val bySource = when (selectedFilter) {
            "AMANZ" -> newsArticles.filter { it.source.equals("Amanz", ignoreCase = true) }
            "SUAMI SIHAT" -> newsArticles.filter { it.source.equals("Suami Sihat", ignoreCase = true) }
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
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // 1. TOP HEADER & SEARCH
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "local news • rss feeds",
                            style = MaterialTheme.typography.displaySmall,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Latest 6 real-time articles from Amanz.my & SuamiSihat.com.my",
                            style = MaterialTheme.typography.labelSmall,
                            color = MeenaTextMuted
                        )
                    }

                    // Refresh Button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(MeenaSurface, RoundedCornerShape(4.dp))
                            .border(1.dp, MeenaBorder, RoundedCornerShape(4.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                NewsRssRepository.refresh(force = true)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = if (isLoading) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier
                                .size(18.dp)
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
                    placeholder = { Text("Filter articles by keyword...", color = MeenaTextMuted, fontSize = 12.sp) },
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NewsFilterPill(
                        label = "ALL (6)",
                        isSelected = selectedFilter == "ALL",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedFilter = "ALL"
                        }
                    )
                    NewsFilterPill(
                        label = "AMANZ.MY",
                        isSelected = selectedFilter == "AMANZ",
                        accentColor = Color(0xFF00A4EF),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedFilter = "AMANZ"
                        }
                    )
                    NewsFilterPill(
                        label = "SUAMISIHAT.COM.MY",
                        isSelected = selectedFilter == "SUAMI SIHAT",
                        accentColor = Color(0xFF107C10),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedFilter = "SUAMI SIHAT"
                        }
                    )
                }
            }
        }

        // 2. RSS FEED ARTICLE LIST (LATEST 6)
        if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MeenaSurface)
                        .border(1.dp, MeenaBorder)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No articles matching \"$searchQuery\"" else "Fetching live feeds...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MeenaTextMuted
                    )
                }
            }
        } else {
            items(filteredList.size, key = { filteredList[it].id }) { idx ->
                val article = filteredList[idx]
                NewsArticleCard(
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

        // 3. RSS FEED SOURCE MONITOR
        item {
            CollapsibleWidget(title = "feed sources • live status") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FeedSourceStatusRow("Amanz.my", "https://amanz.my/feed/", Color(0xFF00A4EF))
                    FeedSourceStatusRow("SuamiSihat.com.my", "https://suamisihat.com.my/feed/", Color(0xFF107C10))
                }
            }
        }

        item { Spacer(modifier = Modifier.height(64.dp)) }
    }
}

@Composable
private fun NewsArticleCard(
    article: NewsArticle,
    onClick: () -> Unit
) {
    val badgeColor = try {
        Color(android.graphics.Color.parseColor(article.sourceBadgeColorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MeenaSurface)
            .border(1.dp, MeenaBorder)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Source badge + time row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(badgeColor, RoundedCornerShape(2.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = article.source.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (article.category.isNotBlank()) {
                        Text(
                            text = "• ${article.category}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MeenaTextMuted
                        )
                    }
                }

                Text(
                    text = article.formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted
                )
            }

            // Headline
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
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
                text = "READ ARTICLE ↗",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = badgeColor,
                modifier = Modifier.padding(top = 2.dp)
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
            .background(
                if (isSelected) accentColor else Color(0xFF141414),
                RoundedCornerShape(0.dp)
            )
            .border(
                1.dp,
                if (isSelected) accentColor else MeenaBorder,
                RoundedCornerShape(0.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else MeenaTextMuted
        )
    }
}

@Composable
private fun FeedSourceStatusRow(name: String, url: String, badgeColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141414))
            .border(1.dp, MeenaBorder)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MeenaProfitGreen, RoundedCornerShape(0.dp))
            )
            Column {
                Text(name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                Text(url, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted, fontSize = 9.sp)
            }
        }
        Text("ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MeenaProfitGreen)
    }
}
