package my.hana.meena

import android.content.Intent
import androidx.lifecycle.lifecycleScope
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.hana.meena.data.preferences.SettingsRepository
import my.hana.meena.data.system.NotificationRepository
import my.hana.meena.theme.MeenaBorderSubtle
import my.hana.meena.theme.MeenaLauncherTheme
import my.hana.meena.theme.MeenaProfitGreen
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.ui.hubs.AgendaHub
import my.hana.meena.ui.hubs.AppsHub
import my.hana.meena.ui.hubs.FinanceHub
import my.hana.meena.ui.hubs.HealthHub
import my.hana.meena.ui.hubs.NewsHub
import my.hana.meena.ui.hubs.StartHub
import my.hana.meena.ui.navigation.CornerNavHub
import my.hana.meena.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepository = SettingsRepository(applicationContext)

        enableEdgeToEdge()
        setContent {
            val userSettingsFlowState = settingsRepository.userSettingsFlow.collectAsState(
                initial = null
            )
            val userSettings = userSettingsFlowState.value

            if (userSettings == null) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                return@setContent
            }

            val parsedAccent = try {
                Color(android.graphics.Color.parseColor(userSettings.accentHex))
            } catch (e: Exception) {
                my.hana.meena.theme.MeenaCyan
            }

            val targetHubExtra = currentHubIntentIndex.value
                ?: intent?.getIntExtra("target_hub", -1)?.takeIf { it in 0..5 }
            val initialHub = targetHubExtra ?: userSettings.defaultHubIndex

            MeenaLauncherTheme(
                baseTheme = userSettings.baseTheme,
                accentColor = parsedAccent,
                fontFamilyName = userSettings.fontFamily,
                fontScalePercent = userSettings.fontScalePercent
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MeenaHomeScreen(
                        settingsRepository = settingsRepository,
                        initialHubIndex = initialHub,
                        targetHubIndex = currentHubIntentIndex.value,
                        onLaunchApp = { packageName -> launchPackage(packageName) },
                        onOpenDialer = { openDialerIntent() },
                        onOpenMessages = { openMessagesIntent() },
                        onOpenEmail = { openEmailIntent() },
                        onOpenCamera = { openCameraIntent() },
                        onOpenCalculator = { openCalculatorIntent() }
                    )
                }
            }
        }
    }

    private var currentHubIntentIndex = mutableStateOf<Int?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val hubExtra = intent.getIntExtra("target_hub", -1)
        if (hubExtra in 0..5) {
            currentHubIntentIndex.value = hubExtra
        }
    }

    private fun launchPackage(packageName: String) {
        lifecycleScope.launch {
            val app = my.hana.meena.data.system.InstalledAppsRepository.loadInstalledApps(this@MainActivity)
                .find { it.packageName == packageName }
            if (app?.componentName != null && app.userHandle != null) {
                val launcherApps = getSystemService(android.content.Context.LAUNCHER_APPS_SERVICE) as android.content.pm.LauncherApps
                launcherApps.startMainActivity(app.componentName, app.userHandle, null, null)
            } else {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    startActivity(launchIntent)
                } else {
                    android.widget.Toast.makeText(this@MainActivity, "App $packageName not installed", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openDialerIntent() {
        val intent = Intent(Intent.ACTION_DIAL)
        startActivity(intent)
    }

    private fun openMessagesIntent() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_MESSAGING)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Opening default messages app...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openEmailIntent() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_EMAIL)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Opening default email app...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCameraIntent() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Camera intent launched", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCalculatorIntent() {
        try {
            val intent = Intent().apply {
                setAction(Intent.ACTION_MAIN)
                addCategory(Intent.CATEGORY_APP_CALCULATOR)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Calculator opened", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun MeenaHomeScreen(
    settingsRepository: SettingsRepository,
    initialHubIndex: Int = 0,
    targetHubIndex: Int? = null,
    onLaunchApp: (String) -> Unit,
    onOpenDialer: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenEmail: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenCalculator: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val userSettings by settingsRepository.userSettingsFlow.collectAsState(initial = null)

    if (userSettings == null) {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize(),
            color = androidx.compose.ui.graphics.Color.Black
        ) {}
        return
    }

    val pagerState = rememberPagerState(
        initialPage = userSettings?.defaultHubIndex ?: initialHubIndex,
        pageCount = { 6 }
    )

    LaunchedEffect(targetHubIndex) {
        if (targetHubIndex != null && targetHubIndex in 0..5) {
            pagerState.animateScrollToPage(targetHubIndex)
        }
    }

    LaunchedEffect(userSettings?.solatZone) {
        val zone = userSettings?.solatZone
        if (!zone.isNullOrBlank()) {
            my.hana.meena.data.system.JakimSolatRepository.setZone(zone)
            my.hana.meena.data.system.JakimSolatRepository.refresh()
        }
    }

    val hubStartListState = rememberLazyListState()
    val hubAgendaListState = rememberLazyListState()
    val hubNewsListState = rememberLazyListState()
    val hubFinanceListState = rememberLazyListState()
    val hubHealthListState = rememberLazyListState()
    val hubAppsListState = rememberLazyListState()

    // Detect if user is actively scrolling in current page or horizontal pager
    val isScrolling by remember {
        derivedStateOf {
            pagerState.isScrollInProgress ||
                    hubStartListState.isScrollInProgress ||
                    hubAgendaListState.isScrollInProgress ||
                    hubNewsListState.isScrollInProgress ||
                    hubFinanceListState.isScrollInProgress ||
                    hubHealthListState.isScrollInProgress ||
                    hubAppsListState.isScrollInProgress
        }
    }

    var isSettingsOpen by remember { mutableStateOf(false) }
    var isActionCenterOpen by remember { mutableStateOf(false) }

    // Intercept hardware/gesture Back button
    BackHandler(enabled = isSettingsOpen || isActionCenterOpen || pagerState.currentPage != (userSettings?.defaultHubIndex ?: 0)) {
        when {
            isSettingsOpen -> isSettingsOpen = false
            isActionCenterOpen -> isActionCenterOpen = false
            pagerState.currentPage != (userSettings?.defaultHubIndex ?: 0) -> {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(userSettings?.defaultHubIndex ?: 0)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Panoramic Hub Pivot Header (WP7 / WP8.1 style - continuous drag sync with 1:1 parallax)
            val hubs = listOf("start", "agenda", "news", "finance", "health", "apps")
            val headerScrollState = rememberScrollState()

            // Continuous real-time header sync with pager drag position
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage + pagerState.currentPageOffsetFraction }
                    .collect { continuousPage ->
                        if (headerScrollState.maxValue > 0) {
                            val targetScroll = (continuousPage * 135.dp.value).toInt().coerceIn(0, headerScrollState.maxValue)
                            headerScrollState.scrollTo(targetScroll)
                        }
                    }
            }

            val currentContinuousPage = pagerState.currentPage + pagerState.currentPageOffsetFraction

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .horizontalScroll(headerScrollState)
                    .padding(start = 20.dp, end = 60.dp, top = 12.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                hubs.forEachIndexed { index, title ->
                    val dist = kotlin.math.abs(currentContinuousPage - index)
                    val titleAlpha = (1f - dist * 0.62f).coerceIn(0.35f, 1f)
                    val titleScale = (1f - dist * 0.08f).coerceIn(0.92f, 1f)
                    Text(
                        text = title,
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = 38.sp,
                        softWrap = false,
                        maxLines = 1,
                        color = Color.White.copy(alpha = titleAlpha),
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = titleScale
                                scaleY = titleScale
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                    )
                }
            }

            val onTogglePin = { pkgId: String ->
                coroutineScope.launch {
                    val settingsNonNull = userSettings!!
                    val currentList = settingsNonNull.pinnedAppIds.toMutableList()
                    if (currentList.contains(pkgId)) {
                        currentList.remove(pkgId)
                    } else {
                        currentList.add(pkgId)
                    }
                    settingsRepository.saveSettings(settingsNonNull.copy(pinnedAppIds = currentList))
                }
            }

            // 6 Lateral Panoramic Canvas Hubs with Windows Phone 3D Turnstile Continuity
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Signature Metro UI Turnstile Parallax & 3D Depth
                            translationX = pageOffset * 48.dp.toPx()
                            alpha = (1f - kotlin.math.abs(pageOffset) * 0.65f).coerceIn(0.15f, 1f)
                            rotationY = (pageOffset * -7.5f).coerceIn(-20f, 20f)
                            cameraDistance = 18f * density
                        }
                ) {
                    when (page) {
                        0 -> StartHub(
                            settings = userSettings!!,
                            listState = hubStartListState,
                            onLaunchPackage = onLaunchApp,
                            onOpenDialer = onOpenDialer,
                            onOpenMessages = onOpenMessages,
                            onOpenEmail = onOpenEmail,
                            onOpenCamera = onOpenCamera,
                            onOpenCalculator = onOpenCalculator
                        )
                        1 -> AgendaHub(
                            settings = userSettings!!,
                            listState = hubAgendaListState,
                            onUpdateSolatZone = { zone ->
                                coroutineScope.launch {
                                    val newSettings = userSettings!!.copy(solatZone = zone)
                                    settingsRepository.saveSettings(newSettings)
                                    my.hana.meena.data.system.JakimSolatRepository.setZone(zone)
                                    my.hana.meena.data.system.JakimSolatRepository.refresh()
                                }
                            }
                        )
                        2 -> NewsHub(
                            settings = userSettings!!,
                            listState = hubNewsListState
                        )
                        3 -> FinanceHub(
                            settings = userSettings!!,
                            listState = hubFinanceListState
                        )
                        4 -> HealthHub(
                            settings = userSettings!!,
                            listState = hubHealthListState
                        )
                        5 -> AppsHub(
                            listState = hubAppsListState,
                            pinnedAppIds = userSettings!!.pinnedAppIds,
                            pinnedAppSizes = userSettings!!.pinnedAppSizes,
                            onTogglePinApp = { pkg -> onTogglePin(pkg) },
                            onSetTileSize = { appId, size ->
                                coroutineScope.launch {
                                    val currentMap = userSettings!!.pinnedAppSizes.toMutableMap()
                                    currentMap[appId] = size
                                    settingsRepository.saveSettings(userSettings!!.copy(pinnedAppSizes = currentMap))
                                }
                            },
                            onLaunchApp = onLaunchApp
                        )
                    }
                }
            }
        }

        // Single Button Bottom-Right Navigation Hub
        CornerNavHub(
            isLeftHanded = userSettings!!.isLeftHanded,
            isScrolling = isScrolling,
            onNavigateToApps = {
                coroutineScope.launch { pagerState.animateScrollToPage(5) }
            },
            onOpenSearch = {
                coroutineScope.launch { pagerState.animateScrollToPage(5) }
            },
            onOpenCamera = onOpenCamera,
            onOpenSettings = { isSettingsOpen = true },
            onOpenDialer = onOpenDialer,
            onOpenMessages = onOpenMessages,
            onOpenEmail = onOpenEmail,
            onOpenCalculator = onOpenCalculator,
            onOpenTaskSwitcher = {
                isActionCenterOpen = true
            }
        )

        // Settings Pivot Dialog
        AnimatedVisibility(
            visible = isSettingsOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            SettingsScreen(
                currentSettings = userSettings!!,
                onSaveSettings = { updated ->
                    coroutineScope.launch { settingsRepository.saveSettings(updated) }
                },
                onExportBackup = {
                    coroutineScope.launch {
                        val exported = settingsRepository.exportToJson(userSettings!!)
                        // Can be saved or shared
                    }
                },
                onImportBackup = {
                    // Import logic hook
                },
                onResetDefaults = {
                    coroutineScope.launch { settingsRepository.resetToFactoryDefaults() }
                },
                onClose = { isSettingsOpen = false }
            )
        }

        // Action Center Dropdown Pull-Down (Windows Phone 8.1 style)
        AnimatedVisibility(
            visible = isActionCenterOpen,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it })
        ) {
            ActionCenterSheet(
                onClose = { isActionCenterOpen = false },
                onOpenSettings = {
                    isActionCenterOpen = false
                    isSettingsOpen = true
                }
            )
        }
    }
}

@Composable
fun ActionCenterSheet(
    onClose: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val liveNotifications by NotificationRepository.notificationsFlow.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .drawBehind {
                    drawLine(
                        color = MeenaBorderSubtle,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "action center",
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 32.sp,
                    color = Color.White
                )
                Text(
                    text = "CLOSE ✕",
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted,
                    modifier = Modifier.clickable(onClick = onClose)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Quick Action Tiles (WP8.1 Style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionTile("Wi-Fi", "Connected", true, Modifier.weight(1f))
                ActionTile("Bluetooth", "Active", true, Modifier.weight(1f))
                ActionTile("Torch", "Off", false, Modifier.weight(1f))
                ActionTile("Settings", "All", true, Modifier.weight(1f), onClick = onOpenSettings)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Notifications bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141414))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("NOTIFICATIONS", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                Text(
                    "ALL SETTINGS →",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onOpenSettings)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (liveNotifications.isNotEmpty()) {
                    liveNotifications.take(4).forEach { item ->
                        ActionNotificationItem(
                            app = item.appName.ifBlank { item.packageName.substringAfterLast('.') },
                            text = if (item.text.isNotBlank()) "${item.title}: ${item.text}" else item.title,
                            time = item.formattedTime
                        )
                    }
                } else {
                    ActionNotificationItem(
                        app = "Meena Launcher",
                        text = "AMOLED 0dp Pure Metro • System Ready",
                        time = "Just now"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ActionTile(
    title: String,
    state: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .background(if (isActive) MaterialTheme.colorScheme.primary else Color(0xFF1E1E1E))
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color.White)
            Spacer(modifier = Modifier.height(2.dp))
            Text(state, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun ActionNotificationItem(app: String, text: String, time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141414))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(app, style = MaterialTheme.typography.labelSmall, color = Color.White)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MeenaTextMuted)
        }
        Text(time, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
    }
}

