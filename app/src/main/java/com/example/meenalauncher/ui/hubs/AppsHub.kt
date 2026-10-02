package com.example.meenalauncher.ui.hubs

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.window.Dialog
import com.example.meenalauncher.data.system.DeviceAppInfo
import com.example.meenalauncher.data.system.InstalledAppsRepository
import com.example.meenalauncher.data.system.NotificationRepository
import com.example.meenalauncher.theme.MeenaBorder
import com.example.meenalauncher.theme.MeenaTextMuted
import com.example.meenalauncher.theme.MeenaTextWhite
import kotlinx.coroutines.launch

data class AppTargetMeta(
    val id: String,
    val name: String,
    val icon: ImageVector? = null,
    val iconBitmap: ImageBitmap? = null,
    val iconBg: Color = Color(0xFF00A4EF),
    val isRealApp: Boolean = true
)

@Composable
fun AppsHub(
    listState: LazyListState,
    pinnedAppIds: List<String> = emptyList(),
    pinnedAppSizes: Map<String, String> = emptyMap(),
    onTogglePinApp: (String) -> Unit = {},
    onSetTileSize: (String, String) -> Unit = { _, _ -> },
    onLaunchApp: (String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var searchQuery by remember { mutableStateOf("") }

    val primaryColor = MaterialTheme.colorScheme.primary
    var selectedTargetForProperties by remember { mutableStateOf<AppTargetMeta?>(null) }

    val coroutineScope = rememberCoroutineScope()
    var isJumpListOpen by remember { mutableStateOf(false) }
    var realInstalledApps by remember { mutableStateOf<List<DeviceAppInfo>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }

    // Live notifications stream to flag active notifications
    val liveNotifications by NotificationRepository.notificationsFlow.collectAsState()

    LaunchedEffect(Unit) {
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

    val groupedApps = remember(displayApps) {
        displayApps.groupBy { it.firstLetter }.toSortedMap()
    }

    val activeLetters = remember(realInstalledApps) {
        realInstalledApps.map { it.firstLetter }.toSet()
    }

    val alphabet = remember { listOf('#') + ('A'..'Z').toList() }
    var activeScrubLetter by remember { mutableStateOf<Char?>(null) }

    val letterIndexMap = remember(groupedApps) {
        val map = mutableMapOf<Char, Int>()
        var currentIndex = 2 // after top spacer (0) and search box (1)
        groupedApps.forEach { (char, apps) ->
            map[char] = currentIndex
            currentIndex += 1 + apps.size
        }
        map
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Authentic WP8.1 Search Bar (White background, black text)
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search apps...", color = Color.Gray, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, "Search", tint = Color.Black) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(4.dp),
                    singleLine = true
                )
            }

            if (isLoadingApps) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = primaryColor)
                    }
                }
            } else if (groupedApps.isEmpty()) {
                item {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No apps found matching \"$searchQuery\"" else "No applications available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MeenaTextMuted,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                }
            } else {
                groupedApps.forEach { (letter, appsInLetter) ->
                    // Metro Jump-List Header Tile
                    item(key = "header-$letter") {
                        Row(
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(primaryColor, RoundedCornerShape(4.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isJumpListOpen = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letter.toString().lowercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontSize = 24.sp
                                )
                            }
                        }
                    }

                    items(appsInLetter.size, key = { "app-${appsInLetter[it].packageName}" }) { idx ->
                        val app = appsInLetter[idx]
                        val appNotif = liveNotifications.firstOrNull { it.packageName == app.packageName }
                        val notifCount = liveNotifications.count { it.packageName == app.packageName }

                        AppRow(
                            name = app.label,
                            iconBitmap = app.iconBitmap,
                            notificationSnippet = appNotif?.text?.ifBlank { appNotif.title },
                            badge = notifCount,
                            time = appNotif?.formattedTime,
                            onClick = { onLaunchApp(app.packageName) },
                            onLongClick = {
                                selectedTargetForProperties = AppTargetMeta(
                                    id = app.packageName,
                                    name = app.label,
                                    iconBitmap = app.iconBitmap,
                                    iconBg = primaryColor,
                                    isRealApp = true
                                )
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }

        // Floating Alphabet Scrubber on the Right Edge for Fast Scrolling
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(28.dp)
                .padding(vertical = 24.dp, horizontal = 2.dp)
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
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                alphabet.forEach { char ->
                    val hasApps = activeLetters.contains(char) || (char == '#' && activeLetters.contains('#'))
                    Text(
                        text = char.toString(),
                        fontSize = 9.sp,
                        fontWeight = if (hasApps) FontWeight.Bold else FontWeight.Normal,
                        color = if (activeScrubLetter == char) primaryColor else if (hasApps) Color.White.copy(alpha = 0.9f) else Color.DarkGray
                    )
                }
            }
        }

        // Floating Magnified Alphabet Indicator when actively scrubbing
        if (activeScrubLetter != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(68.dp)
                    .background(primaryColor, CircleShape)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeScrubLetter.toString(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
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
            val isPinned = pinnedAppIds.contains(target.id)
            Dialog(onDismissRequest = { selectedTargetForProperties = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0C0C0C))
                        .border(2.dp, primaryColor)
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(target.iconBg, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (target.iconBitmap != null) {
                                    Image(bitmap = target.iconBitmap, contentDescription = target.name, modifier = Modifier.size(32.dp))
                                } else if (target.icon != null) {
                                    Icon(target.icon, contentDescription = target.name, tint = Color.White, modifier = Modifier.size(28.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(target.name, style = MaterialTheme.typography.titleLarge, color = Color.White)
                                Text(target.id, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                            }
                        }

                        // PIN / UNPIN
                        MetroDialogActionRow(
                            title = if (isPinned) "Unpin from Start" else "Pin to Start",
                            subtitle = if (isPinned) "Remove quick tile from home canvas" else "Display tile on main panoramic canvas",
                            accentColor = if (isPinned) Color(0xFFFF5252) else primaryColor,
                            iconText = if (isPinned) "📌" else "📍",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTogglePinApp(target.id)
                                Toast.makeText(context, if (isPinned) "Unpinned ${target.name}" else "Pinned ${target.name} to Start", Toast.LENGTH_SHORT).show()
                                selectedTargetForProperties = null
                            }
                        )

                        // APP INFO
                        MetroDialogActionRow(
                            title = "App Info & Permissions",
                            subtitle = "Open Android system settings & permissions",
                            accentColor = Color.White,
                            iconText = "⚙️",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${target.id}")
                                }
                                context.startActivity(intent)
                                selectedTargetForProperties = null
                            }
                        )

                        // UNINSTALL
                        MetroDialogActionRow(
                            title = "Uninstall App",
                            subtitle = "Delete application package from device",
                            accentColor = Color(0xFFFF5252),
                            iconText = "🗑️",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                try {
                                    val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${target.id}"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "System app cannot be uninstalled", Toast.LENGTH_SHORT).show()
                                }
                                selectedTargetForProperties = null
                            }
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1C1C1C))
                                .border(1.dp, MeenaBorder)
                                .clickable { selectedTargetForProperties = null }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("CLOSE", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetroDialogActionRow(
    title: String,
    subtitle: String,
    accentColor: Color,
    iconText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141414))
            .border(1.dp, MeenaBorder)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color(0xFF222222)),
            contentAlignment = Alignment.Center
        ) {
            Text(iconText, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = accentColor)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(
    name: String,
    icon: ImageVector? = null,
    iconBitmap: ImageBitmap? = null,
    iconBg: Color = MaterialTheme.colorScheme.primary,
    notificationSnippet: String? = null,
    badge: Int = 0,
    time: String? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Icon Box with badge
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(iconBg, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = name,
                    modifier = Modifier.size(32.dp)
                )
            } else if (icon != null) {
                Icon(icon, contentDescription = name, tint = Color.White, modifier = Modifier.size(26.dp))
            } else {
                Text(
                    text = name.firstOrNull()?.uppercase() ?: "#",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 20.sp
                )
            }
            if (badge > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(Color(0xFFFF3B30), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badge.toString(),
                        fontSize = 9.sp,
                        color = Color.White
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
                    text = name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontSize = 17.sp,
                    color = Color.White
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
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Only show indicator on the right if there is an unread notification / message
        if (badge > 0 || notificationSnippet != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
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
            .background(Color.Black.copy(alpha = 0.94f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
            .padding(horizontal = 24.dp, vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "choose letter",
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 28.sp,
                    color = Color.White
                )
                Text(
                    text = "✕",
                    style = MaterialTheme.typography.titleLarge,
                    color = MeenaTextMuted,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }

            val rows = allChars.chunked(6)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { rowChars ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowChars.forEach { char ->
                            val isAvailable = activeLetters.contains(char.uppercaseChar())
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .background(
                                        color = if (isAvailable) MaterialTheme.colorScheme.primary else Color(0xFF161616)
                                    )
                                    .clickable(enabled = isAvailable) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectLetter(char.uppercaseChar())
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 20.sp,
                                    color = if (isAvailable) Color.White else Color.DarkGray
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
