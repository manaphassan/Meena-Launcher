package my.hana.meena.ui.components

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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun FavouriteTiles(
    pinnedAppIds: List<String>,
    notificationCounts: Map<String, Int>,
    isMonochrome: Boolean,
    onLaunchApp: (String) -> Unit,
    onOpenApps: () -> Unit,
    onTogglePin: (String) -> Unit,
    onMoveApp: (Int, Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var selectedForMenu by remember { mutableStateOf<String?>(null) }
    
    val pinnedApps = pinnedAppIds
        .mapNotNull { pkg -> my.hana.meena.data.system.InstalledAppsRepository.getCachedApp(pkg) }
        .take(8)
        
    val tileColor = MaterialTheme.colorScheme.primary
    
    if (pinnedApps.isEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .metroClickable(onClick = onOpenApps)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(tileColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.displayMedium,
                    color = MeenaTextWhite
                )
            }
            Text(
                text = "add a favourite",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        return
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        maxItemsInEachRow = 4
    ) {
        pinnedApps.forEachIndexed { index, app ->
            val unreadCount = notificationCounts[app.packageName] ?: 0
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(tileColor)
                    .combinedClickable(
                        onClick = { onLaunchApp(app.packageName) },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedForMenu = app.packageName
                        }
                    )
            ) {
                // App Icon
                if (app.iconBitmap != null) {
                    Image(
                        bitmap = app.iconBitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(32.dp),
                        colorFilter = if (isMonochrome) ColorFilter.tint(MeenaTextWhite) else null
                    )
                }

                // App Label
                Text(
                    text = app.label.lowercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextWhite,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                )

                // Unread Badge
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 4.dp)
                            .size(24.dp)
                            .background(MaterialTheme.colorScheme.error),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MeenaTextWhite
                        )
                    }
                }
            }
        }
        
        // Add Button if less than 8
        if (pinnedApps.size < 8) {
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
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Fill remainder of row with empty spaces to maintain exact size
            val remainder = 4 - ((pinnedApps.size + 1) % 4)
            if (remainder < 4) {
                for (i in 0 until remainder) {
                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                }
            }
        } else {
             val remainder = 4 - (pinnedApps.size % 4)
             if (remainder < 4) {
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
                        .padding(24.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        Text(
                            text = app.label.lowercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MeenaTextWhite
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (index > 0) {
                                Text(
                                    text = "move left",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MeenaTextWhite,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .metroClickable {
                                            onMoveApp(index, index - 1)
                                            selectedForMenu = null
                                        }
                                        .padding(vertical = 12.dp)
                                )
                            }
                            if (index < pinnedApps.size - 1) {
                                Text(
                                    text = "move right",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MeenaTextWhite,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .metroClickable {
                                            onMoveApp(index, index + 1)
                                            selectedForMenu = null
                                        }
                                        .padding(vertical = 12.dp)
                                )
                            }
                            Text(
                                text = "remove",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .metroClickable {
                                        onTogglePin(app.packageName)
                                        selectedForMenu = null
                                    }
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
