package com.example.meenalauncher.ui.settings

import android.content.Intent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.meenalauncher.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meenalauncher.data.model.MeenaUserSettings
import com.example.meenalauncher.theme.MeenaAmber
import com.example.meenalauncher.theme.MeenaBlack
import com.example.meenalauncher.theme.MeenaBorder
import com.example.meenalauncher.theme.MeenaCobalt
import com.example.meenalauncher.theme.MeenaCrimson
import com.example.meenalauncher.theme.MeenaCyan
import com.example.meenalauncher.theme.MeenaDarkSlate
import com.example.meenalauncher.theme.MeenaEmerald
import com.example.meenalauncher.theme.MeenaLime
import com.example.meenalauncher.theme.MeenaMagenta
import com.example.meenalauncher.theme.MeenaOrange
import com.example.meenalauncher.theme.MeenaSurface
import com.example.meenalauncher.theme.MeenaTextMuted
import com.example.meenalauncher.theme.MeenaTextSecondary
import com.example.meenalauncher.theme.MeenaTextWhite

@Composable
fun SettingsScreen(
    currentSettings: MeenaUserSettings,
    onSaveSettings: (MeenaUserSettings) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onResetDefaults: () -> Unit,
    onClose: () -> Unit
) {
    var activeTab by remember { mutableStateOf("basic") }
    val tabs = listOf("basic", "theme", "widgets", "backup", "about")

    val context = LocalContext.current
    var settingsState by remember { mutableStateOf(currentSettings) }

    // Authentic Windows Phone 7 Turnstile 3D Entrance Animation
    var turnstileRotation by remember { mutableFloatStateOf(24f) }
    var turnstileAlpha by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        animate(
            initialValue = 24f,
            targetValue = 0f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        ) { value, _ -> turnstileRotation = value }
    }

    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 250, easing = LinearEasing)
        ) { value, _ -> turnstileAlpha = value }
    }

    fun update(transform: (MeenaUserSettings) -> MeenaUserSettings) {
        val updated = transform(settingsState)
        settingsState = updated
        onSaveSettings(updated)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeenaBlack)
            .graphicsLayer {
                rotationY = turnstileRotation
                alpha = turnstileAlpha
                cameraDistance = 16f * density
            }
    ) {
        // Settings Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Image(
                    painter = painterResource(id = R.drawable.ic_meena_emblem),
                    contentDescription = "Meena Logo",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SETTINGS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted,
                    letterSpacing = 2.sp
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = MeenaTextMuted)
            }
        }

        // Pivot Tabs Header
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(tabs) { tab ->
                val isActive = tab == activeTab
                Text(
                    text = tab,
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 32.sp,
                    color = if (isActive) Color.White else MeenaTextMuted,
                    modifier = Modifier
                        .clickable { activeTab = tab }
                        .padding(bottom = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(MeenaBorder))

        // Content Area
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            when (activeTab) {
                // ==========================================
                // 1. BASIC SETTINGS
                // ==========================================
                "basic" -> {
                    item {
                        SettingSwitchRow(
                            title = "Status bar hiding",
                            subtitle = "Hide top clock & signal for immersive typography",
                            checked = settingsState.isStatusBarHidden,
                            onCheckedChange = { checked ->
                                update { it.copy(isStatusBarHidden = checked) }
                            }
                        )
                    }

                    item {
                        SettingSwitchRow(
                            title = "Handedness ergonomics",
                            subtitle = if (settingsState.isLeftHanded) "Left-handed mode (Nav hub at bottom-left)" else "Right-handed mode (Nav hub at bottom-right)",
                            checked = settingsState.isLeftHanded,
                            onCheckedChange = { checked ->
                                update { it.copy(isLeftHanded = checked) }
                            }
                        )
                    }

                    item {
                        SettingSwitchRow(
                            title = "Haptic feedback",
                            subtitle = "Tactile motor vibrations on button tap & live tile tilt",
                            checked = settingsState.isHapticEnabled,
                            onCheckedChange = { checked ->
                                update { it.copy(isHapticEnabled = checked) }
                            }
                        )
                    }

                    item {
                        Column {
                            Text("DEFAULT START HUB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            val hubNames = listOf("start", "agenda", "news", "finance", "health", "apps")
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                hubNames.chunked(3).forEach { rowHubs ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowHubs.forEach { name ->
                                            val index = hubNames.indexOf(name)
                                            val isSelected = settingsState.defaultHubIndex == index
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MeenaSurface)
                                                    .border(1.dp, if (isSelected) Color.White.copy(alpha = 0.4f) else MeenaBorder)
                                                    .clickable { update { it.copy(defaultHubIndex = index) } }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(name, style = MaterialTheme.typography.labelSmall, color = if (isSelected) Color.White else MeenaTextMuted)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MeenaSurface)
                                .border(1.dp, MeenaBorder)
                                .padding(12.dp)
                        ) {
                            Text("FINANCE & SPENDING INTERCEPTION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Allow Meena to detect bank SMS & eWallet notifications (Maybank, CIMB, TNG, GrabPay) to automatically log spending in the Finance Hub.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MeenaTextMuted
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Ignore if unavailable
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(0.dp)
                            ) {
                                Text("CONFIGURE NOTIFICATION ACCESS →", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    }
                }

                // ==========================================
                // 2. THEME & PERSONALIZATION
                // ==========================================
                "theme" -> {
                    item {
                        Column {
                            Text("BASE THEME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                BaseThemeButton("Pure AMOLED", isSelected = settingsState.baseTheme == "amoled", Modifier.weight(1f)) {
                                    update { it.copy(baseTheme = "amoled") }
                                }
                                BaseThemeButton("Dark Slate", isSelected = settingsState.baseTheme == "slate", Modifier.weight(1f)) {
                                    update { it.copy(baseTheme = "slate") }
                                }
                                BaseThemeButton("Metro Light", isSelected = settingsState.baseTheme == "light", Modifier.weight(1f)) {
                                    update { it.copy(baseTheme = "light") }
                                }
                            }
                        }
                    }

                    item {
                        Column {
                            Text("ACCENT COLOR (8 LUMIA SWATCHES)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            val accents = listOf(
                                "#00A4EF" to MeenaCyan,
                                "#A4C400" to MeenaLime,
                                "#FA6800" to MeenaOrange,
                                "#D80073" to MeenaMagenta,
                                "#0050EF" to MeenaCobalt,
                                "#008A00" to MeenaEmerald,
                                "#E51400" to MeenaCrimson,
                                "#F0A30A" to MeenaAmber
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                accents.take(4).forEach { (hex, col) ->
                                    AccentSwatch(hex, col, isSelected = settingsState.accentHex.equals(hex, ignoreCase = true), Modifier.weight(1f)) {
                                        update { it.copy(accentHex = hex) }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                accents.drop(4).forEach { (hex, col) ->
                                    AccentSwatch(hex, col, isSelected = settingsState.accentHex.equals(hex, ignoreCase = true), Modifier.weight(1f)) {
                                        update { it.copy(accentHex = hex) }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("FONT SCALING", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                                Text("${settingsState.fontScalePercent}%", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                            Slider(
                                value = settingsState.fontScalePercent.toFloat(),
                                onValueChange = { scale ->
                                    update { it.copy(fontScalePercent = scale.toInt()) }
                                },
                                valueRange = 85f..120f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    item {
                        Column {
                            Text("BORDER STYLE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                BaseThemeButton("Subtle Modern (1dp)", isSelected = settingsState.borderStyle != "purist", Modifier.weight(1f)) {
                                    update { it.copy(borderStyle = "modern") }
                                }
                                BaseThemeButton("Purist Flat (0dp)", isSelected = settingsState.borderStyle == "purist", Modifier.weight(1f)) {
                                    update { it.copy(borderStyle = "purist") }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 3. BUILT-IN WIDGETS (15 Modules)
                // ==========================================
                "widgets" -> {
                    item {
                        Text(
                            text = "16 BUILT-IN MODULAR WIDGETS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    val widgetLabels = listOf(
                        "widget-clock-weather" to "1. Clock & Weather (Hero Typography)",
                        "widget-weather-forecast" to "2. Weather Forecast (5-Day Strip)",
                        "widget-my-apps" to "3. My Apps / Live Tiles Grid",
                        "widget-calendar-month" to "4. Monthly Calendar Grid",
                        "widget-today-summary" to "5. Today Summary (Gemini Flash Brief)",
                        "widget-mailbox" to "6. Mailbox (Outlook Unread Stream)",
                        "widget-notes" to "7. Quick Notes",
                        "widget-tasks" to "8. Today's Events / Schedule",
                        "widget-exchange-rates" to "9. Exchange Rates (MYR)",
                        "widget-finance-charts" to "10. Financial Charts (Realtime BTC vs KLSE)",
                        "widget-spending-summary" to "11. Spending Summary (Bank Notification Hook)",
                        "widget-notifications" to "12. Notification Stream",
                        "widget-conversations" to "13. Conversations (Realtime App List)",
                        "widget-news-feed" to "14. News Feed & Watchlist",
                        "widget-device-telemetry" to "15. System Telemetry & Battery Gauge",
                        "widget-map-radar" to "16. Map Radar Location",
                        "widget-health-device" to "17. Connected Device (Smart Band)",
                        "widget-health-telemetry" to "18. Fit Telemetry (Steps/Cal/BPM)",
                        "widget-health-aqi" to "19. Air Quality Index (AQI & UV)"
                    )

                    items(widgetLabels) { (id, label) ->
                        val isEnabled = settingsState.enabledWidgets[id] != false
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MeenaSurface)
                                .border(1.dp, MeenaBorder)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyMedium, color = MeenaTextWhite)
                            Checkbox(
                                checked = isEnabled,
                                onCheckedChange = { checked ->
                                    val newMap = settingsState.enabledWidgets.toMutableMap()
                                    newMap[id] = checked
                                    update { it.copy(enabledWidgets = newMap) }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MeenaBorder
                                )
                            )
                        }
                    }
                }

                // ==========================================
                // 4. BACKUP & RESTORE
                // ==========================================
                "backup" -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("BACKUP LAUNCHER SETTINGS", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("Save your complete layout, themes, and widget settings as a portable JSON file.", style = MaterialTheme.typography.bodyMedium, color = MeenaTextMuted)
                            Button(
                                onClick = onExportBackup,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(0.dp)
                            ) {
                                Text("⬇ EXPORT SETTINGS JSON", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(MeenaBorder))
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("IMPORT / RESTORE SETTINGS", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("Restore preferences from a previously saved JSON backup.", style = MaterialTheme.typography.bodyMedium, color = MeenaTextMuted)
                            Button(
                                onClick = onImportBackup,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MeenaSurface),
                                shape = RoundedCornerShape(0.dp)
                            ) {
                                Text("📂 SELECT BACKUP FILE", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(MeenaBorder))
                    }

                    item {
                        Button(
                            onClick = onResetDefaults,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A1010)),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Text("⚠ RESET TO FACTORY DEFAULTS", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFB4B4))
                        }
                    }
                }

                // ==========================================
                // 5. ABOUT APP
                // ==========================================
                "about" -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Official Meena Launcher Brand Card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF014596), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFF1E6FD9), RoundedCornerShape(12.dp))
                                    .padding(vertical = 24.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_meena_logo_banner),
                                    contentDescription = "Meena Launcher Official Logo",
                                    modifier = Modifier
                                        .fillMaxWidth(0.55f)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.FillWidth
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Meena Launcher", style = MaterialTheme.typography.headlineMedium, fontSize = 24.sp)
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF014596), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("OFFICIAL", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 10.sp)
                                }
                            }
                            Text("Version 1.0.0-PROD (Build 20261002)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(
                                "An authentic Android home screen replacement blending the pure typographic soul of Windows Phone 7/8.1 Metro UI with the extreme information density and modular productivity of AIO Launcher.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MeenaTextSecondary,
                                lineHeight = 20.sp
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MeenaSurface)
                                    .border(1.dp, MeenaBorder)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                InfoRow("Framework:", "Jetpack Compose 1.7.4")
                                InfoRow("Language:", "Kotlin 2.0.21")
                                InfoRow("Target SDK:", "Android 15 (API 35/36)")
                                InfoRow("Developer:", "HaNa Innovation")
                                InfoRow("Design Philosophy:", "Pure AMOLED • 0dp Corners • Content First")
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(48.dp)) }
        }
    }
}

@Composable
private fun SettingSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = MeenaSurface
            )
        )
    }
}

@Composable
private fun BaseThemeButton(name: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .background(MeenaSurface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MeenaBorder
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(name, style = MaterialTheme.typography.labelSmall, color = if (isSelected) Color.White else MeenaTextMuted)
    }
}

@Composable
private fun AccentSwatch(hex: String, color: Color, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(44.dp)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color.White else Color.Black.copy(alpha = 0.4f)
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
        Text(value, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
    }
}
