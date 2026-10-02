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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.meenalauncher.data.system.DeviceAppInfo
import com.example.meenalauncher.data.system.InstalledAppsRepository
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
    val isRealApp: Boolean = false
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
    var simCount by remember { mutableIntStateOf(0) }

    var gmailSummary by remember { mutableStateOf("Google Cloud: Architecture review approved") }
    var gmailBadge by remember { mutableIntStateOf(4) }
    var telegramSummary by remember { mutableStateOf("Ahmad: Deploy completed successfully in staging") }
    var telegramBadge by remember { mutableIntStateOf(9) }
    var whatsappSummary by remember { mutableStateOf("Mom: Remember to come over for dinner tonight!") }
    var whatsappBadge by remember { mutableIntStateOf(6) }

    val primaryColor = MaterialTheme.colorScheme.primary
    var selectedTargetForProperties by remember { mutableStateOf<AppTargetMeta?>(null) }

    val coroutineScope = rememberCoroutineScope()
    var isJumpListOpen by remember { mutableStateOf(false) }
    var realInstalledApps by remember { mutableStateOf<List<DeviceAppInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        val loaded = InstalledAppsRepository.loadInstalledApps(context)
        if (loaded.isNotEmpty()) {
            realInstalledApps = loaded
        }
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
        realInstalledApps.map { it.firstLetter }.toSet().ifEmpty { setOf('A', 'C', 'G', 'T', 'W') }
    }

    val letterIndexMap = remember(groupedApps) {
        val map = mutableMapOf<Char, Int>()
        var currentIndex = 3 // after top spacer (0), search (1), simulation row (2)
        groupedApps.forEach { (char, apps) ->
            map[char] = currentIndex
            currentIndex += 1 + apps.size
        }
        map
    }

    fun triggerSimulatedInflow() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        simCount++
        when (simCount % 3) {
            1 -> {
                telegramBadge += 1
                telegramSummary = "Dr. Zulkifli: Attached the JAKIM falak calculation PDF"
            }
            2 -> {
                gmailBadge += 1
                gmailSummary = "Google Antigravity: Android project scaffold ready"
            }
            0 -> {
                whatsappBadge += 1
                whatsappSummary = "Farhan: Let us review the Jetpack Compose PR"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    shape = RoundedCornerShape(0.dp),
                    singleLine = true
                )
            }

            // Realtime Simulation Trigger
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0E0E0E))
                        .border(1.dp, MeenaBorder)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Simulate Realtime Inflow:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MeenaTextMuted
                    )
                    Button(
                        onClick = { triggerSimulatedInflow() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        Text("⚡ Test Live Alert", fontSize = 11.sp, color = Color.White)
                    }
                }
            }

            if (realInstalledApps.isNotEmpty()) {
                groupedApps.forEach { (letter, appsInLetter) ->
                    item(key = "header-$letter") {
                        Row(
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(0.dp))
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
                        AppRow(
                            name = app.label,
                            iconBitmap = app.iconBitmap,
                            subtitle = "${app.version} • ${app.packageName}",
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
            } else {
                // Fallback Sample Catalog
                item {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isJumpListOpen = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("a", style = MaterialTheme.typography.titleLarge, color = Color.White, fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        AppRow(
                            name = "Alarms & Clock",
                            icon = Icons.Default.Alarm,
                            iconBg = primaryColor,
                            subtitle = "Next alarm: 05:30 AM tomorrow",
                            onClick = { onLaunchApp("com.android.deskclock") },
                            onLongClick = {
                                selectedTargetForProperties = AppTargetMeta(
                                    id = "com.android.deskclock",
                                    name = "Alarms & Clock",
                                    icon = Icons.Default.Alarm,
                                    iconBg = primaryColor
                                )
                            }
                        )
                    }
                }

                item {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isJumpListOpen = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("c", style = MaterialTheme.typography.titleLarge, color = Color.White, fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        AppRow(
                            name = "Calculator",
                            icon = Icons.Default.Calculate,
                            iconBg = primaryColor,
                            subtitle = "Standard / Programmer Math",
                            onClick = { onLaunchApp("com.android.calculator2") },
                            onLongClick = {
                                selectedTargetForProperties = AppTargetMeta(
                                    id = "com.android.calculator2",
                                    name = "Calculator",
                                    icon = Icons.Default.Calculate,
                                    iconBg = primaryColor
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AppRow(
                            name = "Camera",
                            icon = Icons.Default.CameraAlt,
                            iconBg = Color(0xFF333333),
                            subtitle = "4K HDR • Pro Controls",
                            onClick = { onLaunchApp("com.android.camera") },
                            onLongClick = {
                                selectedTargetForProperties = AppTargetMeta(
                                    id = "com.android.camera",
                                    name = "Camera",
                                    icon = Icons.Default.CameraAlt,
                                    iconBg = Color(0xFF333333)
                                )
                            }
                        )
                    }
                }

                item {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isJumpListOpen = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("g", style = MaterialTheme.typography.titleLarge, color = Color.White, fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        AppRow(
                            name = "Gmail",
                            icon = Icons.Default.Email,
                            iconBg = Color(0xFFEA4335),
                            badge = gmailBadge,
                            summary = gmailSummary,
                            time = "10m ago",
                            onClick = { onLaunchApp("com.google.android.gm") },
                            onLongClick = {
                                selectedTargetForProperties = AppTargetMeta(
                                    id = "com.google.android.gm",
                                    name = "Gmail",
                                    icon = Icons.Default.Email,
                                    iconBg = Color(0xFFEA4335)
                                )
                            }
                        )
                    }
                }

                item {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isJumpListOpen = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("t", style = MaterialTheme.typography.titleLarge, color = Color.White, fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        AppRow(
                            name = "Telegram",
                            icon = Icons.AutoMirrored.Filled.Send,
                            iconBg = Color(0xFF229ED9),
                            badge = telegramBadge,
                            summary = telegramSummary,
                            time = "Just now",
                            onClick = { onLaunchApp("org.telegram.messenger") },
                            onLongClick = {
                                selectedTargetForProperties = AppTargetMeta(
                                    id = "org.telegram.messenger",
                                    name = "Telegram",
                                    icon = Icons.AutoMirrored.Filled.Send,
                                    iconBg = Color(0xFF229ED9)
                                )
                            }
                        )
                    }
                }

                item {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isJumpListOpen = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("w", style = MaterialTheme.typography.titleLarge, color = Color.White, fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        AppRow(
                            name = "WhatsApp",
                            icon = Icons.AutoMirrored.Filled.Message,
                            iconBg = Color(0xFF25D366),
                            badge = whatsappBadge,
                            summary = whatsappSummary,
                            time = "2m ago",
                            onClick = { onLaunchApp("com.whatsapp") },
                            onLongClick = {
                                selectedTargetForProperties = AppTargetMeta(
                                    id = "com.whatsapp",
                                    name = "WhatsApp",
                                    icon = Icons.AutoMirrored.Filled.Message,
                                    iconBg = Color(0xFF25D366)
                                )
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }

        // WP8 Alphabet Jump List Overlay (Semantic Zoom)
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
                        .border(2.dp, MaterialTheme.colorScheme.primary)
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(target.iconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                if (target.iconBitmap != null) {
                                    Image(bitmap = target.iconBitmap, contentDescription = target.name, modifier = Modifier.size(32.dp))
                                } else if (target.icon != null) {
                                    Icon(target.icon, contentDescription = target.name, tint = Color.White, modifier = Modifier.size(28.dp))
                                } else {
                                    Text(target.name.firstOrNull()?.uppercase() ?: "#", fontSize = 22.sp, color = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(target.name, style = MaterialTheme.typography.headlineMedium, fontSize = 22.sp, color = Color.White)
                                Text(target.id, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                Text("v2.4.1 • 148 MB • Storage & Cache", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MeenaBorder))

                        // 1. PIN TO START / UNPIN
                        MetroDialogActionRow(
                            title = if (isPinned) "Unpin from Start" else "Pin to Start",
                            subtitle = if (isPinned) "Remove live tile from home hub" else "Create dynamic live tile on Start hub",
                            accentColor = MaterialTheme.colorScheme.primary,
                            iconText = if (isPinned) "✕" else "📌",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTogglePinApp(target.id)
                                Toast.makeText(context, if (isPinned) "Unpinned from Start" else "Pinned to Start", Toast.LENGTH_SHORT).show()
                                selectedTargetForProperties = null
                            }
                        )

                        // TILE GEOMETRY SELECTOR (Visible if pinned)
                        if (isPinned) {
                            val currentSize = pinnedAppSizes[target.id] ?: "2x2"
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF141414))
                                    .border(1.dp, MeenaBorder)
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("TILE GEOMETRY (SIZE ON START)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("1x1", "2x2", "4x2").forEach { sizeOption ->
                                        val isSelected = currentSize == sizeOption
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF222222))
                                                .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MeenaBorder)
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onSetTileSize(target.id, sizeOption)
                                                    Toast.makeText(context, "${target.name} set to $sizeOption tile", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = when (sizeOption) {
                                                    "1x1" -> "1×1 SMALL"
                                                    "4x2" -> "4×2 WIDE"
                                                    else -> "2×2 MEDIUM"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = if (isSelected) Color.White else MeenaTextMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. APP INFO (System Settings)
                        MetroDialogActionRow(
                            title = "App Info",
                            subtitle = "Open system permissions, battery & notifications",
                            accentColor = Color.White,
                            iconText = "⚙️",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                try {
                                    val intent = Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:${target.id}")
                                    )
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open system settings for ${target.name}", Toast.LENGTH_SHORT).show()
                                }
                                selectedTargetForProperties = null
                            }
                        )

                        // 3. CLEAR CACHE
                        MetroDialogActionRow(
                            title = "Clear Cache",
                            subtitle = "Free temporary app memory (Simulated: 42.8 MB freed)",
                            accentColor = Color.White,
                            iconText = "🧹",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                Toast.makeText(context, "${target.name}: Cache cleared (42.8 MB freed)", Toast.LENGTH_SHORT).show()
                                selectedTargetForProperties = null
                            }
                        )

                        // 4. FORCE STOP
                        MetroDialogActionRow(
                            title = "Force Stop",
                            subtitle = "Immediately kill background processes",
                            accentColor = Color.White,
                            iconText = "⏹️",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                Toast.makeText(context, "${target.name}: Application process stopped", Toast.LENGTH_SHORT).show()
                                selectedTargetForProperties = null
                            }
                        )

                        // 5. UNINSTALL APP
                        MetroDialogActionRow(
                            title = "Uninstall App",
                            subtitle = "Delete application package and all local data",
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

                        // Close Button
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
    subtitle: String? = null,
    badge: Int = 0,
    summary: String? = null,
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
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Icon Box with badge
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(iconBg, RoundedCornerShape(0.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = name,
                    modifier = Modifier.size(28.dp)
                )
            } else if (icon != null) {
                Icon(icon, contentDescription = name, tint = Color.White, modifier = Modifier.size(24.dp))
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
                        .background(Color.White)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badge.toString(),
                        fontSize = 9.sp,
                        color = Color.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Titles & Summary
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, style = MaterialTheme.typography.headlineMedium, fontSize = 18.sp)
                if (time != null) {
                    Text(time, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                }
            }

            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            } else if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted,
                    maxLines = 1
                )
            }
        }

        // Dedicated Metro ⋮ action button
        Box(
            modifier = Modifier
                .clickable { onLongClick() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "⋮",
                style = MaterialTheme.typography.titleMedium,
                color = MeenaTextMuted,
                fontSize = 20.sp
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
                    text = "jump to",
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 32.sp,
                    color = Color.White
                )
                Text(
                    text = "✕",
                    style = MaterialTheme.typography.titleLarge,
                    color = MeenaTextMuted,
                    modifier = Modifier.clickable { onDismiss() }.padding(8.dp)
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allChars) { char ->
                    val isActive = activeLetters.contains(char.uppercaseChar()) || activeLetters.contains(char)
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(
                                color = if (isActive) MaterialTheme.colorScheme.primary else Color(0xFF161616),
                                shape = RoundedCornerShape(0.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isActive) Color.White.copy(alpha = 0.3f) else Color(0xFF222222),
                                shape = RoundedCornerShape(0.dp)
                            )
                            .clickable(enabled = isActive) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectLetter(char.uppercaseChar())
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char.toString(),
                            color = if (isActive) Color.White else Color(0xFF444444),
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 22.sp
                        )
                    }
                }
            }
        }
    }
}

