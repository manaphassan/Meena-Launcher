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
 * Map of the user's rough area on a dark OpenStreetMap basemap (CARTO Dark Matter tiles).
 * Shown only after a real location fix; credits OpenStreetMap and CARTO as their terms require.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OpenMapsRadarWidget(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val currentLocation by DeviceLocationHelper.locationFlow.collectAsState()

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        DeviceLocationHelper.refreshLocation(context)
    }

    LaunchedEffect(currentLocation.latitude, currentLocation.longitude, isMapLoaded) {
        if (isMapLoaded) {
            webViewRef?.evaluateJavascript(
                "if (window.recenter) { window.recenter(${currentLocation.latitude}, ${currentLocation.longitude}); }",
                null
            )
        }
    }

    val hasRealFix = currentLocation.isGpsActive
    val placeName = ClockInfo.placeName(currentLocation.locationName, hasRealFix)

    val mapHtml = remember(currentLocation.latitude, currentLocation.longitude) {
        generateDarkOpenMapsHtml(
            lat = currentLocation.latitude,
            lng = currentLocation.longitude,
            locationName = currentLocation.locationName
        )
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
                    text = stringResource(
                        R.string.map_accuracy,
                        currentLocation.formattedCoordinates,
                        ((currentLocation.accuracyMeters / 1000f).coerceAtLeast(0.1f)).let { "%.1f".format(it) }
                    ),
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
                            setBackgroundColor(AndroidColor.parseColor("#080808"))

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
                    modifier = Modifier.fillMaxSize()
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
                        "window.recenter && window.recenter(${currentLocation.latitude}, ${currentLocation.longitude});",
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

/**
 * Builds the HTML & Leaflet payload for rendering Dark OpenStreetMap
 * with 3km radius radar wave, animated radar pulse, concentric ranges,
 * and POI marker with True North bearing arrow.
 */
private fun generateDarkOpenMapsHtml(
    lat: Double,
    lng: Double,
    locationName: String
): String {
    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  html, body, #map {
    width: 100%;
    height: 100%;
    background: #080808;
    overflow: hidden;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  }
  .leaflet-container { background: #080808; }
  
  /* High contrast dark theme tiles using CartoDB Dark Matter / OSM fallback */
  .leaflet-tile {
    filter: brightness(0.7) invert(1) contrast(3) hue-rotate(200deg) saturate(0.2) brightness(0.7);
  }

  /* Animated Expanding Radar Pulse Waves (3km Geodesic) */
  @keyframes radarWavePulse {
    0% {
      transform: scale(0.05);
      opacity: 0.9;
    }
    60% {
      opacity: 0.4;
    }
    100% {
      transform: scale(1.0);
      opacity: 0.0;
    }
  }

  .radar-pulse-container {
    position: relative;
    width: 280px;
    height: 280px;
    pointer-events: none;
  }

  .radar-pulse-ring-1 {
    position: absolute;
    top: 0; left: 0; right: 0; bottom: 0;
    border-radius: 50%;
    border: 2px solid #00A4EF;
    background: radial-gradient(circle, rgba(0, 164, 239, 0.25) 0%, rgba(0, 164, 239, 0.05) 60%, transparent 100%);
    animation: radarWavePulse 3.5s cubic-bezier(0.25, 0.46, 0.45, 0.94) infinite;
    pointer-events: none;
  }

  .radar-pulse-ring-2 {
    position: absolute;
    top: 0; left: 0; right: 0; bottom: 0;
    border-radius: 50%;
    border: 1.5px solid rgba(0, 164, 239, 0.7);
    animation: radarWavePulse 3.5s cubic-bezier(0.25, 0.46, 0.45, 0.94) infinite;
    animation-delay: 1.75s;
    pointer-events: none;
  }

  /* Rotating Radar Sweep Line */
  @keyframes radarSweepRotate {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
  }

  .radar-sweep-beam {
    position: absolute;
    top: 0; left: 0; right: 0; bottom: 0;
    border-radius: 50%;
    background: conic-gradient(from 0deg, rgba(0, 164, 239, 0.4) 0deg, rgba(0, 164, 239, 0.08) 45deg, transparent 75deg);
    animation: radarSweepRotate 4s linear infinite;
    pointer-events: none;
  }

  /* Current Location POI Marker with True North Bearing Arrow */
  .poi-anchor {
    position: relative;
    width: 36px;
    height: 36px;
  }

  .poi-core-dot {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 12px;
    height: 12px;
    background: #00A4EF;
    border: 2px solid #FFFFFF;
    box-shadow: 0 0 12px #00A4EF, 0 0 24px rgba(0, 164, 239, 0.6);
    border-radius: 50%;
  }

  /* Arrow pointing North bearing directly above the POI dot */
  .north-bearing-arrow {
    position: absolute;
    top: -18px;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    flex-direction: column;
    align-items: center;
    pointer-events: none;
  }

  .north-arrow-triangle {
    width: 0;
    height: 0;
    border-left: 6px solid transparent;
    border-right: 6px solid transparent;
    border-bottom: 11px solid #00A4EF;
    filter: drop-shadow(0 0 4px #00A4EF);
  }

  .north-arrow-letter {
    font-size: 10px;
    font-weight: 900;
    color: #FFFFFF;
    text-shadow: 0 0 4px #000;
    margin-top: 1px;
    letter-spacing: 0.5px;
  }

  .range-text-label {
    background: rgba(0, 0, 0, 0.85);
    color: #00A4EF;
    border: 1px solid rgba(0, 164, 239, 0.8);
    font-size: 8px;
    font-weight: bold;
    padding: 1px 4px;
    white-space: nowrap;
    text-align: center;
  }
</style>
</head>
<body>
<div id="map"></div>
<script>
  var lat = $lat;
  var lng = $lng;

  // Initialize Leaflet OpenMap
  var map = L.map('map', {
    zoomControl: false,
    attributionControl: false,
    minZoom: 10,
    maxZoom: 18,
    preferCanvas: true
  }).setView([lat, lng], 13);

  // CartoDB Dark Matter tiles
  L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/dark_all/{z}/{x}/{y}.png', {
    subdomains: 'abcd',
    maxZoom: 19
  }).addTo(map);

  // 1.0 km & 2.0 km concentric radar guide rings
  L.circle([lat, lng], {
    radius: 1000,
    color: 'rgba(0, 164, 239, 0.3)',
    weight: 1,
    dashArray: '2, 4',
    fill: false
  }).addTo(map);

  L.circle([lat, lng], {
    radius: 2000,
    color: 'rgba(0, 164, 239, 0.4)',
    weight: 1,
    dashArray: '3, 5',
    fill: false
  }).addTo(map);

  // 3.0 km outer radar boundary circle
  var radar3kmCircle = L.circle([lat, lng], {
    radius: 3000,
    color: '#00A4EF',
    weight: 1.5,
    dashArray: '5, 5',
    fillColor: '#00A4EF',
    fillOpacity: 0.07
  }).addTo(map);

  // 3.0 KM Range Tag on North Perimeter
  var northPerimeterLat = lat + (3000 / 111320.0);
  var rangeTagIcon = L.divIcon({
    className: 'range-text-label',
    html: '3 km around you',
    iconSize: [120, 16],
    iconAnchor: [60, 8]
  });
  var rangeTagMarker = L.marker([northPerimeterLat, lng], { icon: rangeTagIcon }).addTo(map);

  // Animated Radar Pulse & Rotating Sweep Wave Overlay
  var radarOverlayIcon = L.divIcon({
    className: '',
    html: '<div class="radar-pulse-container"><div class="radar-pulse-ring-1"></div><div class="radar-pulse-ring-2"></div><div class="radar-sweep-beam"></div></div>',
    iconSize: [280, 280],
    iconAnchor: [140, 140]
  });
  var radarWaveMarker = L.marker([lat, lng], { icon: radarOverlayIcon, interactive: false }).addTo(map);

  // Current Location POI Marker with North Bearing Arrow
  var poiIcon = L.divIcon({
    className: 'poi-anchor',
    html: '<div class="north-bearing-arrow"><div class="north-arrow-triangle"></div><div class="north-arrow-letter">N</div></div><div class="poi-core-dot"></div>',
    iconSize: [36, 36],
    iconAnchor: [18, 18]
  });
  var poiMarker = L.marker([lat, lng], { icon: poiIcon }).addTo(map);

  // Exposed JS hooks for Android interaction
  window.recenter = function(newLat, newLng) {
    if (newLat && newLng) {
      lat = newLat;
      lng = newLng;
      var newNorthLat = lat + (3000 / 111320.0);
      poiMarker.setLatLng([lat, lng]);
      radar3kmCircle.setLatLng([lat, lng]);
      radarWaveMarker.setLatLng([lat, lng]);
      rangeTagMarker.setLatLng([newNorthLat, lng]);
    }
    map.setView([lat, lng], 13, { animate: true });
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

