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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import my.hana.meena.ui.navigation.ParallaxHelper
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable
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

/** The last base theme, kept outside DataStore so it can be read before the first frame. */
private object LaunchTheme {
    private const val PREFS = "meena_launch_theme"
    private const val KEY = "base_theme"

    fun read(context: android.content.Context): String =
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).getString(KEY, null) ?: "dark"

    fun write(context: android.content.Context, baseTheme: String) {
        val prefs = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        if (prefs.getString(KEY, null) != baseTheme) prefs.edit { putString(KEY, baseTheme) }
    }
}

/** Stands in for the phone's haptics when vibration is turned off in settings. */
private object SilentHaptics : androidx.compose.ui.hapticfeedback.HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: androidx.compose.ui.hapticfeedback.HapticFeedbackType) = Unit
}

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepository = SettingsRepository(applicationContext)
        if (savedInstanceState == null) showPrivacyPolicyIfAsked(intent)

        enableEdgeToEdge()
        // Settings load a moment after launch; start in the last theme so there is no flash of the wrong colour.
        val startTheme = LaunchTheme.read(this)
        window.setBackgroundDrawable(
            my.hana.meena.theme.meenaPaletteFor(startTheme).background.toArgb().toDrawable()
        )
        setContent {
            val userSettingsFlowState = settingsRepository.userSettingsFlow.collectAsState(
                initial = null
            )
            val userSettings = userSettingsFlowState.value

            if (userSettings == null) {
                Box(modifier = Modifier.fillMaxSize().background(my.hana.meena.theme.meenaPaletteFor(startTheme).background))
                return@setContent
            }
            LaunchedEffect(userSettings.baseTheme) { LaunchTheme.write(this@MainActivity, userSettings.baseTheme) }

            val parsedAccent = try {
                Color(android.graphics.Color.parseColor(userSettings.accentHex))
            } catch (e: Exception) {
                my.hana.meena.theme.MeenaCyan
            }

            val targetHubExtra = currentHubIntentIndex.value
                ?: intent?.getIntExtra("target_hub", -1)?.takeIf { it in 0..4 }
            val initialHub = (targetHubExtra ?: userSettings.defaultHubIndex).coerceIn(0, 4)

            val hideStatusBar = userSettings.isStatusBarHidden
            LaunchedEffect(hideStatusBar, userSettings.baseTheme) {
                val bars = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                bars.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                val statusBars = androidx.core.view.WindowInsetsCompat.Type.statusBars()
                val lightPage = my.hana.meena.theme.meenaPaletteFor(userSettings.baseTheme).isLight
                bars.isAppearanceLightStatusBars = lightPage
                bars.isAppearanceLightNavigationBars = lightPage
                if (hideStatusBar) bars.hide(statusBars) else bars.show(statusBars)
            }

            val systemHaptics = LocalHapticFeedback.current
            CompositionLocalProvider(
                LocalHapticFeedback provides if (userSettings.isHapticEnabled) systemHaptics else SilentHaptics
            ) {
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
                            homePresses = homePresses.value,
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
    }

    private var currentHubIntentIndex = mutableStateOf<Int?>(null)

    /** Counts home presses while Meena is already in front; each one goes back to today. */
    private var homePresses = mutableStateOf(0)

    override fun onNewIntent(intent: Intent) {
        // Read focus before the new intent is handled: true when the person is already on Meena.
        val alreadyOnHome = hasWindowFocus() &&
            (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) == 0
        super.onNewIntent(intent)
        setIntent(intent)
        showPrivacyPolicyIfAsked(intent)
        val hubExtra = intent.getIntExtra("target_hub", -1)
        if (hubExtra in 0..4) {
            currentHubIntentIndex.value = hubExtra
        } else if (alreadyOnHome && intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            homePresses.value += 1
        }
    }

    /** Health Connect's "privacy policy" link lands here; show the same policy as Play Console. */
    private fun showPrivacyPolicyIfAsked(intent: Intent?) {
        if (intent?.action in my.hana.meena.data.system.PrivacyPolicy.RATIONALE_ACTIONS) {
            if (!my.hana.meena.data.system.PrivacyPolicy.open(this)) {
                my.hana.meena.ui.components.MetroMessage.show(getString(R.string.privacy_policy_open_failed))
            }
        }
    }

    private fun launchPackage(packageName: String) {
        lifecycleScope.launch {
            val app = my.hana.meena.data.system.InstalledAppsRepository.loadInstalledApps(this@MainActivity)
                .find { it.packageName == packageName }
            if (app?.componentName != null && app.userHandle != null) {
                val launcherApps = getSystemService(android.content.Context.LAUNCHER_APPS_SERVICE) as android.content.pm.LauncherApps
                launcherApps.startMainActivity(app.componentName, app.userHandle, null, null)
                my.hana.meena.data.system.AppLaunchStats.record(this@MainActivity, packageName)
            } else {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    startActivity(launchIntent)
                    my.hana.meena.data.system.AppLaunchStats.record(this@MainActivity, packageName)
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
    homePresses: Int = 0,
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
    var showBluetoothDisclosure by remember { mutableStateOf(false) }

    val bluetoothPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { _ ->
        my.hana.meena.data.system.BluetoothDevicesRepository.refresh(context)
    }

    // The home role prompt. A "no" there is respected; settings, system, home app stays available.
    val homeRoleLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
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
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Android 12+ asks for both together and the person can still choose approximate.
        if (results.values.any { it }) {
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

    LaunchedEffect(userSettings.iconPack) {
        runCatching { my.hana.meena.data.system.InstalledAppsRepository.setIconPack(context.applicationContext, userSettings.iconPack) }
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

    // A zone picked in settings wins; on auto, use the zone worked out from the rough location.
    val autoSolatZone by remember {
        my.hana.meena.data.system.DeviceLocationHelper.locationFlow
            .map { it.solatZone }
            .distinctUntilChanged()
    }.collectAsState(initial = null)
    LaunchedEffect(userSettings.solatZone, autoSolatZone) {
        val zone = userSettings.solatZone.ifBlank { autoSolatZone.orEmpty() }
        if (zone.isNotBlank()) {
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
    var isNearbyMapOpen by remember { mutableStateOf(false) }
    var focusAppsSearch by remember { mutableStateOf(false) }

    // Shake to hide private content. The accelerometer needs no permission and is only read while
    // Meena is in front. Hidden stays hidden until the next shake (or Meena restarts).
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val hiddenMessage = stringResource(R.string.shake_hidden_message)
    val shownMessage = stringResource(R.string.shake_shown_message)
    androidx.compose.runtime.DisposableEffect(userSettings.shakeToHide, lifecycleOwner) {
        if (!userSettings.shakeToHide) my.hana.meena.data.system.PrivacyVeil.show()
        val sensors = context.getSystemService(android.content.Context.SENSOR_SERVICE) as? android.hardware.SensorManager
        val accelerometer = sensors?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
        val detector = my.hana.meena.data.system.ShakeDetector {
            val hidden = my.hana.meena.data.system.PrivacyVeil.toggle()
            my.hana.meena.ui.components.MetroMessage.show(if (hidden) hiddenMessage else shownMessage)
        }
        val listener = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(event: android.hardware.SensorEvent) {
                detector.onReading(event.values[0], event.values[1], event.values[2], android.os.SystemClock.elapsedRealtime())
            }
            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
        }
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> if (userSettings.shakeToHide && accelerometer != null) {
                    sensors.registerListener(listener, accelerometer, android.hardware.SensorManager.SENSOR_DELAY_UI)
                }
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> sensors?.unregisterListener(listener)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sensors?.unregisterListener(listener)
        }
    }

    // Home pressed while already on Meena: close what's open and go back to today.
    LaunchedEffect(homePresses) {
        if (homePresses > 0) {
            isNearbyMapOpen = false
            isWeatherDetailOpen = false
            isSettingsOpen = false
            isActionCenterOpen = false
            pagerState.animateScrollToPage(0)
        }
    }

    // Intercept hardware/gesture Back button (return to today hub)
    BackHandler(enabled = isSettingsOpen || isActionCenterOpen || isWeatherDetailOpen || isNearbyMapOpen || pagerState.currentPage != 0) {
        when {
            isNearbyMapOpen -> isNearbyMapOpen = false
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
                    text = my.hana.meena.data.model.HomeTitle.display(userSettings.homeTitle),
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-1).sp,
                    // A faint ghost on black; on light pages and e-ink a faint grey disappears, so use solid grey.
                    color = if (my.hana.meena.theme.LocalMeenaPalette.current.isLight) MeenaTextMuted else MeenaTextWhite.copy(alpha = 0.22f),
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

            val favouritesFullMsg = stringResource(R.string.favourites_full)
            val onTogglePin = { pkgId: String ->
                coroutineScope.launch {
                    val settingsNonNull = userSettings
                    val updated = my.hana.meena.data.system.FavouriteRules.toggle(settingsNonNull.pinnedAppIds, pkgId) {
                        my.hana.meena.data.system.InstalledAppsRepository.getCachedApp(it) != null
                    }
                    if (updated == null) {
                        my.hana.meena.ui.components.MetroMessage.show(favouritesFullMsg)
                    } else {
                        settingsRepository.saveSettings(settingsNonNull.copy(pinnedAppIds = updated))
                    }
                }
            }

            // Leaving a page drops its focus, so the apps search box doesn't keep the keyboard up
            // or keep the apps page alive behind Today.
            val pageFocusManager = androidx.compose.ui.platform.LocalFocusManager.current
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.settledPage }.collect { pageFocusManager.clearFocus() }
            }

            // 5 Lateral Panoramic Canvas Hubs with Windows Phone 3D Turnstile Continuity
            val isReduced = userSettings.reducedMotion
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
                            // A page more than a step away must never show. The pager keeps a focused page (like
                            // the apps search box) laid out next to the current one, and the shift below would
                            // otherwise pull it onto the screen at the minimum alpha.
                            if (kotlin.math.abs(pageOffset) >= 1.5f) {
                                alpha = 0f
                            } else if (!isReduced) {
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
                            onRequestBluetoothPermission = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) showBluetoothDisclosure = true
                            },
                            onOpenSettings = {
                                settingsInitialTab = "widgets"
                                isSettingsOpen = true
                            },
                            onOpenWeatherDetail = { isWeatherDetailOpen = true },
                            onOpenNearbyMap = { isNearbyMapOpen = true },
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
                            onSetPinned = { updated ->
                                coroutineScope.launch { settingsRepository.saveSettings(userSettings.copy(pinnedAppIds = updated)) }
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
                            isMonochrome = userSettings.isMonochromeFavourites,
                            is24Hour = userSettings.is24HourFormat,
                            sortMode = userSettings.appsSortMode,
                            onSortModeChange = { mode ->
                                coroutineScope.launch { settingsRepository.saveSettings(userSettings.copy(appsSortMode = mode)) }
                            },
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
                animate = !userSettings.reducedMotion,
                onNavigateBack = { isWeatherDetailOpen = false }
            )
        }

        // Nearby map sub-page
        AnimatedVisibility(
            visible = isNearbyMapOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            val mapConfig = userSettings.widgetConfigs["widget-map-radar"]
            my.hana.meena.ui.hubs.NearbyMapSubPage(
                ringsOn = mapConfig?.customSettings?.get("rings") != "off",
                animate = !userSettings.reducedMotion,
                is24Hour = userSettings.is24HourFormat,
                onRingsChange = { on ->
                    coroutineScope.launch {
                        val currentMap = userSettings.widgetConfigs.toMutableMap()
                        val existing = currentMap["widget-map-radar"] ?: my.hana.meena.data.model.MeenaUserSettings.defaultWidgetMap()["widget-map-radar"]
                        if (existing != null) {
                            currentMap["widget-map-radar"] = existing.copy(customSettings = existing.customSettings + ("rings" to if (on) "on" else "off"))
                            settingsRepository.saveSettings(userSettings.copy(widgetConfigs = currentMap))
                        }
                    }
                },
                onNavigateBack = { isNearbyMapOpen = false }
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
                    locationPermissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
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

        if (showBluetoothDisclosure && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            MetroDialog(
                title = stringResource(R.string.disclosure_bluetooth_title),
                message = "${stringResource(R.string.disclosure_bluetooth_body)}\n\n${stringResource(R.string.disclosure_bluetooth_question)}",
                confirmText = stringResource(R.string.action_yes),
                dismissText = stringResource(R.string.action_no),
                onConfirm = {
                    showBluetoothDisclosure = false
                    bluetoothPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
                },
                onDismiss = { showBluetoothDisclosure = false }
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
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
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

        // After the welcome, ask once to become the home app (if Meena isn't already).
        val needsHomePrompt = remember(userSettings.hasCompletedFirstRunSetup, userSettings.homePromptAnswered) {
            userSettings.hasCompletedFirstRunSetup && !userSettings.homePromptAnswered &&
                !my.hana.meena.data.system.HomeRole.isDefault(context)
        }
        if (needsHomePrompt && !showFirstRunPermissionDialog) {
            val answer = {
                coroutineScope.launch {
                    settingsRepository.saveSettings(userSettings.copy(homePromptAnswered = true))
                }
            }
            MetroDialog(
                title = stringResource(R.string.home_app_title),
                message = stringResource(R.string.home_app_question),
                confirmText = stringResource(R.string.action_yes),
                dismissText = stringResource(R.string.action_no),
                onConfirm = {
                    answer()
                    val request = my.hana.meena.data.system.HomeRole.requestIntent(context)
                    if (request != null) homeRoleLauncher.launch(request)
                    else my.hana.meena.data.system.HomeRole.openPicker(context)
                },
                onDismiss = { answer() }
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

    val dividerColor = MeenaBorderSubtle
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
                        color = dividerColor,
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

            // Notifications bar (mirrors based on handedness per §2.5)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(my.hana.meena.theme.MeenaAccentFill)
                    .padding(horizontal = my.hana.meena.theme.MeenaSpacing.m, vertical = my.hana.meena.theme.MeenaSpacing.m),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLeftHanded) {
                    Text(
                        text = stringResource(R.string.action_all_settings),
                        style = MaterialTheme.typography.labelSmall,
                        color = my.hana.meena.theme.MeenaOnAccent,
                        modifier = Modifier.metroClickable(onClick = onOpenSettings)
                    )
                    Text(stringResource(R.string.quick_panel_notifications), style = MaterialTheme.typography.labelSmall, color = my.hana.meena.theme.MeenaOnAccent)
                } else {
                    Text(stringResource(R.string.quick_panel_notifications), style = MaterialTheme.typography.labelSmall, color = my.hana.meena.theme.MeenaOnAccent)
                    Text(
                        text = stringResource(R.string.action_all_settings),
                        style = MaterialTheme.typography.labelSmall,
                        color = my.hana.meena.theme.MeenaOnAccent,
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
            .background(my.hana.meena.theme.MeenaAccentFill)
            .metroClickable(onClick = onClick)
            .padding(my.hana.meena.theme.MeenaSpacing.m)
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.labelSmall, color = my.hana.meena.theme.MeenaOnAccent)
            Spacer(modifier = Modifier.height(2.dp))
            Text(state, style = MaterialTheme.typography.bodySmall, color = my.hana.meena.theme.MeenaOnAccent, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ActionNotificationItem(app: String, text: String, time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(my.hana.meena.theme.MeenaAccentFill)
            .padding(horizontal = my.hana.meena.theme.MeenaSpacing.m, vertical = my.hana.meena.theme.MeenaSpacing.s),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(app, style = MaterialTheme.typography.labelSmall, color = my.hana.meena.theme.MeenaOnAccent)
            Text(text, style = MaterialTheme.typography.bodySmall, color = my.hana.meena.theme.MeenaOnAccent)
        }
        if (time.isNotBlank()) {
            Text(time, style = MaterialTheme.typography.labelSmall, color = my.hana.meena.theme.MeenaOnAccent)
        }
    }
}

