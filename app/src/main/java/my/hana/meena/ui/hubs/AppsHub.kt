package my.hana.meena.ui.hubs

import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.res.stringResource
import my.hana.meena.R
import android.content.Intent
import android.net.Uri

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape


import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.hana.meena.ui.components.metroFeatherIn
import my.hana.meena.ui.components.MetroDialog

import androidx.compose.ui.window.Dialog
import my.hana.meena.data.system.DeviceAppInfo
import my.hana.meena.data.system.InstalledAppsRepository
import my.hana.meena.data.system.NotificationRepository
import my.hana.meena.theme.MeenaBorder
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextWhite
import my.hana.meena.theme.MeenaBlack
import my.hana.meena.ui.components.MetroDottedProgress
import my.hana.meena.ui.components.metroClickable
import kotlinx.coroutines.launch

@Composable
fun AppsHub(
    listState: LazyListState,
    pinnedAppIds: List<String> = emptyList(),
    focusSearch: Boolean = false,
    reducedMotion: Boolean = false,
    sortMode: String = my.hana.meena.data.model.AppsSort.AZ,
    onSortModeChange: (String) -> Unit = {},
    onSearchFocused: () -> Unit = {},
    onTogglePinApp: (String) -> Unit = {},
    onLaunchApp: (String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(focusSearch) {
        if (focusSearch) {
            listState.scrollToItem(0)
            focusRequester.requestFocus()
            onSearchFocused()
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    var selectedTargetForProperties by remember { mutableStateOf<DeviceAppInfo?>(null) }
    var showUninstallConfirm by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var isJumpListOpen by remember { mutableStateOf(false) }
    var realInstalledApps by remember { mutableStateOf<List<DeviceAppInfo>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }

    // Live notifications stream to flag active notifications
    val liveNotifications by NotificationRepository.notificationsFlow.collectAsState()

    // Keyed on the revision so a new icon pack (or a re-scan) redraws the list.
    LaunchedEffect(InstalledAppsRepository.revision) {
        val loaded = InstalledAppsRepository.loadInstalledApps(context)
        realInstalledApps = loaded
        isLoadingApps = false
    }

    val displayApps = remember(searchQuery, realInstalledApps) {
        if (searchQuery.isNotBlank()) {
            realInstalledApps.filter {
                it.label.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
            }
        } else {
            realInstalledApps
        }
    }

    // A to z keeps the letter tiles and scrubber; the other layouts use plain section titles.
    val mode = my.hana.meena.data.model.AppsSort.of(sortMode)
    var isSortPickerOpen by remember { mutableStateOf(false) }
    val launchVersion by my.hana.meena.data.system.AppLaunchStats.version.collectAsState()
    val otherTitle = stringResource(R.string.apps_category_other)
    val recentTitles = mapOf(
        my.hana.meena.data.system.AppGrouping.BUCKET_TODAY to stringResource(R.string.apps_recent_today),
        my.hana.meena.data.system.AppGrouping.BUCKET_WEEK to stringResource(R.string.apps_recent_week),
        my.hana.meena.data.system.AppGrouping.BUCKET_EARLIER to stringResource(R.string.apps_recent_earlier),
        my.hana.meena.data.system.AppGrouping.BUCKET_NEVER to stringResource(R.string.apps_recent_never)
    )
    val sections = remember(displayApps, mode, launchVersion, otherTitle, recentTitles) {
        when (mode) {
            my.hana.meena.data.model.AppsSort.CATEGORY -> my.hana.meena.data.system.AppGrouping.byCategory(
                displayApps,
                titleOf = { category ->
                    if (category < 0 || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) null
                    else runCatching { android.content.pm.ApplicationInfo.getCategoryTitle(context, category)?.toString() }.getOrNull()
                },
                otherTitle = otherTitle
            )

            my.hana.meena.data.model.AppsSort.RECENT -> {
                val now = System.currentTimeMillis()
                val startOfToday = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                my.hana.meena.data.system.AppGrouping.byRecent(
                    displayApps,
                    lastUsed = my.hana.meena.data.system.AppLaunchStats.load(context).mapValues { it.value.lastUsed },
                    nowMillis = now,
                    startOfTodayMillis = startOfToday,
                    titleOf = { recentTitles[it].orEmpty() }
                )
            }

            else -> my.hana.meena.data.system.AppGrouping.byLetter(displayApps)
        }
    }
    val sortTitles = mapOf(
        my.hana.meena.data.model.AppsSort.AZ to stringResource(R.string.apps_sort_az),
        my.hana.meena.data.model.AppsSort.CATEGORY to stringResource(R.string.apps_sort_category),
        my.hana.meena.data.model.AppsSort.RECENT to stringResource(R.string.apps_sort_recent)
    )

    val activeLetters = remember(realInstalledApps) {
        realInstalledApps.map { it.firstLetter }.toSet()
    }

    val alphabet = remember { listOf('#') + ('A'..'Z').toList() }
    var activeScrubLetter by remember { mutableStateOf<Char?>(null) }

    val letterIndexMap = remember(sections) {
        val map = mutableMapOf<Char, Int>()
        var currentIndex = 3 // after top spacer (0), search box (1) and the sort line (2)
        sections.forEach { section ->
            section.letter?.let { map[it] = currentIndex }
            currentIndex += 1 + section.apps.size
        }
        map
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = my.hana.meena.theme.MeenaSpacing.l),
            verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                // No box: an accent underline on the page, like the picker search.
                val underline = MaterialTheme.colorScheme.primary
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .drawWithContent {
                            drawContent()
                            val y = size.height - 1.dp.toPx()
                            drawLine(underline, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 2.dp.toPx())
                        },
                    placeholder = { Text(stringResource(R.string.apps_search_hint), color = MeenaTextMuted, fontSize = 14.sp) },

                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MeenaBlack,
                        unfocusedContainerColor = MeenaBlack,
                        focusedTextColor = MeenaTextWhite,
                        unfocusedTextColor = MeenaTextWhite,
                        cursorColor = underline,
                        focusedBorderColor = MeenaTextWhite.copy(alpha=0f),
                        unfocusedBorderColor = MeenaTextWhite.copy(alpha=0f)
                    ),
                    shape = RoundedCornerShape(0.dp),
                    singleLine = true
                )
            }

            item(key = "sort") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .metroClickable { isSortPickerOpen = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.apps_sort_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MeenaTextMuted,
                        modifier = Modifier.padding(end = my.hana.meena.theme.MeenaSpacing.m)
                    )
                    Text(
                        text = sortTitles[mode].orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = primaryColor
                    )
                }
            }

            if (isLoadingApps) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = my.hana.meena.theme.MeenaSpacing.l),
                        contentAlignment = Alignment.Center
                    ) {
                        MetroDottedProgress()
                    }
                }
            } else if (sections.isEmpty()) {
                item {
                    Text(
                        text = if (searchQuery.isNotBlank()) stringResource(R.string.apps_no_match, searchQuery) else stringResource(R.string.apps_none),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MeenaTextMuted,
                        modifier = Modifier.padding(vertical = my.hana.meena.theme.MeenaSpacing.l)
                    )
                }
            } else {
                sections.forEach { section ->
                    val appsInLetter = section.apps
                    val letter = section.letter
                    item(key = "header-${section.key}") {
                        if (letter == null) {
                            // Category or time block: a plain lowercase title, no tile.
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = primaryColor,
                                modifier = Modifier
                                    .metroFeatherIn(index = 0, reducedMotion = reducedMotion)
                                    .padding(top = my.hana.meena.theme.MeenaSpacing.m, bottom = my.hana.meena.theme.MeenaSpacing.s)
                            )
                            return@item
                        }
                        // Metro Jump-List Header Tile (48x48dp, 0dp corner radius, metroClickable)
                        Row(
                            modifier = Modifier
                                .metroFeatherIn(index = 0, reducedMotion = reducedMotion)
                                .padding(top = my.hana.meena.theme.MeenaSpacing.m, bottom = my.hana.meena.theme.MeenaSpacing.s),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(my.hana.meena.theme.MeenaAccentFill)
                                    .metroClickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isJumpListOpen = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letter.toString().lowercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = my.hana.meena.theme.MeenaOnAccent,
                                    fontSize = 24.sp
                                )
                            }
                        }
                    }

                    // One app can have several launcher entries (a dialer's phone and contacts), so key by activity and user.
                    items(appsInLetter.size, key = { i ->
                        val a = appsInLetter[i]
                        "app-${a.componentName?.flattenToShortString() ?: a.packageName}-${a.userHandle?.hashCode() ?: 0}"
                    }) { idx ->
                        val app = appsInLetter[idx]
                        val appNotif = liveNotifications.firstOrNull { it.packageName == app.packageName }
                        val notifCount = liveNotifications.count { it.packageName == app.packageName }

                        AppRow(
                            modifier = Modifier.metroFeatherIn(
                                index = idx,
                                reducedMotion = reducedMotion
                            ),
                            name = app.label,
                            iconBitmap = app.iconBitmap,
                            notificationSnippet = appNotif?.text?.ifBlank { appNotif.title },
                            badge = notifCount,
                            time = appNotif?.formattedTime,
                            onClick = { onLaunchApp(app.packageName) },
                            onLongClick = {
                                selectedTargetForProperties = app
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }

        // Floating Alphabet Scrubber on the Right Edge for Fast Scrolling (a to z only)
        if (mode == my.hana.meena.data.model.AppsSort.AZ) Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(28.dp)
                .padding(vertical = my.hana.meena.theme.MeenaSpacing.l, horizontal = 0.dp)
                .pointerInput(alphabet, letterIndexMap) {
                    detectTapGestures(
                        onPress = { offset ->
                            val totalH = size.height
                            val itemH = totalH / alphabet.size
                            val idx = (offset.y / itemH).toInt().coerceIn(0, alphabet.size - 1)
                            val letter = alphabet[idx]
                            activeScrubLetter = letter
                            val targetIdx = letterIndexMap[letter]
                                ?: letterIndexMap.entries.firstOrNull { it.key >= letter }?.value
                            if (targetIdx != null) {
                                coroutineScope.launch { listState.scrollToItem(targetIdx) }
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            tryAwaitRelease()
                            activeScrubLetter = null
                        }
                    )
                }
                .pointerInput(alphabet, letterIndexMap) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val totalH = size.height
                            val itemH = totalH / alphabet.size
                            val idx = (offset.y / itemH).toInt().coerceIn(0, alphabet.size - 1)
                            val letter = alphabet[idx]
                            activeScrubLetter = letter
                            val targetIdx = letterIndexMap[letter]
                                ?: letterIndexMap.entries.firstOrNull { it.key >= letter }?.value
                            if (targetIdx != null) {
                                coroutineScope.launch { listState.scrollToItem(targetIdx) }
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onDragEnd = { activeScrubLetter = null },
                        onDragCancel = { activeScrubLetter = null },
                        onDrag = { change, _ ->
                            change.consume()
                            val totalH = size.height
                            val itemH = totalH / alphabet.size
                            val idx = (change.position.y / itemH).toInt().coerceIn(0, alphabet.size - 1)
                            val letter = alphabet[idx]
                            if (activeScrubLetter != letter) {
                                activeScrubLetter = letter
                                val targetIdx = letterIndexMap[letter]
                                    ?: letterIndexMap.entries.firstOrNull { it.key >= letter }?.value
                                if (targetIdx != null) {
                                    coroutineScope.launch { listState.scrollToItem(targetIdx) }
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Every letter must fit: at large text sizes 27 letters overflow the screen and the
            // touch rows (size.height / 27) stop matching what is drawn. Shrink only as needed.
            androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxHeight()) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val slotSp = with(density) { (maxHeight / alphabet.size * 0.75f).toSp() }
            val letterSize = if (slotSp.value < 12f) slotSp else 12.sp
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                alphabet.forEach { char ->
                    val hasApps = activeLetters.contains(char) || (char == '#' && activeLetters.contains('#'))
                    Text(
                        text = char.toString().lowercase(),
                        fontSize = letterSize,
                        lineHeight = letterSize,
                        fontWeight = if (hasApps) FontWeight.Medium else FontWeight.Normal,
                        color = if (activeScrubLetter == char) primaryColor else if (hasApps) MeenaTextWhite.copy(alpha = 0.9f) else MeenaTextMuted
                    )
                }
            }
            }
        }

        if (isSortPickerOpen) {
            my.hana.meena.ui.components.MetroListPicker(
                title = stringResource(R.string.apps_sort_title),
                items = my.hana.meena.data.model.AppsSort.ALL.map { key ->
                    my.hana.meena.ui.components.MetroPickerItem(
                        key = key,
                        title = sortTitles[key].orEmpty(),
                        subtitle = when (key) {
                            my.hana.meena.data.model.AppsSort.CATEGORY -> stringResource(R.string.apps_sort_category_hint)
                            my.hana.meena.data.model.AppsSort.RECENT -> stringResource(R.string.apps_sort_recent_hint)
                            else -> null
                        }
                    )
                },
                selectedKey = mode,
                onSelect = { key ->
                    onSortModeChange(key)
                    coroutineScope.launch { listState.scrollToItem(0) }
                },
                onDismiss = { isSortPickerOpen = false },
                enableSearch = false
            )
        }

        // Floating Magnified Alphabet Indicator when actively scrubbing
        if (activeScrubLetter != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(68.dp)
                    .background(my.hana.meena.theme.MeenaAccentFill),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeScrubLetter.toString().lowercase(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Medium,
                    color = my.hana.meena.theme.MeenaOnAccent
                )
            }
        }

        // FULL SCREEN JUMP LIST OVERLAY
        if (isJumpListOpen) {
            AlphabetJumpListOverlay(
                activeLetters = activeLetters,
                onSelectLetter = { char ->
                    isJumpListOpen = false
                    val idx = letterIndexMap[char]
                    if (idx != null) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(idx)
                        }
                    }
                },
                onDismiss = { isJumpListOpen = false }
            )
        }

        // METRO APP PROPERTIES DIALOG
        val target = selectedTargetForProperties
        if (target != null) {
            val isPinned = my.hana.meena.data.system.FavouriteRules.contains(pinnedAppIds, target.packageName)
            if (showUninstallConfirm) {
                MetroDialog(
                    title = stringResource(R.string.dialog_uninstall_title),
                    message = stringResource(R.string.dialog_uninstall_question, target.label.lowercase()),
                    confirmText = stringResource(R.string.action_yes),
                    dismissText = stringResource(R.string.action_no),
                    onConfirm = {
                        try {
                            InstalledAppsRepository.invalidateCache()
                            // The system uninstaller silently finishes for callers targeting API 28+
                            // without REQUEST_DELETE_PACKAGES (AOSP UninstallerActivity.onCreate), so
                            // without it we open the app's info page, where uninstall is one tap.
                            val packageUri = Uri.parse("package:${target.packageName}")
                            val canUninstallDirectly = context.checkSelfPermission(
                                android.Manifest.permission.REQUEST_DELETE_PACKAGES
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            val intent = if (canUninstallDirectly) {
                                Intent(Intent.ACTION_DELETE, packageUri)
                            } else {
                                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
                            }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                        showUninstallConfirm = false
                        selectedTargetForProperties = null
                    },
                    onDismiss = { showUninstallConfirm = false }
                )
            } else {
                Dialog(onDismissRequest = {
                    selectedTargetForProperties = null
                    showUninstallConfirm = false
                }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MeenaBlack)
                            .padding(my.hana.meena.theme.MeenaSpacing.l)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(my.hana.meena.theme.MeenaAccentFill),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (target.iconBitmap != null) {
                                        Image(bitmap = target.iconBitmap, contentDescription = target.label, modifier = Modifier.size(32.dp))
                                    } else {
                                        Text(target.firstLetter.toString().lowercase(), fontSize = 24.sp, fontWeight = FontWeight.Medium, color = my.hana.meena.theme.MeenaOnAccent)
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(target.label.lowercase(), style = MaterialTheme.typography.titleLarge, color = MeenaTextWhite)
                                    Text(stringResource(R.string.quick_actions_title), style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                }
                            }

                            // PIN / UNPIN
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .metroClickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onTogglePinApp(target.packageName)
                                        selectedTargetForProperties = null
                                    }
                                    .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isPinned) stringResource(R.string.apps_favourite_remove) else stringResource(R.string.apps_favourite_add),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isPinned) MaterialTheme.colorScheme.error else primaryColor
                                    )
                                    Text(
                                        text = if (isPinned) stringResource(R.string.apps_favourite_remove_hint) else stringResource(R.string.apps_favourite_add_hint),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextMuted
                                    )
                                }
                            }

                            // APP INFO
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .metroClickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.parse("package:${target.packageName}")
                                        }
                                        context.startActivity(intent)
                                        selectedTargetForProperties = null
                                    }
                                    .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.menu_app_info),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MeenaTextWhite
                                    )
                                    Text(
                                        text = stringResource(R.string.apps_app_info_hint),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextMuted
                                    )
                                }
                            }

                            // UNINSTALL
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .metroClickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showUninstallConfirm = true
                                    }
                                    .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.menu_uninstall),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = stringResource(R.string.apps_uninstall_hint),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextMuted
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = my.hana.meena.theme.MeenaSpacing.s),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = stringResource(R.string.action_close),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MeenaTextMuted,
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .metroClickable { selectedTargetForProperties = null }
                                        .padding(vertical = my.hana.meena.theme.MeenaSpacing.m, horizontal = my.hana.meena.theme.MeenaSpacing.s)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(
    modifier: Modifier = Modifier,
    name: String,
    icon: ImageVector? = null,
    iconBitmap: ImageBitmap? = null,
    iconBg: Color = my.hana.meena.theme.MeenaAccentFill,
    notificationSnippet: String? = null,
    badge: Int = 0,
    time: String? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Icon Box with badge
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(iconBg, RoundedCornerShape(0.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = name,
                    modifier = Modifier.size(32.dp)
                )
            } else if (icon != null) {
                Icon(icon, contentDescription = name, tint = my.hana.meena.theme.MeenaOnAccent, modifier = Modifier.size(26.dp))
            } else {
                Text(
                    text = name.firstOrNull()?.lowercase() ?: "#",
                    color = my.hana.meena.theme.MeenaOnAccent,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 20.sp
                )
            }
            if (badge > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(my.hana.meena.theme.MeenaAccentFill, RoundedCornerShape(0.dp))
                        .padding(horizontal = my.hana.meena.theme.MeenaSpacing.s, vertical = 0.dp)
                ) {
                    Text(
                        text = badge.toString(),
                        fontSize = 12.sp,
                        color = my.hana.meena.theme.MeenaOnAccent
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Titles & Summary (no package name, no version)
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name.lowercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontSize = 17.sp,
                    color = MeenaTextWhite
                )
                if (time != null && notificationSnippet != null) {
                    Text(time, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                }
            }

            // Only show if there is an unread message or notification
            if (notificationSnippet != null) {
                Text(
                    text = notificationSnippet,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 0.dp)
                )
            }
        }

        // Only show indicator on the right if there is an unread notification / message
        if (badge > 0 || notificationSnippet != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun AlphabetJumpListOverlay(
    activeLetters: Set<Char>,
    onSelectLetter: (Char) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val allChars = listOf('#') + ('a'..'z').toList()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MeenaBlack.copy(alpha = 0.94f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
            .padding(horizontal = my.hana.meena.theme.MeenaSpacing.l, vertical = my.hana.meena.theme.MeenaSpacing.l),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.apps_jump_to),
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 28.sp,
                    color = MeenaTextWhite
                )
                Text(
                    text = stringResource(R.string.action_close),
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .metroClickable { onDismiss() }
                        .padding(vertical = my.hana.meena.theme.MeenaSpacing.m, horizontal = my.hana.meena.theme.MeenaSpacing.s)
                )
            }

            val rows = allChars.chunked(6)
            Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)) {
                rows.forEach { rowChars ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)
                    ) {
                        rowChars.forEach { char ->
                            val isAvailable = activeLetters.contains(char.uppercaseChar())
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .background(
                                        color = if (isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                                    )
                                    .metroClickable(enabled = isAvailable) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectLetter(char.uppercaseChar())
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char.toString().lowercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 20.sp,
                                    color = if (isAvailable) MeenaTextWhite else MeenaTextMuted
                                )
                            }
                        }
                        if (rowChars.size < 6) {
                            repeat(6 - rowChars.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

