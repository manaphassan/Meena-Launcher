package my.hana.meena

import android.content.Intent
import androidx.lifecycle.lifecycleScope
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.layout.Layout
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.hana.meena.data.preferences.SettingsRepository
import my.hana.meena.data.system.NotificationRepository
import my.hana.meena.data.system.QuickPanelHardwareHelper
import my.hana.meena.theme.MeenaBorderSubtle
import my.hana.meena.theme.MeenaLauncherTheme
import my.hana.meena.theme.MeenaBlack
import my.hana.meena.theme.MeenaTextWhite
import my.hana.meena.theme.MeenaProfitGreen
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.ui.hubs.AppsHub
import my.hana.meena.ui.hubs.FinanceHub
import my.hana.meena.ui.hubs.InboxHub
import my.hana.meena.ui.hubs.NewsHub
import my.hana.meena.ui.hubs.TodayHub
import my.hana.meena.ui.components.MetroDialog
import my.hana.meena.ui.components.metroClickable
import my.hana.meena.ui.settings.SettingsScreen
import androidx.compose.ui.res.stringResource
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
                Box(modifier = Modifier.fillMaxSize().background(MeenaBlack))
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
                fontScalePercent = userSettings.fontScalePercent
            ) {
                my.hana.meena.ui.components.MetroMessageContainer()
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
                    my.hana.meena.ui.components.MetroMessage.show(getString(R.string.error_app_not_installed, packageName))
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
            my.hana.meena.ui.components.MetroMessage.show(getString(R.string.error_open_messages))
        }
    }

    private fun openEmailIntent() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_EMAIL)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            my.hana.meena.ui.components.MetroMessage.show(getString(R.string.error_open_email))
        }
    }

    private fun openCameraIntent() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            my.hana.meena.ui.components.MetroMessage.show(getString(R.string.error_open_camera))
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
            my.hana.meena.ui.components.MetroMessage.show(getString(R.string.error_open_calculator))
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
    val userSettingsState by settingsRepository.userSettingsFlow.collectAsState(initial = null)
    val context = androidx.compose.ui.platform.LocalContext.current

    val userSettings = userSettingsState ?: run {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize(),
            color = MeenaBlack
        ) {}
        return
    }

    var showFirstRunPermissionDialog by remember {
        mutableStateOf(!userSettings.hasCompletedFirstRunSetup)
    }
    var showLocationDisclosure by remember { mutableStateOf(false) }
    var showCalendarDisclosure by remember { mutableStateOf(false) }

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
            my.hana.meena.data.health.HealthRepository.refreshTelemetry(context, userSettings)
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
        my.hana.meena.data.health.HealthRepository.refreshTelemetry(context, userSettings)
    }

    LaunchedEffect(userSettings.stepGoal, userSettings.healthCompanionPackage) {
        my.hana.meena.data.health.HealthRepository.refreshTelemetry(context, userSettings)
    }

    val defaultHubClamped = (userSettings.defaultHubIndex).coerceIn(0, 4)

    val pagerState = rememberPagerState(
        initialPage = defaultHubClamped,
        pageCount = { 5 }
    )

    LaunchedEffect(targetHubIndex) {
        if (targetHubIndex != null && targetHubIndex in 0..4) {
            pagerState.animateScrollToPage(targetHubIndex)
        }
    }

    LaunchedEffect(userSettings.solatZone) {
        val zone = userSettings.solatZone
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

    var isSettingsOpen by remember { mutableStateOf(false) }
    var settingsInitialTab by remember { mutableStateOf("widgets") }
    var isActionCenterOpen by remember { mutableStateOf(false) }
    var isWeatherDetailOpen by remember { mutableStateOf(false) }
    var focusAppsSearch by remember { mutableStateOf(false) }

    // Intercept hardware/gesture Back button (return to today hub)
    BackHandler(enabled = isSettingsOpen || isActionCenterOpen || isWeatherDetailOpen || pagerState.currentPage != 0) {
        when {
            isWeatherDetailOpen -> isWeatherDetailOpen = false
            isSettingsOpen -> isSettingsOpen = false
            isActionCenterOpen -> isActionCenterOpen = false
            pagerState.currentPage != 0 -> {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(0)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Panoramic Hub Pivot Header (WP7 / WP8.1 style - multi-speed parallax with right-edge peek)
            val hubs = listOf("today", "inbox", "news", "money", "apps")
            val headerScrollState = rememberScrollState()
            val localDensity = LocalDensity.current
            val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
            val screenWidthPx = with(localDensity) { screenWidthDp.roundToPx() }
            val startMarginDp = 24.dp
            val startMarginPx = with(localDensity) { startMarginDp.roundToPx() }
            val peekWidthDp = 68.dp
            val peekWidthPx = with(localDensity) { peekWidthDp.roundToPx() }
            val titleSpanPx = ParallaxHelper.calculateTitleSpan(screenWidthPx, startMarginPx, peekWidthPx)

            // Continuous real-time header sync with pager drag position
            LaunchedEffect(pagerState, titleSpanPx) {
                snapshotFlow {
                    val continuousPage = pagerState.currentPage + pagerState.currentPageOffsetFraction
                    val maxScroll = headerScrollState.maxValue
                    Pair(continuousPage, maxScroll)
                }.collect { (continuousPage, maxScroll) ->
                    if (maxScroll > 0) {
                        val targetScroll = ParallaxHelper.calculatePanoramicScrollOffset(
                            continuousPage = continuousPage,
                            pageCount = hubs.size,
                            titleSpanPx = titleSpanPx,
                            maxScroll = maxScroll
                        )
                        headerScrollState.scrollTo(targetScroll)
                    }
                }
            }

            val currentContinuousPage = pagerState.currentPage + pagerState.currentPageOffsetFraction

            // Authentic WP7 Multi-Speed Panoramic Header (slow brand watermark + continuous peek titles)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = my.hana.meena.theme.MeenaSpacing.s)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 20f) {
                                isActionCenterOpen = true
                            }
                        }
                    }
            ) {
                // Layer 1: Ultra-wide Brand Header ("meena") - slow background parallax canvas layer
                Text(
                    text = "meena",
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-1).sp,
                    color = MeenaTextWhite.copy(alpha = 0.22f),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .graphicsLayer {
                            val currentContinuousPage = pagerState.currentPage + pagerState.currentPageOffsetFraction
                            translationX = ParallaxHelper.calculateBrandParallaxOffset(
                                continuousPage = currentContinuousPage,
                                screenWidthPx = screenWidthPx,
                                speedRatio = 0.32f
                            )
                        }
                        .padding(start = my.hana.meena.theme.MeenaSpacing.l)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    settingsInitialTab = "widgets"
                                    isSettingsOpen = true
                                }
                            )
                        }
                )

                // Layer 2: Hub Section Titles - continuous parallax with guaranteed right-edge peek affordance
                Layout(
                    content = {
                        hubs.forEachIndexed { index, title ->
                            val dist = kotlin.math.abs(currentContinuousPage - index)
                            val titleAlpha = (1f - dist * 0.58f).coerceIn(0.42f, 1f)
                            val titleScale = (1f - dist * 0.05f).coerceIn(0.95f, 1f)
                            Text(
                                text = title,
                                style = MaterialTheme.typography.displayLarge,
                                fontSize = 56.sp,
                                lineHeight = 60.sp,
                                fontWeight = FontWeight.Light,
                                softWrap = false,
                                maxLines = 1,
                                color = MeenaTextWhite.copy(alpha = titleAlpha),
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = titleScale
                                        scaleY = titleScale
                                    }
                                    .pointerInput(index) {
                                        detectTapGestures(
                                            onTap = {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(index)
                                                }
                                            },
                                            onLongPress = {
                                                settingsInitialTab = "widgets"
                                                isSettingsOpen = true
                                            }
                                        )
                                    }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(headerScrollState, enabled = false)
                        .padding(top = 0.dp, bottom = my.hana.meena.theme.MeenaSpacing.s)
                ) { measurables, constraints ->
                    val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
                    val lastPlaceableWidth = placeables.lastOrNull()?.width ?: 0
                    val totalWidth = startMarginPx + (hubs.size - 1) * titleSpanPx + lastPlaceableWidth + screenWidthPx
                    val maxHeight = placeables.maxOfOrNull { it.height } ?: 0

                    layout(totalWidth, maxHeight) {
                        placeables.forEachIndexed { index, placeable ->
                            val x = startMarginPx + index * titleSpanPx
                            placeable.placeRelative(x = x, y = 0)
                        }
                    }
                }
            }

            val onTogglePin = { pkgId: String ->
                coroutineScope.launch {
                    val settingsNonNull = userSettings
                    val currentList = settingsNonNull.pinnedAppIds.toMutableList()
                    if (currentList.contains(pkgId)) {
                        currentList.remove(pkgId)
                    } else {
                        currentList.add(pkgId)
                    }
                    settingsRepository.saveSettings(settingsNonNull.copy(pinnedAppIds = currentList))
                }
            }

            // 5 Lateral Panoramic Canvas Hubs with Windows Phone 3D Turnstile Continuity
            val isReduced = userSettings.isReducedMotion
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            if (!isReduced) {
                                // Signature Metro UI Turnstile Parallax & 3D Depth
                                translationX = pageOffset * 36.dp.toPx()
                                alpha = (1f - kotlin.math.abs(pageOffset) * 0.55f).coerceIn(0.20f, 1f)
                                rotationY = (pageOffset * -6.5f).coerceIn(-16f, 16f)
                                cameraDistance = 16f * density
                            } else {
                                alpha = (1f - kotlin.math.abs(pageOffset)).coerceIn(0f, 1f)
                            }
                        }
                ) {
                    when (page) {
                        0 -> TodayHub(
                            settings = userSettings,
                            listState = hubTodayListState,
                            onUpdateSolatZone = { zone ->
                                coroutineScope.launch {
                                    val newSettings = userSettings.copy(solatZone = zone)
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
                            onRequestCalendarPermission = { showCalendarDisclosure = true },
                            onRequestLocationPermission = { showLocationDisclosure = true },
                            onOpenSettings = {
                                settingsInitialTab = "widgets"
                                isSettingsOpen = true
                            },
                            onOpenWeatherDetail = { isWeatherDetailOpen = true },
                            onOpenApps = {
                                coroutineScope.launch { pagerState.animateScrollToPage(4) }
                            },
                            onTogglePin = { pkg -> onTogglePin(pkg) },
                            onReorderPin = { from, to ->
                                coroutineScope.launch {
                                    val currentList = userSettings.pinnedAppIds.toMutableList()
                                    if (from in currentList.indices && to in currentList.indices) {
                                        val item = currentList.removeAt(from)
                                        currentList.add(to, item)
                                        settingsRepository.saveSettings(userSettings.copy(pinnedAppIds = currentList))
                                    }
                                }
                            },
                            onToggleWidgetCollapse = { widgetId, isCollapsed ->
                                coroutineScope.launch {
                                    val currentMap = userSettings.widgetConfigs.toMutableMap()
                                    val existing = currentMap[widgetId] ?: my.hana.meena.data.model.MeenaUserSettings.defaultWidgetMap()[widgetId]
                                    if (existing != null) {
                                        currentMap[widgetId] = existing.copy(isCollapsed = isCollapsed)
                                        settingsRepository.saveSettings(userSettings.copy(widgetConfigs = currentMap))
                                    }
                                }
                            }
                        )
                        1 -> InboxHub(
                            settings = userSettings,
                            listState = hubInboxListState
                        )
                        2 -> NewsHub(
                            settings = userSettings,
                            listState = hubNewsListState
                        )
                        3 -> FinanceHub(
                            settings = userSettings,
                            listState = hubMoneyListState,
                            onOpenSettings = {
                                settingsInitialTab = "widgets"
                                isSettingsOpen = true
                            }
                        )
                        4 -> AppsHub(
                            listState = hubAppsListState,
                            pinnedAppIds = userSettings.pinnedAppIds,
                            focusSearch = focusAppsSearch,
                            reducedMotion = userSettings.reducedMotion,
                            onSearchFocused = { focusAppsSearch = false },
                            onTogglePinApp = { pkg -> onTogglePin(pkg) },
                            onLaunchApp = onLaunchApp
                        )
                    }
                }
            }
        }

        // Edge Gesture: Swipe inward from edge opposite handedness to jump to apps with search focused
        val isLeftHanded = userSettings.isLeftHanded
        val edgeAlignment = if (isLeftHanded) Alignment.CenterEnd else Alignment.CenterStart
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(28.dp)
                .align(edgeAlignment)
                .pointerInput(isLeftHanded) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        val inwardDrag = if (isLeftHanded) -dragAmount else dragAmount
                        if (inwardDrag > 20f) {
                            focusAppsSearch = true
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(4)
                            }
                        }
                    }
                }
        )

        // Weather Detail Sub-Page
        AnimatedVisibility(
            visible = isWeatherDetailOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            val weatherState by my.hana.meena.data.system.WeatherRepository.state.collectAsState()
            val weatherData = (weatherState as? my.hana.meena.ui.widgets.WidgetState.Ready)?.data ?:
                              (weatherState as? my.hana.meena.ui.widgets.WidgetState.Stale)?.data
            val healthState by my.hana.meena.data.health.HealthRepository.state.collectAsState()
            val healthData = (healthState as? my.hana.meena.ui.widgets.WidgetState.Ready)?.data ?:
                             (healthState as? my.hana.meena.ui.widgets.WidgetState.Stale)?.data
            val aqi = healthData?.airIndex

            val tempUnit = userSettings.widgetConfigs["widget-clock-weather"]?.customSettings?.get("temp_unit") ?: "c"
            val isFahrenheit = tempUnit.equals("f", ignoreCase = true)

            my.hana.meena.ui.hubs.WeatherDetailSubPage(
                isFahrenheit = isFahrenheit,
                aqiData = aqi,
                onNavigateBack = { isWeatherDetailOpen = false }
            )
        }

        // Settings Pivot Dialog
        AnimatedVisibility(
            visible = isSettingsOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            val backupSharedMsg = stringResource(R.string.backup_shared)
            val backupShareFailedMsg = stringResource(R.string.backup_share_failed)
            val backupRestoredMsg = stringResource(R.string.backup_restored)
            val backupParseFailedMsg = stringResource(R.string.backup_parse_failed)
            val backupClipboardHintMsg = stringResource(R.string.backup_clipboard_hint)
            val backupImportFailedMsg = stringResource(R.string.backup_import_failed)

            SettingsScreen(
                currentSettings = userSettings,
                initialTab = settingsInitialTab,
                onSaveSettings = { updated ->
                    coroutineScope.launch { settingsRepository.saveSettings(updated) }
                },
                onExportBackup = {
                    coroutineScope.launch {
                        val exported = settingsRepository.exportToJson(userSettings)
                        try {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_SUBJECT, "meena-settings-backup.json")
                                putExtra(Intent.EXTRA_TEXT, exported)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, backupSharedMsg))
                        } catch (e: Exception) {
                            my.hana.meena.ui.components.MetroMessage.show(backupShareFailedMsg)
                        }
                    }
                },
                onImportBackup = {
                    try {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                        if (!clip.isNullOrBlank() && clip.startsWith("{") && clip.contains("widgetConfigs")) {
                            coroutineScope.launch {
                                val success = settingsRepository.importFromJson(clip)
                                if (success) {
                                    my.hana.meena.ui.components.MetroMessage.show(backupRestoredMsg)
                                } else {
                                    my.hana.meena.ui.components.MetroMessage.show(backupParseFailedMsg)
                                }
                            }
                        } else {
                            my.hana.meena.ui.components.MetroMessage.show(backupClipboardHintMsg)
                        }
                    } catch (e: Exception) {
                        my.hana.meena.ui.components.MetroMessage.show(backupImportFailedMsg)
                    }
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
                isLeftHanded = userSettings.isLeftHanded,
                onClose = { isActionCenterOpen = false },
                onOpenSettings = {
                    isActionCenterOpen = false
                    settingsInitialTab = "look & feel"
                    isSettingsOpen = true
                }
            )
        }

        if (showLocationDisclosure) {
            MetroDialog(
                title = stringResource(R.string.disclosure_location_title),
                message = "${stringResource(R.string.disclosure_location_body)}\n\n${stringResource(R.string.disclosure_location_question)}",
                confirmText = stringResource(R.string.action_yes),
                dismissText = stringResource(R.string.action_no),
                onConfirm = {
                    showLocationDisclosure = false
                    locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                },
                onDismiss = { showLocationDisclosure = false }
            )
        }

        if (showCalendarDisclosure) {
            MetroDialog(
                title = stringResource(R.string.disclosure_calendar_title),
                message = "${stringResource(R.string.disclosure_calendar_body)}\n\n${stringResource(R.string.disclosure_calendar_question)}",
                confirmText = stringResource(R.string.action_yes),
                dismissText = stringResource(R.string.action_no),
                onConfirm = {
                    showCalendarDisclosure = false
                    calendarPermissionLauncher.launch(android.Manifest.permission.READ_CALENDAR)
                },
                onDismiss = { showCalendarDisclosure = false }
            )
        }

        // First-Time Setup Permissions Popup Dialog (Authentic Metro UI)
        if (showFirstRunPermissionDialog) {
            MetroDialog(
                title = stringResource(R.string.welcome_title),
                message = stringResource(R.string.welcome_body),
                confirmText = stringResource(R.string.action_yes),
                dismissText = stringResource(R.string.action_no),
                onConfirm = {
                    showFirstRunPermissionDialog = false
                    coroutineScope.launch {
                        settingsRepository.saveSettings(userSettings.copy(hasCompletedFirstRunSetup = true))
                    }
                    permissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.READ_CALENDAR
                        )
                    )
                },
                onDismiss = {
                    showFirstRunPermissionDialog = false
                    coroutineScope.launch {
                        settingsRepository.saveSettings(userSettings.copy(hasCompletedFirstRunSetup = true))
                    }
                }
            )
        }
    }
}

@Composable
fun ActionCenterSheet(
    isLeftHanded: Boolean = false,
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
            .background(MeenaBlack.copy(alpha = 0.88f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MeenaBlack)
                .drawBehind {
                    drawLine(
                        color = MeenaBorderSubtle,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .statusBarsPadding()
                .padding(horizontal = my.hana.meena.theme.MeenaSpacing.l, vertical = my.hana.meena.theme.MeenaSpacing.m)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < -20f) {
                            onClose()
                        }
                    }
                }
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
                    text = stringResource(R.string.quick_panel_title),
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 32.sp,
                    color = MeenaTextWhite
                )
                Text(
                    text = stringResource(R.string.action_close),
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .metroClickable(onClick = onClose)
                        .padding(top = my.hana.meena.theme.MeenaSpacing.m)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Quick Action Tiles (WP8.1 Style with Live Android Hardware State)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
            ) {
                ActionTile(
                    title = stringResource(R.string.quick_panel_wifi),
                    state = wifiState.label,
                    isActive = wifiState.isConnected,
                    modifier = Modifier.weight(1f),
                    onClick = { QuickPanelHardwareHelper.openWifiPanel(context) }
                )
                ActionTile(
                    title = stringResource(R.string.quick_panel_bluetooth),
                    state = bluetoothState.label,
                    isActive = bluetoothState.isEnabled,
                    modifier = Modifier.weight(1f),
                    onClick = { QuickPanelHardwareHelper.openBluetoothPanel(context) }
                )
                ActionTile(
                    title = stringResource(R.string.quick_panel_torch),
                    state = if (isTorchActive) stringResource(R.string.quick_panel_torch_on) else stringResource(R.string.quick_panel_torch_off),
                    isActive = isTorchActive,
                    modifier = Modifier.weight(1f),
                    onClick = { QuickPanelHardwareHelper.toggleTorch(context) }
                )
                ActionTile(
                    title = stringResource(R.string.quick_panel_settings),
                    state = stringResource(R.string.quick_panel_settings_all),
                    isActive = true,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenSettings
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Notifications bar (mirrors based on handedness per Â§2.5)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = my.hana.meena.theme.MeenaSpacing.m, vertical = my.hana.meena.theme.MeenaSpacing.m),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLeftHanded) {
                    Text(
                        text = stringResource(R.string.action_all_settings),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.metroClickable(onClick = onOpenSettings)
                    )
                    Text(stringResource(R.string.quick_panel_notifications), style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                } else {
                    Text(stringResource(R.string.quick_panel_notifications), style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
                    Text(
                        text = stringResource(R.string.action_all_settings),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.metroClickable(onClick = onOpenSettings)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
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
                        app = stringResource(R.string.quick_panel_notifications),
                        text = stringResource(R.string.state_all_quiet),
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
            .heightIn(min = 52.dp)
            .background(if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary)
            .metroClickable(onClick = onClick)
            .padding(my.hana.meena.theme.MeenaSpacing.m)
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
            Spacer(modifier = Modifier.height(2.dp))
            Text(state, style = MaterialTheme.typography.bodySmall, color = MeenaTextWhite.copy(alpha = 0.7f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun ActionNotificationItem(app: String, text: String, time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = my.hana.meena.theme.MeenaSpacing.m, vertical = my.hana.meena.theme.MeenaSpacing.s),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(app, style = MaterialTheme.typography.labelSmall, color = MeenaTextWhite)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MeenaTextMuted)
        }
        if (time.isNotBlank()) {
            Text(time, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
        }
    }
}

