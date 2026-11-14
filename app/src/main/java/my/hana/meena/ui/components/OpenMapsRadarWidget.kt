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

    // Built once per look: later fixes move the dot through recenter() instead of reloading the page.
    val mapHtml = remember(accentHex, styleUrl, pageColor, ringLabels) {
        generateDarkOpenMapsHtml(
            lat = currentLocation.latitude,
            lng = currentLocation.longitude,
            accuracyMeters = currentLocation.accuracyMeters,
            accent = accentHex,
            styleUrl = styleUrl,
            pageColor = pageColor,
            ringLabels = ringLabels
        )
    }

    // Rings follow the switch; the pulse also stops with reduced motion.
    LaunchedEffect(ringsOn, animate, isMapLoaded, mapHtml) {
        if (isMapLoaded) webViewRef?.evaluateJavascript("window.setRings && window.setRings($ringsOn, $animate);", null)
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

/** Distance rings drawn around the dot, in km. */
private val MAP_RING_KM = listOf(3, 5)

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
    ringLabels: List<String>
): String {
    val ringKm = MAP_RING_KM.joinToString(",")
    val labels = ringLabels.joinToString(",") { "'" + it.replace("'", "") + "'" }
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
  /* Ring distance labels: plain accent text, no box. */
  .ring-label { color: $accent; font: 12px sans-serif; white-space: nowrap; transform: translate(-50%, -100%); }
</style>
</head>
<body>
<div id="map"></div>
<script>
  var lat = $lat;
  var lng = $lng;
  var accent = '$accent';

  var map = L.map('map', {
    zoomControl: false,
    attributionControl: false,
    minZoom: 9,
    maxZoom: 17,
    preferCanvas: true
  }).setView([lat, lng], 13);
  // The WebView is laid out at 0 height inside Today's list and CSS heights stay 0, so size the
  // map from innerHeight in pixels and tell Leaflet whenever that changes.
  var fitted = false;
  function fitArea() {
    // With distance rings on, show the widest ring; else the ring the phone is unsure within,
    // never closer than street level.
    var target = (ringsOn && rings.length) ? rings[rings.length - 1].getBounds() : area.getBounds();
    map.fitBounds(target, { padding: [16, 16], maxZoom: 15, animate: false });
  }
  function fitToWindow() {
    var el = document.getElementById('map');
    var h = window.innerHeight;
    if (h > 0 && el.clientHeight !== h) {
      el.style.height = h + 'px';
      map.invalidateSize();
      if (!fitted) { fitArea(); fitted = true; }
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

  // Distance rings (km): a thin still outline each, a soft wave that grows out to it, and a label
  // at the top. The wave runs at about 20 frames a second and stops when the page is hidden.
  var RING_KM = [$ringKm];
  var RING_LABELS = [$labels];
  var PULSE_MS = 3000;
  var ringsOn = false, pulseOn = false, timer = null;
  var rings = [], waves = [], tags = [];
  function labelPoint(km) { return [lat + km / 111.32, lng]; }
  RING_KM.forEach(function (km, i) {
    rings.push(L.circle([lat, lng], { radius: km * 1000, color: accent, weight: 1, opacity: 0.7, fill: false, interactive: false }));
    waves.push(L.circle([lat, lng], { radius: 1, color: accent, weight: 1.5, opacity: 0, fillColor: accent, fillOpacity: 0, interactive: false }));
    tags.push(L.marker(labelPoint(km), {
      icon: L.divIcon({ className: 'ring-label', html: RING_LABELS[i] || (km + ' km'), iconSize: null }),
      interactive: false, keyboard: false
    }));
  });
  function tick() {
    if (document.hidden) return;
    var now = Date.now();
    RING_KM.forEach(function (km, i) {
      // The two waves start half a beat apart so they don't move in step.
      var t = ((now / PULSE_MS) + i * 0.5) % 1;
      waves[i].setRadius(Math.max(1, t * km * 1000));
      waves[i].setStyle({ opacity: 0.6 * (1 - t), fillOpacity: 0.08 * (1 - t) });
    });
  }
  function stopPulse() {
    if (timer) { clearInterval(timer); timer = null; }
    waves.forEach(function (w) { map.removeLayer(w); });
  }
  window.setRings = function (on, animate) {
    var changed = on !== ringsOn;
    ringsOn = on;
    pulseOn = on && animate;
    stopPulse();
    rings.concat(tags).forEach(function (layer) { if (on) layer.addTo(map); else map.removeLayer(layer); });
    if (pulseOn) {
      waves.forEach(function (w) { w.addTo(map); });
      tick();
      timer = setInterval(tick, 50);
    }
    if (changed && document.getElementById('map').clientHeight > 0) fitArea();
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
      tags.forEach(function (m, i) { m.setLatLng(labelPoint(RING_KM[i])); });
    }
    if (newRadius) area.setRadius(newRadius);
    if (document.getElementById('map').clientHeight > 0) { fitArea(); } else { fitted = false; }
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

