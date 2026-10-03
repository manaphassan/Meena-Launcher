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
import androidx.compose.foundation.border
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import my.hana.meena.ui.navigation.ParallaxHelper
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
import my.hana.meena.data.system.QuickPanelHardwareHelper
import my.hana.meena.theme.MeenaBorderSubtle
import my.hana.meena.theme.MeenaLauncherTheme
import my.hana.meena.theme.MeenaProfitGreen
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.ui.hubs.AppsHub
import my.hana.meena.ui.hubs.FinanceHub
import my.hana.meena.ui.hubs.InboxHub
import my.hana.meena.ui.hubs.NewsHub
import my.hana.meena.ui.hubs.TodayHub
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
                ?: intent?.getIntExtra("target_hub", -1)?.takeIf { it in 0..4 }
            val initialHub = (targetHubExtra ?: userSettings.defaultHubIndex).coerceIn(0, 4)

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
        if (hubExtra in 0..4) {
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
    val context = androidx.compose.ui.platform.LocalContext.current

    if (userSettings == null) {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize(),
            color = androidx.compose.ui.graphics.Color.Black
        ) {}
        return
    }

    var showFirstRunPermissionDialog by remember {
        mutableStateOf(userSettings?.hasCompletedFirstRunSetup == false)
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            my.hana.meena.data.system.DeviceLocationHelper.refreshLocation(context)
            my.hana.meena.data.system.WeatherRepository.refresh(context, force = true)
        }
        if (permissions[android.Manifest.permission.READ_CALENDAR] == true) {
            my.hana.meena.data.system.DeviceCalendarRepository.refresh(context)
        }
        val flat = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val isNotificationAccessGranted = flat != null && flat.contains(context.packageName)
        if (!isNotificationAccessGranted) {
            try {
                val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                context.startActivity(intent)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    val calendarPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            my.hana.meena.data.system.DeviceCalendarRepository.refresh(context)
        }
    }

    val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            my.hana.meena.data.system.DeviceLocationHelper.refreshLocation(context)
            my.hana.meena.data.system.WeatherRepository.refresh(context, force = true)
        }
    }

    LaunchedEffect(Unit) {
        QuickPanelHardwareHelper.initialize(context)
        if (my.hana.meena.data.system.DeviceLocationHelper.hasLocationPermission(context)) {
            my.hana.meena.data.system.DeviceLocationHelper.refreshLocation(context)
            my.hana.meena.data.system.WeatherRepository.refresh(context)
        }
        if (my.hana.meena.data.system.DeviceCalendarRepository.hasCalendarPermission(context)) {
            my.hana.meena.data.system.DeviceCalendarRepository.refresh(context)
        }
    }

    val defaultHubClamped = (userSettings?.defaultHubIndex ?: initialHubIndex).coerceIn(0, 4)

    val pagerState = rememberPagerState(
        initialPage = defaultHubClamped,
        pageCount = { 5 }
    )

    LaunchedEffect(targetHubIndex) {
        if (targetHubIndex != null && targetHubIndex in 0..4) {
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

    val hubTodayListState = rememberLazyListState()
    val hubInboxListState = rememberLazyListState()
    val hubNewsListState = rememberLazyListState()
    val hubMoneyListState = rememberLazyListState()
    val hubAppsListState = rememberLazyListState()

    // Detect if user is actively scrolling in current page or horizontal pager
    val isScrolling by remember {
        derivedStateOf {
            pagerState.isScrollInProgress ||
                    hubTodayListState.isScrollInProgress ||
                    hubInboxListState.isScrollInProgress ||
                    hubNewsListState.isScrollInProgress ||
                    hubMoneyListState.isScrollInProgress ||
                    hubAppsListState.isScrollInProgress
        }
    }

    var isSettingsOpen by remember { mutableStateOf(false) }
    var isActionCenterOpen by remember { mutableStateOf(false) }

    // Intercept hardware/gesture Back button
    BackHandler(enabled = isSettingsOpen || isActionCenterOpen || pagerState.currentPage != defaultHubClamped) {
        when {
            isSettingsOpen -> isSettingsOpen = false
            isActionCenterOpen -> isActionCenterOpen = false
            pagerState.currentPage != defaultHubClamped -> {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(defaultHubClamped)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Panoramic Hub Pivot Header (WP7 / WP8.1 style - continuous drag sync with measured title widths)
            val hubs = listOf("today", "inbox", "news", "money", "apps")
            val headerScrollState = rememberScrollState()
            val titleWidths = remember { mutableStateListOf(0, 0, 0, 0, 0) }
            val localDensity = LocalDensity.current
            val titleSpacingDp = 32.dp
            val titleSpacingPx = with(localDensity) { titleSpacingDp.roundToPx() }

            // Continuous real-time header sync with pager drag position and measured widths
            LaunchedEffect(pagerState) {
                snapshotFlow {
                    val continuousPage = pagerState.currentPage + pagerState.currentPageOffsetFraction
                    val widths = titleWidths.toList()
                    val maxScroll = headerScrollState.maxValue
                    Triple(continuousPage, widths, maxScroll)
                }.collect { (continuousPage, widths, maxScroll) ->
                    if (maxScroll > 0 && widths.any { it > 0 }) {
                        val targetScroll = ParallaxHelper.calculateTitleScrollOffset(
                            continuousPage = continuousPage,
                            titleWidths = widths,
                            spacingPx = titleSpacingPx,
                            maxScroll = maxScroll
                        )
                        headerScrollState.scrollTo(targetScroll)
                    }
                }
            }

            val currentContinuousPage = pagerState.currentPage + pagerState.currentPageOffsetFraction
            val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .horizontalScroll(headerScrollState, enabled = false)
                    .padding(start = 24.dp, top = 12.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(titleSpacingDp),
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
                            .onSizeChanged { size ->
                                if (index < titleWidths.size && titleWidths[index] != size.width) {
                                    titleWidths[index] = size.width
                                }
                            }
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
                // Trailing spacer so the final hub title ("apps") can scroll cleanly to the start margin
                Spacer(modifier = Modifier.width((screenWidthDp - 24.dp).coerceAtLeast(80.dp)))
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
                        0 -> TodayHub(
                            settings = userSettings!!,
                            listState = hubTodayListState,
                            onUpdateSolatZone = { zone ->
                                coroutineScope.launch {
                                    val newSettings = userSettings!!.copy(solatZone = zone)
                                    settingsRepository.saveSettings(newSettings)
                                    my.hana.meena.data.system.JakimSolatRepository.setZone(zone)
                                    my.hana.meena.data.system.JakimSolatRepository.refresh()
                                }
                            },
                            onLaunchPackage = onLaunchApp,
                            onOpenDialer = onOpenDialer,
                            onOpenMessages = onOpenMessages,
                            onOpenEmail = onOpenEmail,
                            onOpenCamera = onOpenCamera,
                            onOpenCalculator = onOpenCalculator,
                            onRequestCalendarPermission = { calendarPermissionLauncher.launch(android.Manifest.permission.READ_CALENDAR) },
                            onRequestLocationPermission = { locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION) }
                        )
                        1 -> InboxHub(
                            settings = userSettings!!,
                            listState = hubInboxListState
                        )
                        2 -> NewsHub(
                            settings = userSettings!!,
                            listState = hubNewsListState
                        )
                        3 -> FinanceHub(
                            settings = userSettings!!,
                            listState = hubMoneyListState
                        )
                        4 -> AppsHub(
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
                coroutineScope.launch { pagerState.animateScrollToPage(4) }
            },
            onOpenSearch = {
                coroutineScope.launch { pagerState.animateScrollToPage(4) }
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

        // First-Time Setup Permissions Popup Dialog (Authentic Metro UI)
        if (showFirstRunPermissionDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = {
                    showFirstRunPermissionDialog = false
                    coroutineScope.launch {
                        settingsRepository.saveSettings(userSettings!!.copy(hasCompletedFirstRunSetup = true))
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0C0C0C))
                        .border(1.dp, MaterialTheme.colorScheme.primary)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "welcome to meena",
                        style = MaterialTheme.typography.displayMedium,
                        fontSize = 24.sp,
                        color = Color.White
                    )
                    Text(
                        text = "to show your local prayer times, weather, radar map, and alerts, meena needs a couple of quick permissions. all your data stays strictly on your phone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MeenaTextMuted
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        androidx.compose.material3.Button(
                            onClick = {
                                showFirstRunPermissionDialog = false
                                coroutineScope.launch {
                                    settingsRepository.saveSettings(userSettings!!.copy(hasCompletedFirstRunSetup = true))
                                }
                                permissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                                        android.Manifest.permission.READ_CALENDAR
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
                        ) {
                            Text(
                                text = "enable",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                        androidx.compose.material3.Button(
                            onClick = {
                                showFirstRunPermissionDialog = false
                                coroutineScope.launch {
                                    settingsRepository.saveSettings(userSettings!!.copy(hasCompletedFirstRunSetup = true))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E1E1E)
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
                        ) {
                            Text(
                                text = "not now",
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaTextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionCenterSheet(
    onClose: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val liveNotifications by NotificationRepository.notificationsFlow.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val wifiState by QuickPanelHardwareHelper.wifiState.collectAsState()
    val bluetoothState by QuickPanelHardwareHelper.bluetoothState.collectAsState()
    val isTorchActive by QuickPanelHardwareHelper.isTorchActive.collectAsState()

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
                    text = "close",
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted,
                    modifier = Modifier.clickable(onClick = onClose)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Quick Action Tiles (WP8.1 Style with Live Android Hardware State)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionTile(
                    title = "wi-fi",
                    state = wifiState.label,
                    isActive = wifiState.isConnected,
                    modifier = Modifier.weight(1f),
                    onClick = { QuickPanelHardwareHelper.openWifiPanel(context) }
                )
                ActionTile(
                    title = "bluetooth",
                    state = bluetoothState.label,
                    isActive = bluetoothState.isEnabled,
                    modifier = Modifier.weight(1f),
                    onClick = { QuickPanelHardwareHelper.openBluetoothPanel(context) }
                )
                ActionTile(
                    title = "torch",
                    state = if (isTorchActive) "on" else "off",
                    isActive = isTorchActive,
                    modifier = Modifier.weight(1f),
                    onClick = { QuickPanelHardwareHelper.toggleTorch(context) }
                )
                ActionTile(
                    title = "settings",
                    state = "all",
                    isActive = true,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenSettings
                )
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
                Text("notifications", style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                Text(
                    "all settings",
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
                        app = "notifications",
                        text = "all caught up",
                        time = ""
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

