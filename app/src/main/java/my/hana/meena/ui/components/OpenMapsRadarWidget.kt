package my.hana.meena.ui.components

import my.hana.meena.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.heightIn
import my.hana.meena.data.system.ClockInfo
import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.MotionEvent
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import kotlin.math.roundToInt
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import my.hana.meena.data.system.DeviceLocation
import my.hana.meena.data.system.DeviceLocationHelper
import my.hana.meena.theme.MeenaBorder
import my.hana.meena.theme.MeenaProfitGreen
import my.hana.meena.theme.MeenaSurface
import my.hana.meena.theme.MeenaSurfaceElevated
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextSecondary
import my.hana.meena.theme.MeenaTextWhite
import my.hana.meena.theme.MeenaBlack

/**
 * Map of the user's rough area on OpenFreeMap's dark vector style (OpenStreetMap data), drawn by
 * MapLibre inside Leaflet. Shown only after a real location fix; credits OpenFreeMap, OpenMapTiles
 * and OpenStreetMap as their terms require.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OpenMapsRadarWidget(
    modifier: Modifier = Modifier,
    ringsOn: Boolean = true,
    animate: Boolean = true,
    is24Hour: Boolean = true,
    onRingsChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val currentLocation by DeviceLocationHelper.locationFlow.collectAsState()

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        DeviceLocationHelper.refreshLocation(context)
    }

    val accentHex = String.format("#%06X", 0xFFFFFF and MaterialTheme.colorScheme.primary.toArgb())
    val radiusMeters = currentLocation.accuracyMeters.coerceIn(100f, 5000f)
    // OpenFreeMap's own styles (https://openfreemap.org/quick_start/): dark on black, positron on white and paper.
    val isLightTheme = my.hana.meena.theme.LocalMeenaPalette.current.isLight
    val styleUrl = if (isLightTheme) "https://tiles.openfreemap.org/styles/positron" else "https://tiles.openfreemap.org/styles/dark"
    val pageColor = String.format("#%06X", 0xFFFFFF and my.hana.meena.theme.LocalMeenaPalette.current.background.toArgb())

    LaunchedEffect(currentLocation.latitude, currentLocation.longitude, radiusMeters, isMapLoaded) {
        if (isMapLoaded) {
            webViewRef?.evaluateJavascript(
                "if (window.recenter) { window.recenter(${currentLocation.latitude}, ${currentLocation.longitude}, $radiusMeters); }",
                null
            )
        }
    }

    val hasRealFix = currentLocation.isGpsActive
    val placeName = ClockInfo.placeName(currentLocation.locationName, hasRealFix)
    val ringLabels = MAP_RING_KM.map { stringResource(R.string.map_ring_label, it) }
    val palette = my.hana.meena.theme.LocalMeenaPalette.current
    val textHex = String.format("#%06X", 0xFFFFFF and palette.text.toArgb())
    val mutedHex = String.format("#%06X", 0xFFFFFF and palette.textMuted.toArgb())
    val sunHex = String.format("#%06X", 0xFFFFFF and palette.warning.toArgb())
    val compassWords = CompassWords(
        north = stringResource(R.string.map_compass_n),
        east = stringResource(R.string.map_compass_e),
        south = stringResource(R.string.map_compass_s),
        west = stringResource(R.string.map_compass_w),
        qibla = stringResource(R.string.map_qibla)
    )

    // Built once per look: later fixes move the dot through recenter() instead of reloading the page.
    val mapHtml = remember(accentHex, styleUrl, pageColor, ringLabels, textHex, mutedHex, sunHex, compassWords) {
        generateDarkOpenMapsHtml(
            lat = currentLocation.latitude,
            lng = currentLocation.longitude,
            accuracyMeters = currentLocation.accuracyMeters,
            accent = accentHex,
            styleUrl = styleUrl,
            pageColor = pageColor,
            ringLabels = ringLabels,
            textColor = textHex,
            mutedColor = mutedHex,
            sunColor = sunHex,
            words = compassWords
        )
    }

    // Rings follow the switch; the pulse also stops with reduced motion.
    LaunchedEffect(ringsOn, animate, isMapLoaded, mapHtml) {
        if (isMapLoaded) webViewRef?.evaluateJavascript("window.setRings && window.setRings($ringsOn, $animate);", null)
    }

    // Compass: only while the rings show and Meena is on screen, so the sensor never runs in the background.
    var heading by remember { mutableDoubleStateOf(0.0) }
    var compassUnsteady by remember { mutableStateOf(false) }
    val compass = remember {
        my.hana.meena.data.system.CompassHeading(
            context.applicationContext,
            onHeading = { heading = it },
            onUnsteady = { compassUnsteady = it }
        )
    }
    val qiblaBearing = remember(currentLocation.latitude, currentLocation.longitude) {
        my.hana.meena.data.system.Qibla.bearing(currentLocation.latitude, currentLocation.longitude)
    }
    LaunchedEffect(currentLocation.latitude, currentLocation.longitude) {
        compass.setPlace(currentLocation.latitude, currentLocation.longitude, System.currentTimeMillis())
    }
    androidx.lifecycle.compose.LifecycleResumeEffect(ringsOn, hasRealFix) {
        if (ringsOn && hasRealFix) compass.start()
        onPauseOrDispose { compass.stop() }
    }
    LaunchedEffect(heading, qiblaBearing, isMapLoaded, mapHtml) {
        if (isMapLoaded) {
            webViewRef?.evaluateJavascript(
                "window.setCompass && window.setCompass(${"%.1f".format(java.util.Locale.US, heading)}, ${"%.2f".format(java.util.Locale.US, qiblaBearing)});",
                null
            )
        }
    }

    // The sun, worked out on the phone: today's rise and set directions, and where it is now.
    var sunClock by remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(ringsOn) {
        while (ringsOn) {
            sunClock = System.currentTimeMillis()
            kotlinx.coroutines.delay(60_000L)
        }
    }
    val dayStart = remember(sunClock / 3_600_000L) {
        java.util.Calendar.getInstance().apply {
            timeInMillis = sunClock
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    // Rounded so small fixes don't redo the whole day.
    val sunLat = (currentLocation.latitude * 100).roundToInt() / 100.0
    val sunLng = (currentLocation.longitude * 100).roundToInt() / 100.0
    val sunEvents = remember(dayStart, sunLat, sunLng) { my.hana.meena.data.system.SunPath.events(dayStart, sunLat, sunLng) }
    val sunNow = remember(sunClock, sunLat, sunLng) { my.hana.meena.data.system.SunPath.position(sunClock, sunLat, sunLng) }
    val sunUp = sunNow.elevation > my.hana.meena.data.system.SunPath.HORIZON
    val timeFormat = remember(is24Hour) { java.text.SimpleDateFormat(if (is24Hour) "HH:mm" else "h:mm a", java.util.Locale.UK) }
    val riseText = sunEvents.riseMillis?.let { timeFormat.format(java.util.Date(it)).lowercase(java.util.Locale.UK) }
    val setText = sunEvents.setMillis?.let { timeFormat.format(java.util.Date(it)).lowercase(java.util.Locale.UK) }
    LaunchedEffect(sunEvents, sunNow, sunUp, riseText, setText, isMapLoaded, mapHtml) {
        if (isMapLoaded) {
            fun num(v: Double?) = v?.let { "%.1f".format(java.util.Locale.US, it) } ?: "null"
            fun str(v: String?) = v?.let { jsString(it) } ?: "null"
            webViewRef?.evaluateJavascript(
                "window.setSun && window.setSun(${num(sunEvents.riseAzimuth)}, ${num(sunEvents.setAzimuth)}, " +
                    "${num(if (sunUp) sunNow.azimuth else null)}, ${str(riseText)}, ${str(setText)});",
                null
            )
        }
    }

    CollapsibleWidget(
        title = stringResource(R.string.map_title),
        collapsedSummary = {
            Text(
                text = placeName ?: stringResource(R.string.map_no_location_short),
                style = MaterialTheme.typography.labelSmall,
                color = MeenaTextMuted
            )
        },
        modifier = modifier
    ) {
        // Without a real fix the map would centre on the built-in default; show that honestly instead.
        if (!hasRealFix) {
            Text(
                text = stringResource(R.string.map_no_location),
                style = MaterialTheme.typography.bodySmall,
                color = MeenaTextMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .metroClickable { DeviceLocationHelper.refreshLocation(context) }
            )
            return@CollapsibleWidget
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
        ) {
            Column {
                if (placeName != null) {
                    Text(
                        text = placeName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MeenaTextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = if (currentLocation.accuracyMeters < 1000f) {
                        stringResource(R.string.map_accuracy_m, currentLocation.accuracyMeters.coerceAtLeast(1f).toInt())
                    } else {
                        stringResource(
                            R.string.map_accuracy,
                            (currentLocation.accuracyMeters / 1000f).let { "%.1f".format(java.util.Locale.UK, it) }
                        )
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .background(MeenaBlack)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                allowContentAccess = true
                                allowFileAccess = true
                                cacheMode = WebSettings.LOAD_DEFAULT
                                loadWithOverviewMode = true
                                useWideViewPort = true
                            }
                            setBackgroundColor(AndroidColor.parseColor(pageColor))

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isMapLoaded = true
                                }
                            }

                            // Two fingers or a mostly sideways drag belong to the map; vertical drags scroll Today.
                            var downX = 0f
                            var downY = 0f
                            @android.annotation.SuppressLint("ClickableViewAccessibility")
                            setOnTouchListener { v, event ->
                                when (event.actionMasked) {
                                    MotionEvent.ACTION_DOWN -> {
                                        downX = event.x
                                        downY = event.y
                                    }
                                    MotionEvent.ACTION_POINTER_DOWN -> v.parent?.requestDisallowInterceptTouchEvent(true)
                                    MotionEvent.ACTION_MOVE -> {
                                        val sideways = kotlin.math.abs(event.x - downX) > kotlin.math.abs(event.y - downY)
                                        if (event.pointerCount > 1 || sideways) v.parent?.requestDisallowInterceptTouchEvent(true)
                                    }
                                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                        v.parent?.requestDisallowInterceptTouchEvent(false)
                                    }
                                }
                                false
                            }

                            loadDataWithBaseURL("https://www.openstreetmap.org", mapHtml, "text/html", "UTF-8", null)
                            webViewRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    // A new look (theme or accent) needs the page loaded again.
                    update = { view ->
                        if (view.tag != mapHtml) {
                            if (view.tag != null) {
                                isMapLoaded = false
                                view.setBackgroundColor(AndroidColor.parseColor(pageColor))
                                view.loadDataWithBaseURL("https://www.openstreetmap.org", mapHtml, "text/html", "UTF-8", null)
                            }
                            view.tag = mapHtml
                        }
                    }
                )
            }

            // What the phone knows about this spot; all of it comes from the fix and the address lookup.
            my.hana.meena.ui.components.MetroToggleRow(
                title = stringResource(R.string.map_rings_title),
                description = stringResource(if (animate) R.string.map_rings_hint else R.string.map_rings_hint_still),
                checked = ringsOn,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onRingsChange(it)
                }
            )
            if (ringsOn) {
                Text(
                    text = when {
                        !compass.isAvailable -> stringResource(R.string.map_compass_missing, qiblaBearing.roundToInt())
                        compassUnsteady -> stringResource(R.string.map_compass_unsteady)
                        else -> stringResource(R.string.map_qibla_bearing, qiblaBearing.roundToInt())
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted
                )
                if (riseText != null || setText != null) {
                    Text(
                        text = when {
                            riseText != null && setText != null -> stringResource(R.string.map_sun_times, riseText, setText)
                            riseText != null -> stringResource(R.string.map_sun_rise_only, riseText)
                            else -> stringResource(R.string.map_sun_set_only, setText!!)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MeenaTextMuted
                    )
                }
            }
            val minutesAgo = ((System.currentTimeMillis() - currentLocation.fixTimeMillis) / 60_000L).coerceAtLeast(0L)
            val foundText = when {
                currentLocation.fixTimeMillis <= 0L -> null
                minutesAgo < 1L -> stringResource(R.string.map_found_just_now)
                minutesAgo < 60L -> stringResource(R.string.map_found_minutes, minutesAgo.toInt())
                else -> stringResource(R.string.map_found_hours, (minutesAgo / 60L).toInt())
            }
            Column {
                currentLocation.region?.let { MapDetailRow(stringResource(R.string.map_detail_region), it) }
                MapDetailRow(stringResource(R.string.map_detail_coordinates), currentLocation.formattedCoordinates)
                foundText?.let { MapDetailRow(stringResource(R.string.map_detail_found), it) }
            }
            // Without precise location Android blurs the spot on purpose; say so and point to the switch.
            if (currentLocation.accuracyMeters >= 1000f) {
                Text(
                    text = stringResource(
                        if (DeviceLocationHelper.hasPreciseLocation(context)) R.string.map_rough_note_signal else R.string.map_rough_note
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .metroClickable {
                            if (DeviceLocationHelper.hasPreciseLocation(context)) DeviceLocationHelper.refreshLocation(context)
                            else DeviceLocationHelper.openAppSettings(context)
                        }
                        .padding(vertical = my.hana.meena.theme.MeenaSpacing.m)
                )
            }

            // Required credit for the map data (ODbL) and tiles.
            Text(
                text = stringResource(R.string.map_attribution),
                style = MaterialTheme.typography.labelSmall,
                color = MeenaTextMuted,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .metroClickable {
                        try {
                            context.startActivity(
                                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.openstreetmap.org/copyright"))
                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {}
                    }
                    .padding(vertical = my.hana.meena.theme.MeenaSpacing.m)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.l)
            ) {
                MapTextAction(stringResource(R.string.map_zoom_in)) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    webViewRef?.evaluateJavascript("window.zoomIn && window.zoomIn();", null)
                }
                MapTextAction(stringResource(R.string.map_zoom_out)) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    webViewRef?.evaluateJavascript("window.zoomOut && window.zoomOut();", null)
                }
                MapTextAction(stringResource(R.string.map_recenter)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    DeviceLocationHelper.refreshLocation(context)
                    webViewRef?.evaluateJavascript(
                        "window.recenter && window.recenter(${currentLocation.latitude}, ${currentLocation.longitude}, $radiusMeters);",
                        null
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                MapTextAction(stringResource(R.string.map_open)) {
                    DeviceLocationHelper.openInExternalMaps(
                        context = context,
                        lat = currentLocation.latitude,
                        lng = currentLocation.longitude,
                        label = currentLocation.locationName
                    )
                }
            }
        }
    }
}

@Composable
private fun MapDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MeenaTextMuted)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MeenaTextWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = my.hana.meena.theme.MeenaSpacing.m)
        )
    }
}

@Composable
private fun MapTextAction(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .metroClickable(onClick = onClick)
            .padding(vertical = my.hana.meena.theme.MeenaSpacing.m)
    )
}

/** Distance rings drawn around the dot, in km. The last one is the compass ring. */
private val MAP_RING_KM = listOf(3, 5)

/** Words drawn on the compass, from strings.xml. */
private data class CompassWords(val north: String, val east: String, val south: String, val west: String, val qibla: String)

private fun jsString(text: String): String = "'" + text.replace("\\", "").replace("'", "") + "'"

/**
 * HTML for the map: an OpenFreeMap vector style through MapLibre inside Leaflet, a plain dot
 * where the phone thinks it is and a thin ring as wide as that guess; the view is fitted to the
 * ring. Optional 3 and 5 km rings can pulse outward. Nothing on it pretends to be weather data.
 */
private fun generateDarkOpenMapsHtml(
    lat: Double,
    lng: Double,
    accuracyMeters: Float,
    accent: String,
    styleUrl: String,
    pageColor: String,
    ringLabels: List<String>,
    textColor: String,
    mutedColor: String,
    sunColor: String,
    words: CompassWords
): String {
    val ringKm = MAP_RING_KM.joinToString(",")
    val labels = ringLabels.joinToString(",") { jsString(it) }
    val cardinals = listOf(words.north, words.east, words.south, words.west).joinToString(",") { jsString(it) }
    val qiblaWord = jsString(words.qibla)
    val radius = accuracyMeters.coerceIn(100f, 5000f)
    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<!-- OpenFreeMap vector tiles need MapLibre; the connector lets Leaflet host them (approved 13 Nov 2026). -->
<link rel="stylesheet" href="https://unpkg.com/maplibre-gl@5.24.1/dist/maplibre-gl.css" />
<script src="https://unpkg.com/maplibre-gl@5.24.1/dist/maplibre-gl.js"></script>
<script src="https://unpkg.com/@maplibre/maplibre-gl-leaflet@0.1.4/leaflet-maplibre-gl.js"></script>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  html, body { width: 100%; height: 100%; background: $pageColor; overflow: hidden; }
  /* Height is set from window.innerHeight in script: CSS vh and % resolve to 0 in this WebView. */
  #map { position: fixed; top: 0; left: 0; width: 100%; height: 0; background: $pageColor; }
  .leaflet-container { background: $pageColor; }
  /* Compass and ring labels sit over the map and never take touches. */
  #overlay { position: fixed; top: 0; left: 0; width: 100%; height: 0; pointer-events: none; z-index: 1000; overflow: visible; }
  #overlay text { font: 12px sans-serif; }
</style>
</head>
<body>
<div id="map"></div>
<svg id="overlay" xmlns="http://www.w3.org/2000/svg"></svg>
<script>
  var lat = $lat;
  var lng = $lng;
  var accent = '$accent';

  var map = L.map('map', {
    zoomControl: false,
    attributionControl: false,
    minZoom: 9,
    maxZoom: 17,
    // Quarter steps so the 5 km ring can fill the frame instead of snapping to half its size.
    zoomSnap: 0.25,
    preferCanvas: true
  }).setView([lat, lng], 13);
  // The WebView is laid out at 0 height inside Today's list and CSS heights stay 0, so size the
  // map from innerHeight in pixels and tell Leaflet whenever that changes.
  var fitted = false;
  function fitArea() {
    // With distance rings on, show the widest ring; else the ring the phone is unsure within,
    // never closer than street level.
    var outerKm = RING_KM[RING_KM.length - 1];
    var target = ringsOn ? L.latLng(lat, lng).toBounds(outerKm * 2000) : area.getBounds();
    map.fitBounds(target, { padding: [16, 16], maxZoom: 15, animate: false });
  }
  function fitToWindow() {
    var el = document.getElementById('map');
    var h = window.innerHeight;
    if (h > 0 && el.clientHeight !== h) {
      el.style.height = h + 'px';
      document.getElementById('overlay').style.height = h + 'px';
      map.invalidateSize();
      // Refit whenever the height changes: the WebView grows after first layout, and fitting
      // only once left the rings sitting low.
      fitArea();
      fitted = true;
      drawCompass();
    }
  }
  window.addEventListener('resize', fitToWindow);
  var fitTries = 0;
  var fitTimer = setInterval(function () {
    fitToWindow();
    if (++fitTries > 40) clearInterval(fitTimer);
  }, 250);

  // OpenFreeMap vector style (free, no key; https://openfreemap.org). Credit is shown below the map.
  L.maplibreGL({ style: '$styleUrl' }).addTo(map);

  // How rough the location is: a thin ring, barely filled.
  var area = L.circle([lat, lng], {
    radius: $radius,
    color: accent,
    weight: 1,
    opacity: 0.8,
    fillColor: accent,
    fillOpacity: 0.06,
    interactive: false
  }).addTo(map);

  // Where the phone thinks it is.
  var here = L.circleMarker([lat, lng], {
    radius: 5,
    color: '#FFFFFF',
    weight: 1.5,
    fillColor: accent,
    fillOpacity: 1,
    interactive: false
  }).addTo(map);

  // Distance rings (km). The 3 km ring is a clear line on the map; the 5 km ring is a lighter
  // compass ring drawn over it. A soft wave grows out to each one (about 20 frames a second,
  // paused when the page is hidden).
  var RING_KM = [$ringKm];
  var RING_LABELS = [$labels];
  var CARDINALS = [$cardinals];
  var QIBLA_WORD = $qiblaWord;
  var TEXT = '$textColor', MUTED = '$mutedColor', SUN = '$sunColor';
  var PULSE_MS = 3000;
  var WAVE_OPACITY = [0.6, 0.3];
  var ringsOn = false, pulseOn = false, timer = null;
  var heading = 0, qibla = null;
  var sun = { rise: null, set: null, now: null, riseText: null, setText: null };
  var rings = [], waves = [];
  RING_KM.forEach(function (km, i) {
    rings.push(L.circle([lat, lng], { radius: km * 1000, color: accent, weight: 1.2, opacity: 0.85, fill: false, interactive: false }));
    waves.push(L.circle([lat, lng], { radius: 1, color: accent, weight: 1.5, opacity: 0, fillColor: accent, fillOpacity: 0, interactive: false }));
  });
  function tick() {
    if (document.hidden) return;
    var now = Date.now();
    RING_KM.forEach(function (km, i) {
      // The two waves start half a beat apart so they don't move in step; the outer one is fainter.
      var t = ((now / PULSE_MS) + i * 0.5) % 1;
      var top = WAVE_OPACITY[i] || 0.3;
      waves[i].setRadius(Math.max(1, t * km * 1000));
      waves[i].setStyle({ opacity: top * (1 - t), fillOpacity: top * 0.12 * (1 - t) });
    });
  }
  function stopPulse() {
    if (timer) { clearInterval(timer); timer = null; }
    waves.forEach(function (w) { map.removeLayer(w); });
  }

  // Compass overlay (SVG): the 5 km ring with ticks and n e s w turned so n points to real north,
  // a qibla arrow from the dot out to the 3 km ring, and a qibla mark that moves round the 5 km ring.
  // Words stay upright. heading = which way the top of the phone points, from true north.
  var NS = 'http://www.w3.org/2000/svg';
  var overlay = document.getElementById('overlay');
  function el(name, attrs, text) {
    var e = document.createElementNS(NS, name);
    for (var k in attrs) e.setAttribute(k, attrs[k]);
    if (text) e.textContent = text;
    overlay.appendChild(e);
    return e;
  }
  function px(km) {
    var c = map.latLngToContainerPoint([lat, lng]);
    return c.distanceTo(map.latLngToContainerPoint([lat + km / 111.32, lng]));
  }
  function at(c, r, bearing) {
    var a = (bearing - heading) * Math.PI / 180;
    return { x: c.x + r * Math.sin(a), y: c.y - r * Math.cos(a) };
  }
  function drawCompass() {
    while (overlay.firstChild) overlay.removeChild(overlay.firstChild);
    if (!ringsOn || !document.getElementById('map').clientHeight) return;
    var c = map.latLngToContainerPoint([lat, lng]);
    var r3 = px(RING_KM[0]), r5 = px(RING_KM[RING_KM.length - 1]);

    el('circle', { cx: c.x, cy: c.y, r: r5, fill: 'none', stroke: accent, 'stroke-width': 1, 'stroke-opacity': 0.35 });
    var d = '';
    for (var b = 0; b < 360; b += 10) {
      var len = (b % 90 === 0) ? 8 : (b % 30 === 0 ? 5 : 3);
      var p1 = at(c, r5, b), p2 = at(c, r5 - len, b);
      d += 'M' + p1.x.toFixed(1) + ',' + p1.y.toFixed(1) + 'L' + p2.x.toFixed(1) + ',' + p2.y.toFixed(1);
    }
    el('path', { d: d, stroke: MUTED, 'stroke-width': 1, 'stroke-opacity': 0.8, fill: 'none' });
    CARDINALS.forEach(function (word, i) {
      var p = at(c, r5 - 16, i * 90);
      el('text', { x: p.x, y: p.y, 'text-anchor': 'middle', 'dominant-baseline': 'central',
        fill: i === 0 ? accent : TEXT, 'fill-opacity': i === 0 ? 1 : 0.7 }, word);
    });

    // North pointer: a small solid triangle on the 5 km ring, tip pointing out, with n just inside.
    var nTip = at(c, r5 + 9, 0), nLeft = at(c, r5 - 1, -4), nRight = at(c, r5 - 1, 4);
    el('path', { d: 'M' + nTip.x + ',' + nTip.y + 'L' + nLeft.x + ',' + nLeft.y + 'L' + nRight.x + ',' + nRight.y + 'Z', fill: accent });

    // Ring sizes, where the map has room; they don't turn. 3 km sits just inside its ring on the
    // right, 5 km just outside its own at the top right, so neither meets the sunrise mark.
    el('text', { x: c.x + r3 - 4, y: c.y - 4, 'text-anchor': 'end', fill: accent }, RING_LABELS[0] || '');
    el('text', { x: c.x + r5 * 0.72 + 4, y: c.y - r5 * 0.72 - 2, fill: accent, 'fill-opacity': 0.6 }, RING_LABELS[RING_LABELS.length - 1] || '');

    // Sun: where it is now, as a dotted line from the dot to the 5 km ring (only while it's up),
    // and today's sunrise (filled) and sunset (open) on the ring, with their times outside.
    if (sun.now !== null) {
      var s1 = at(c, 9, sun.now), s2 = at(c, r5 - 5, sun.now);
      el('path', { d: 'M' + s1.x + ',' + s1.y + 'L' + s2.x + ',' + s2.y, stroke: SUN, 'stroke-width': 2,
        'stroke-dasharray': '0.1 5', 'stroke-linecap': 'round', fill: 'none' });
      el('circle', { cx: s2.x, cy: s2.y, r: 3, fill: SUN });
    }
    [['rise', sun.rise, sun.riseText], ['set', sun.set, sun.setText]].forEach(function (e) {
      if (e[1] === null) return;
      var p = at(c, r5, e[1]);
      el('circle', e[0] === 'rise'
        ? { cx: p.x, cy: p.y, r: 4, fill: SUN }
        : { cx: p.x, cy: p.y, r: 3.5, fill: 'none', stroke: SUN, 'stroke-width': 1.5 });
      if (e[2]) {
        var t = at(c, r5 + 16, e[1]);
        el('text', { x: t.x, y: t.y, 'text-anchor': 'middle', 'dominant-baseline': 'central', fill: SUN }, e[2]);
      }
    });

    if (qibla !== null) {
      // The qibla sits on the 3 km ring (a small square, the Kaaba), and the arrow runs from just
      // past the dot to it.
      var m = at(c, r3, qibla);
      var from = at(c, 9, qibla), tip = at(c, r3 - 6, qibla);
      var left = at(tip, 7, qibla + 155), right = at(tip, 7, qibla - 155);
      el('path', {
        d: 'M' + from.x + ',' + from.y + 'L' + tip.x + ',' + tip.y + 'M' + left.x + ',' + left.y + 'L' + tip.x + ',' + tip.y + 'L' + right.x + ',' + right.y,
        stroke: accent, 'stroke-width': 1.5, fill: 'none', 'stroke-linecap': 'round', 'stroke-linejoin': 'round'
      });
      el('rect', { x: m.x - 3.5, y: m.y - 3.5, width: 7, height: 7, fill: accent,
        transform: 'rotate(' + (qibla - heading) + ' ' + m.x + ' ' + m.y + ')' });
      var w = at(c, r3 + 13, qibla);
      el('text', { x: w.x, y: w.y, 'text-anchor': 'middle', 'dominant-baseline': 'central', fill: accent }, QIBLA_WORD);
    }
  }
  map.on('move zoom zoomend resize', drawCompass);

  window.setSun = function (rise, set, now, riseText, setText) {
    sun = { rise: rise, set: set, now: now, riseText: riseText, setText: setText };
    drawCompass();
  };

  window.setCompass = function (newHeading, newQibla) {
    heading = newHeading || 0;
    qibla = (newQibla === null || newQibla === undefined) ? null : newQibla;
    drawCompass();
  };

  window.setRings = function (on, animate) {
    var changed = on !== ringsOn;
    ringsOn = on;
    pulseOn = on && animate;
    stopPulse();
    // Only the 3 km ring is a map layer; the 5 km ring is the compass.
    if (on) rings[0].addTo(map); else map.removeLayer(rings[0]);
    if (pulseOn) {
      waves.forEach(function (w) { w.addTo(map); });
      tick();
      timer = setInterval(tick, 50);
    }
    if (changed && document.getElementById('map').clientHeight > 0) fitArea();
    drawCompass();
  };
  document.addEventListener('visibilitychange', function () {
    if (!document.hidden && pulseOn) tick();
  });

  window.recenter = function (newLat, newLng, newRadius) {
    if (newLat && newLng) {
      lat = newLat;
      lng = newLng;
      here.setLatLng([lat, lng]);
      area.setLatLng([lat, lng]);
      rings.concat(waves).forEach(function (c) { c.setLatLng([lat, lng]); });
    }
    if (newRadius) area.setRadius(newRadius);
    if (document.getElementById('map').clientHeight > 0) { fitArea(); } else { fitted = false; }
    drawCompass();
  };

  window.zoomIn = function() {
    map.zoomIn();
  };

  window.zoomOut = function() {
    map.zoomOut();
  };
</script>
</body>
</html>
""".trimIndent()
}

