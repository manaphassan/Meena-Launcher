package my.hana.meena.ui.components

import androidx.compose.ui.res.stringResource
import my.hana.meena.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import my.hana.meena.theme.MeenaBlack
import my.hana.meena.theme.MeenaTextWhite
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun FavouriteTiles(
    pinnedAppIds: List<String>,
    notificationCounts: Map<String, Int>,
    isMonochrome: Boolean,
    onLaunchApp: (String) -> Unit,
    onOpenApps: () -> Unit,
    onTogglePin: (String) -> Unit,
    onMoveApp: (Int, Int) -> Unit,
    liveFaces: Map<String, List<my.hana.meena.data.system.LiveFace>> = emptyMap(),
    animateLive: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    var selectedForMenu by remember { mutableStateOf<String?>(null) }
    
    val pinnedApps = my.hana.meena.data.system.FavouriteRules.visible(pinnedAppIds) { pkg ->
        my.hana.meena.data.system.InstalledAppsRepository.getCachedApp(pkg)
    }
    val perRow = my.hana.meena.data.system.FavouriteRules.PER_ROW
        
    val tileColor = MaterialTheme.colorScheme.primary
    
    if (pinnedApps.isEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .metroClickable(onClick = onOpenApps)
                .padding(vertical = my.hana.meena.theme.MeenaSpacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(tileColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineMedium,
                    color = my.hana.meena.theme.MeenaOnAccent
                )
            }
            Text(
                text = stringResource(R.string.favourites_add),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        return
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s),
        verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s),
        maxItemsInEachRow = perRow
    ) {
        pinnedApps.forEachIndexed { index, app ->
            val unreadCount = notificationCounts[app.packageName] ?: 0
            val faces = liveFaces[app.packageName].orEmpty()
            val tileDescription = buildString {
                append(app.label.lowercase())
                faces.forEach { append(", ").append(it.primary).append(' ').append(it.secondary) }
            }

            val pressSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            LiveTileFlip(
                faces = faces,
                animate = animateLive,
                index = index,
                color = tileColor,
                interactionSource = pressSource,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .semantics { contentDescription = tileDescription }
                    .combinedClickable(
                        interactionSource = pressSource,
                        indication = null, // no ripple (Meena UI)
                        onClick = { onLaunchApp(app.packageName) },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedForMenu = app.packageName
                        }
                    )
            ) {
                // App Icon: in monochrome, the app's glyph in white; without one, its normal icon (never a white blob).
                val glyph = if (isMonochrome) app.glyphBitmap else null
                val bitmap = glyph ?: app.iconBitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(if (glyph != null) 40.dp else 32.dp),
                        colorFilter = if (glyph != null) ColorFilter.tint(my.hana.meena.theme.MeenaOnAccent) else null
                    )
                }

                // App Label
                Text(
                    text = app.label.lowercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = my.hana.meena.theme.MeenaOnAccent,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(my.hana.meena.theme.MeenaSpacing.s)
                )

                // Unread count, Metro style: a plain number in the corner, no badge box.
                if (unreadCount > 0) {
                    Text(
                        text = unreadCount.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = my.hana.meena.theme.MeenaOnAccent,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(my.hana.meena.theme.MeenaSpacing.s)
                    )
                }
            }
        }
        
        // Add button while there is room
        if (pinnedApps.size < my.hana.meena.data.system.FavouriteRules.MAX) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(tileColor.copy(alpha = 0.2f))
                    .metroClickable(onClick = onOpenApps),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Fill remainder of row with empty spaces to maintain exact size
            val remainder = perRow - ((pinnedApps.size + 1) % perRow)
            if (remainder < perRow) {
                for (i in 0 until remainder) {
                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                }
            }
        } else {
             val remainder = perRow - (pinnedApps.size % perRow)
             if (remainder < perRow) {
                 for (i in 0 until remainder) {
                     Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                 }
             }
        }
    }

    // Long press menu
    if (selectedForMenu != null) {
        val app = my.hana.meena.data.system.InstalledAppsRepository.getCachedApp(selectedForMenu!!)
        val index = pinnedAppIds.indexOf(selectedForMenu)
        if (app != null) {
            Dialog(onDismissRequest = { selectedForMenu = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MeenaBlack)
                        .padding(my.hana.meena.theme.MeenaSpacing.l)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.l)) {
                        Text(
                            text = app.label.lowercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MeenaTextWhite
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)) {
                            if (index > 0) {
                                Text(
                                    text = stringResource(R.string.favourites_move_left),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MeenaTextWhite,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .metroClickable {
                                            onMoveApp(index, index - 1)
                                            selectedForMenu = null
                                        }
                                        .padding(vertical = my.hana.meena.theme.MeenaSpacing.m)
                                )
                            }
                            if (index < pinnedApps.size - 1) {
                                Text(
                                    text = stringResource(R.string.favourites_move_right),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MeenaTextWhite,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .metroClickable {
                                            onMoveApp(index, index + 1)
                                            selectedForMenu = null
                                        }
                                        .padding(vertical = my.hana.meena.theme.MeenaSpacing.m)
                                )
                            }
                            Text(
                                text = stringResource(R.string.action_remove),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .metroClickable {
                                        onTogglePin(app.packageName)
                                        selectedForMenu = null
                                    }
                                    .padding(vertical = my.hana.meena.theme.MeenaSpacing.m)
                            )
                        }
                    }
                }
            }
        }
    }
}
