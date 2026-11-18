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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import my.hana.meena.data.system.DeviceAppInfo
import my.hana.meena.data.system.FavouriteRules
import my.hana.meena.data.system.FavouriteSlot
import my.hana.meena.data.system.InstalledAppsRepository
import my.hana.meena.theme.MeenaBlack
import my.hana.meena.theme.MeenaTextWhite
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun FavouriteTiles(
    pinnedAppIds: List<String>,
    notificationCounts: Map<String, Int>,
    isMonochrome: Boolean,
    colorful: Boolean = false,
    isSmall: Boolean = false,
    onLaunchApp: (String) -> Unit,
    onOpenApps: () -> Unit,
    onTogglePin: (String) -> Unit,
    onMoveApp: (Int, Int) -> Unit,
    onSetPinned: (List<String>) -> Unit = {},
    liveFaces: Map<String, List<my.hana.meena.data.system.LiveFace>> = emptyMap(),
    animateLive: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    var selectedForMenu by remember { mutableStateOf<String?>(null) }
    // Slot id whose group is getting a new app, or losing one.
    var addingTo by remember { mutableStateOf<String?>(null) }
    var removingFrom by remember { mutableStateOf<String?>(null) }
    var confirmRemoveGroup by remember { mutableStateOf<String?>(null) }
    var shortcutPickApp by remember { mutableStateOf(false) }
    var shortcutPickFrom by remember { mutableStateOf<String?>(null) }
    val groupFullMsg = stringResource(R.string.favourites_group_full)
    val fullMsg = stringResource(R.string.favourites_full)

    // Redraw when a shortcut tile finishes loading (or another app pins one).
    val shortcutRevision by my.hana.meena.data.system.ShortcutRepository.revision.collectAsState()
    val slots = remember(pinnedAppIds, shortcutRevision, InstalledAppsRepository.revision) {
        FavouriteRules.visible(pinnedAppIds) { id ->
            if (FavouriteRules.isShortcut(id)) {
                my.hana.meena.data.system.ShortcutRepository.getCachedShortcut(id)
            } else {
                InstalledAppsRepository.getCachedApp(id)
            }
        }
    }
    val perRow = if (isSmall) FavouriteRules.PER_ROW_SMALL else FavouriteRules.PER_ROW

    val tileColor = my.hana.meena.theme.MeenaAccentFill

    if (slots.isEmpty()) {
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
                androidx.compose.material3.Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_thin_add),
                    contentDescription = null,
                    tint = my.hana.meena.theme.MeenaOnAccent,
                    modifier = Modifier.size(24.dp)
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
        slots.forEachIndexed { index, slot ->
            when (slot) {
                is FavouriteSlot.Group -> GroupTile(
                    apps = slot.apps,
                    color = tileColor,
                    isMonochrome = isMonochrome,
                    notificationCounts = notificationCounts,
                    onLaunchApp = onLaunchApp,
                    onAdd = { addingTo = slot.id },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedForMenu = slot.id
                    },
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                )

                is FavouriteSlot.App -> {
                    val app = slot.app
                    val appTileColor = if (colorful && app.dominantColor != 0) {
                        androidx.compose.ui.graphics.Color(app.dominantColor)
                    } else {
                        tileColor
                    }
                    val realPkg = app.shortcutPackage ?: app.packageName
                    val unreadCount = notificationCounts[realPkg] ?: 0
                    val faces = liveFaces[realPkg].orEmpty()
                    val tileDescription = buildString {
                        append(app.label.lowercase())
                        faces.forEach { append(", ").append(it.primary).append(' ').append(it.secondary) }
                    }

                    val pressSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    LiveTileFlip(
                        faces = faces,
                        animate = animateLive,
                        index = index,
                        color = appTileColor,
                        interactionSource = pressSource,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .semantics { contentDescription = tileDescription }
                            .combinedClickable(
                                interactionSource = pressSource,
                                indication = null, // no ripple (Meena UI)
                                onClick = { onLaunchApp(slot.id) },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedForMenu = slot.id
                                }
                            )
                    ) {
                        // Shortcut tiles always show their full icon (contact photo); app tiles use glyph in monochrome.
                        val isShortcutTile = app.shortcutId != null
                        val glyph = if (isMonochrome && !isShortcutTile && !colorful) app.glyphBitmap else null
                        val bitmap = glyph ?: app.iconBitmap
                        if (bitmap != null) {
                            val iconSize = when {
                                isSmall && glyph != null -> 32.dp
                                isSmall -> 28.dp
                                glyph != null -> 40.dp
                                else -> 32.dp
                            }
                            Image(
                                bitmap = bitmap,
                                contentDescription = null,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(iconSize),
                                colorFilter = if (glyph != null) ColorFilter.tint(my.hana.meena.theme.MeenaOnAccent) else null
                            )
                        }

                        // App Label — hidden in small tile mode
                        if (!isSmall) {
                            Text(
                                text = app.label.lowercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = my.hana.meena.theme.MeenaOnAccent,
                                maxLines = 1,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(my.hana.meena.theme.MeenaSpacing.s)
                            )
                        }

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
            }
        }

        // Add button while there is room
        if (slots.size < FavouriteRules.MAX) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(tileColor.copy(alpha = 0.2f))
                    .metroClickable(onClick = onOpenApps),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_thin_add),
                    contentDescription = stringResource(R.string.favourites_add),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Fill remainder of row with empty spaces to maintain exact size
            val remainder = perRow - ((slots.size + 1) % perRow)
            if (remainder < perRow) {
                for (i in 0 until remainder) {
                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                }
            }
        } else {
             val remainder = perRow - (slots.size % perRow)
             if (remainder < perRow) {
                 for (i in 0 until remainder) {
                     Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                 }
             }
        }
    }

    // Long press menu, for an app, shortcut, or group
    val menuId = selectedForMenu
    if (menuId != null) {
        val index = pinnedAppIds.indexOf(menuId)
        val isGroup = FavouriteRules.isGroup(menuId)
        val members = FavouriteRules.members(menuId).mapNotNull { id ->
            if (FavouriteRules.isShortcut(id)) {
                my.hana.meena.data.system.ShortcutRepository.getCachedShortcut(id)
            } else {
                InstalledAppsRepository.getCachedApp(id)
            }
        }
        if (index >= 0 && members.isNotEmpty()) {
            Dialog(onDismissRequest = { selectedForMenu = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MeenaBlack)
                        .padding(my.hana.meena.theme.MeenaSpacing.l)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.l)) {
                        Text(
                            text = if (isGroup) stringResource(R.string.favourites_group_title) else members.first().label.lowercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MeenaTextWhite
                        )
                        if (isGroup) {
                            Text(
                                text = members.joinToString(", ") { it.label.lowercase() },
                                style = MaterialTheme.typography.bodySmall,
                                color = my.hana.meena.theme.MeenaTextMuted
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)) {
                            @Composable
                            fun MenuItem(text: String, color: Color = MeenaTextWhite, onClick: () -> Unit) {
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = color,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .metroClickable {
                                            selectedForMenu = null
                                            onClick()
                                        }
                                        .padding(vertical = my.hana.meena.theme.MeenaSpacing.m)
                                )
                            }
                            if (isGroup && members.size < FavouriteRules.GROUP_MAX) {
                                MenuItem(stringResource(R.string.favourites_group_add)) { addingTo = menuId }
                            }
                            if (!isGroup) {
                                MenuItem(stringResource(R.string.favourites_make_group)) {
                                    val grouped = FavouriteRules.makeGroup(pinnedAppIds, index)
                                    onSetPinned(grouped)
                                    addingTo = grouped[index]
                                }
                            }
                            if (index > 0) {
                                MenuItem(stringResource(R.string.favourites_move_left)) { onMoveApp(index, index - 1) }
                            }
                            if (index < pinnedAppIds.size - 1) {
                                MenuItem(stringResource(R.string.favourites_move_right)) { onMoveApp(index, index + 1) }
                            }
                            if (pinnedAppIds.size < FavouriteRules.MAX) {
                                MenuItem(stringResource(R.string.favourites_add_shortcut)) {
                                    shortcutPickApp = true
                                }
                            }
                            if (isGroup) {
                                MenuItem(stringResource(R.string.favourites_group_take_out)) { removingFrom = menuId }
                                MenuItem(stringResource(R.string.favourites_group_remove), MaterialTheme.colorScheme.error) {
                                    confirmRemoveGroup = menuId
                                }
                            } else {
                                MenuItem(stringResource(R.string.action_remove), MaterialTheme.colorScheme.error) {
                                    if (FavouriteRules.isShortcut(menuId)) {
                                        onSetPinned(pinnedAppIds - menuId)
                                    } else {
                                        onTogglePin(members.first().packageName)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Pick an app to add to a group: every installed app not already in it, with search.
    val addId = addingTo
    if (addId != null) {
        val members = FavouriteRules.members(addId)
        val choices = InstalledAppsRepository.cachedAppList()
            .filter { it.packageName !in members }
            .distinctBy { it.packageName }
        MetroListPicker(
            title = stringResource(R.string.favourites_group_add_title),
            items = choices.map { MetroPickerItem(key = it.packageName, title = it.label.lowercase()) },
            selectedKey = "",
            onSelect = { pkg ->
                val index = pinnedAppIds.indexOf(addId)
                if (index >= 0) {
                    val updated = FavouriteRules.addToGroup(pinnedAppIds, index, pkg)
                    if (updated == null) MetroMessage.show(groupFullMsg) else onSetPinned(updated)
                }
            },
            onDismiss = { addingTo = null },
            enableSearch = true
        )
    }

    // Pick an app to take out of a group.
    val takeOutId = removingFrom
    if (takeOutId != null) {
        val members = FavouriteRules.members(takeOutId).mapNotNull { InstalledAppsRepository.getCachedApp(it) }
        MetroListPicker(
            title = stringResource(R.string.favourites_group_take_out_title),
            items = members.map { MetroPickerItem(key = it.packageName, title = it.label.lowercase()) },
            selectedKey = "",
            onSelect = { pkg -> onSetPinned(FavouriteRules.remove(pinnedAppIds, pkg)) },
            onDismiss = { removingFrom = null },
            enableSearch = false
        )
    }

    val removeId = confirmRemoveGroup
    if (removeId != null) {
        MetroDialog(
            title = stringResource(R.string.favourites_group_remove_title),
            message = stringResource(R.string.favourites_group_remove_question),
            confirmText = stringResource(R.string.action_yes),
            dismissText = stringResource(R.string.action_no),
            onConfirm = {
                onSetPinned(pinnedAppIds - removeId)
                confirmRemoveGroup = null
            },
            onDismiss = { confirmRemoveGroup = null }
        )
    }

    // Step 1: pick an app that publishes shortcuts
    if (shortcutPickApp) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val apps = remember {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
                my.hana.meena.data.system.ShortcutRepository.appsWithShortcuts(context)
            } else emptyList()
        }
        if (apps.isEmpty()) {
            shortcutPickApp = false
            val msg = stringResource(
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N_MR1)
                    R.string.favourites_shortcuts_unavailable
                else R.string.favourites_no_shortcuts
            )
            MetroMessage.show(msg)
        } else {
            MetroListPicker(
                title = stringResource(R.string.favourites_pick_app),
                items = apps.map { MetroPickerItem(key = it.packageName, title = it.label.lowercase()) },
                selectedKey = "",
                onSelect = { pkg ->
                    shortcutPickApp = false
                    shortcutPickFrom = pkg
                },
                onDismiss = { shortcutPickApp = false },
                enableSearch = true
            )
        }
    }

    // Step 2: pick a shortcut from the chosen app
    val scPkg = shortcutPickFrom
    if (scPkg != null) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val shortcuts = remember(scPkg) {
            my.hana.meena.data.system.ShortcutRepository.queryShortcuts(context, scPkg)
        }
        if (shortcuts.isEmpty()) {
            shortcutPickFrom = null
            MetroMessage.show(stringResource(R.string.favourites_no_shortcuts))
        } else {
            MetroListPicker(
                title = stringResource(R.string.favourites_pick_shortcut),
                items = shortcuts.map { MetroPickerItem(key = it.packageName, title = it.label.lowercase()) },
                selectedKey = "",
                onSelect = { key ->
                    if (pinnedAppIds.size >= FavouriteRules.MAX) {
                        MetroMessage.show(fullMsg)
                    } else if (key !in pinnedAppIds) {
                        onSetPinned(pinnedAppIds + key)
                    }
                    shortcutPickFrom = null
                },
                onDismiss = { shortcutPickFrom = null },
                enableSearch = true
            )
        }
    }
}

/**
 * One favourite slot split into four small tiles, Windows Phone style. Each small tile opens its
 * own app; a spare one adds an app. The gaps are drawn, so every small tile keeps its full
 * quarter of the slot as a touch target. Long press anywhere opens the group menu.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun GroupTile(
    apps: List<DeviceAppInfo>,
    color: Color,
    isMonochrome: Boolean,
    notificationCounts: Map<String, Int>,
    onLaunchApp: (String) -> Unit,
    onAdd: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gap = my.hana.meena.theme.MeenaSpacing.s
    val addLabel = stringResource(R.string.favourites_group_add)
    Column(modifier = modifier) {
        for (row in 0 until 2) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                for (col in 0 until 2) {
                    val app = apps.getOrNull(row * 2 + col)
                    val unread = app?.let { notificationCounts[it.packageName] } ?: 0
                    val description = when {
                        app == null -> addLabel
                        unread > 0 -> "${app.label.lowercase()}, $unread"
                        else -> app.label.lowercase()
                    }
                    val fill = if (app == null) color.copy(alpha = 0.2f) else color
                    val miniInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    val miniPressed by miniInteraction.collectIsPressedAsState()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .drawBehind {
                                // Half the gap on the inner sides only, so the group lines up with whole tiles.
                                val half = gap.toPx() / 2f
                                val left = if (col == 1) half else 0f
                                val right = if (col == 0) half else 0f
                                val top = if (row == 1) half else 0f
                                val bottom = if (row == 0) half else 0f
                                drawRect(fill, Offset(left, top), Size(size.width - left - right, size.height - top - bottom))
                            }
                            .semantics { contentDescription = description }
                            .metroPressEffect(isPressed = miniPressed)
                            .combinedClickable(
                                interactionSource = miniInteraction,
                                indication = null,
                                onClick = { if (app != null) onLaunchApp(app.packageName) else onAdd() },
                                onLongClick = onLongPress
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (app == null) {
                            androidx.compose.material3.Icon(
                                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_thin_add),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            val glyph = if (isMonochrome) app.glyphBitmap else null
                            val bitmap = glyph ?: app.iconBitmap
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = null,
                                    modifier = Modifier.size(if (glyph != null) 28.dp else 24.dp),
                                    colorFilter = if (glyph != null) ColorFilter.tint(my.hana.meena.theme.MeenaOnAccent) else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
