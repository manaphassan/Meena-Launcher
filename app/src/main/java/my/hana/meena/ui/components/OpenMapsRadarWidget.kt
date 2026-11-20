package my.hana.meena.ui.components

import my.hana.meena.R
import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.MotionEvent
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import my.hana.meena.data.system.ClockInfo
import my.hana.meena.data.system.DeviceLocation
import my.hana.meena.data.system.DeviceLocationHelper
import my.hana.meena.data.system.PrayerSlotInfo
import my.hana.meena.data.system.Qibla
import my.hana.meena.data.system.SunEvents
import my.hana.meena.data.system.SunPath
import my.hana.meena.data.system.SunPosition
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextWhite
import kotlin.math.roundToInt

/**
 * The nearby map on Today: the place, a still preview of the map with its rings and compass,
 * and a one-line credit. No buttons or long text here; tapping the map opens the full page.
 * The compass sensor doesn't run on Today, so the preview stays north-up.
 */
@Composable
fun OpenMapsRadarWidget(
    modifier: Modifier = Modifier,
    ringsOn: Boolean = true,
    animate: Boolean = true,
    is24Hour: Boolean = true,
    onOpen: () -> Unit = {}
) {
    val context = LocalContext.current
    val location by DeviceLocationHelper.locationFlow.collectAsState()
    LaunchedEffect(Unit) { DeviceLocationHelper.refreshLocation(context) }

    val hasRealFix = location.isGpsActive
    val placeName = ClockInfo.placeName(location.locationName, hasRealFix)

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

        val sky = rememberSkyInfo(location, is24Hour, emptyList(), active = ringsOn)
        Column(verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)) {
            if (placeName != null) {
                Text(
                    text = placeName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MeenaTextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val openLabel = stringResource(R.string.map_open_larger)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                NearbyMap(
                    location = location,
                    sky = sky,
                    heading = null,
                    ringsOn = ringsOn,
                    animate = animate,
                    interactive = false,
                    detailed = false,
                    description = mapDescription(placeName, sky),
                    modifier = Modifier.fillMaxSize()
                )
                // Catches taps for the whole preview; drags still scroll Today.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .metroClickable(onClickLabel = openLabel, tiltDegrees = 2f, onClick = onOpen)
                )
            }
            // Required credit for the map data (ODbL) and tiles; the full wording is on the map page.
            Text(
                text = stringResource(R.string.map_attribution_short),
                style = MaterialTheme.typography.labelSmall,
                color = MeenaTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** One sun-up prayer time placed on the sun's path. */
data class PrayerMark(val name: String, val position: SunPosition, val timeText: String)

/** Everything the map draws about the sky and qibla for the current place and minute. */
data class SkyInfo(
    val qibla: Double,
    val events: SunEvents,
    val now: SunPosition,
    val riseText: String?,
    val setText: String?,
    val path: List<SunPosition>,
    val prayers: List<PrayerMark>
) {
    val sunUp: Boolean get() = now.elevation > SunPath.HORIZON
}

/**
 * Qibla, today's sunrise and sunset, the sun's path and where it is now, refreshed every
 * minute while [active]. [slots] are today's JAKIM times; those while the sun is up are placed
 * on its path. All worked out on the phone.
 */
@Composable
fun rememberSkyInfo(location: DeviceLocation, is24Hour: Boolean, slots: List<PrayerSlotInfo>, active: Boolean = true): SkyInfo {
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(active) {
        while (active) {
            clock = System.currentTimeMillis()
            kotlinx.coroutines.delay(60_000L)
        }
    }
    val dayStart = remember(clock / 3_600_000L) { startOfDay(clock) }
    // Rounded so small fixes don't redo the whole day.
    val lat = (location.latitude * 100).roundToInt() / 100.0
    val lng = (location.longitude * 100).roundToInt() / 100.0
    val qibla = remember(location.latitude, location.longitude) { Qibla.bearing(location.latitude, location.longitude) }
    val events = remember(dayStart, lat, lng) { SunPath.events(dayStart, lat, lng) }
    val now = remember(clock, lat, lng) { SunPath.position(clock, lat, lng) }
    val path = remember(events, lat, lng) {
        if (events.riseMillis != null && events.setMillis != null) SunPath.path(events.riseMillis, events.setMillis, lat, lng) else emptyList()
    }
    val timeFormat = remember(is24Hour) { java.text.SimpleDateFormat(if (is24Hour) "HH:mm" else "h:mm a", java.util.Locale.UK) }
    fun format(millis: Long) = timeFormat.format(java.util.Date(millis)).lowercase(java.util.Locale.UK)
    val prayers = remember(slots, dayStart, lat, lng, is24Hour) {
        slots.filter { !it.isSolar }.mapNotNull { slot ->
            val at = dayStart + (slot.hour24 * 60L + slot.minute) * 60_000L
            val position = SunPath.position(at, lat, lng)
            // Maghrib is the sunset mark already; subuh and isyak happen in the dark.
            if (position.elevation < 5.0) null else PrayerMark(slot.name.lowercase(), position, format(at))
        }
    }
    return SkyInfo(
        qibla = qibla,
        events = events,
        now = now,
        riseText = events.riseMillis?.let(::format),
        setText = events.setMillis?.let(::format),
        path = path,
        prayers = prayers
    )
}

private fun startOfDay(millis: Long): Long = java.util.Calendar.getInstance().apply {
    timeInMillis = millis
    set(java.util.Calendar.HOUR_OF_DAY, 0)
    set(java.util.Calendar.MINUTE, 0)
    set(java.util.Calendar.SECOND, 0)
    set(java.util.Calendar.MILLISECOND, 0)
}.timeInMillis

/** Which way the phone points, or null while the compass is off or missing. */
data class CompassState(val heading: Double?, val unsteady: Boolean, val available: Boolean)

/**
 * Reads the compass only while [active] and Meena is on screen, so the sensor never runs in
 * the background.
 */
@Composable
fun rememberCompass(active: Boolean, location: DeviceLocation): CompassState {
    val context = LocalContext.current
    var heading by remember { mutableDoubleStateOf(Double.NaN) }
    var unsteady by remember { mutableStateOf(false) }
    val compass = remember {
        my.hana.meena.data.system.CompassHeading(
            context.applicationContext,
            onHeading = { heading = it },
            onUnsteady = { unsteady = it }
        )
    }
    LaunchedEffect(location.latitude, location.longitude) {
        compass.setPlace(location.latitude, location.longitude, System.currentTimeMillis())
    }
    androidx.lifecycle.compose.LifecycleResumeEffect(active) {
        if (active) compass.start()
        onPauseOrDispose {
            compass.stop()
            heading = Double.NaN
        }
    }
    return CompassState(heading.takeUnless { it.isNaN() || !active }, unsteady, compass.isAvailable)
}

/** What a screen reader hears for the map: the place, the qibla and the sun. */
@Composable
fun mapDescription(placeName: String?, sky: SkyInfo): String {
    val base = androidx.compose.ui.res.pluralStringResource(
        R.plurals.map_a11y,
        sky.qibla.roundToInt(),
        placeName ?: stringResource(R.string.weather_near_you),
        sky.qibla.roundToInt()
    )
    val sun = if (sky.riseText != null && sky.setText != null) " " + stringResource(R.string.map_a11y_sun, sky.riseText, sky.setText) else ""
    return base + sun
}

/** Lets the map page zoom and recenter the map. */
class NearbyMapController {
    internal var view: WebView? = null
    fun zoomIn() { view?.evaluateJavascript("window.zoomIn && window.zoomIn();", null) }
    fun zoomOut() { view?.evaluateJavascript("window.zoomOut && window.zoomOut();", null) }
    fun recenter() { view?.evaluateJavascript("window.refit && window.refit();", null) }
}

/**
 * The map itself: OpenFreeMap through MapLibre inside Leaflet, with the location dot, the 3 and
 * 5 km rings, the compass on the 5 km ring and the qibla on the 3 km ring. [heading] turns the
 * compass (null keeps it north-up). [interactive] lets it be dragged and pinched; [detailed]
 * adds the sun's path, prayer marks and all the labels.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NearbyMap(
    location: DeviceLocation,
    sky: SkyInfo,
    heading: Double?,
    ringsOn: Boolean,
    animate: Boolean,
    interactive: Boolean,
    detailed: Boolean,
    description: String,
    modifier: Modifier = Modifier,
    controller: NearbyMapController? = null
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }

    val palette = my.hana.meena.theme.LocalMeenaPalette.current
    fun hex(c: androidx.compose.ui.graphics.Color) = String.format("#%06X", 0xFFFFFF and c.toArgb())
    val accentHex = hex(MaterialTheme.colorScheme.primary)
    // OpenFreeMap's own styles (https://openfreemap.org/quick_start/): dark on black, positron on white and paper.
    val styleUrl = if (palette.isLight) "https://tiles.openfreemap.org/styles/positron" else "https://tiles.openfreemap.org/styles/dark"
    val look = MapLook(
        accent = accentHex,
        styleUrl = styleUrl,
        textColor = hex(palette.text),
        mutedColor = hex(palette.textMuted),
        pageColor = hex(palette.background),
        sunColor = hex(palette.warning),
        ringLabels = MAP_RING_KM.map { stringResource(R.string.map_ring_label, it) },
        cardinals = listOf(
            stringResource(R.string.map_compass_n),
            stringResource(R.string.map_compass_e),
            stringResource(R.string.map_compass_s),
            stringResource(R.string.map_compass_w)
        ),
        qiblaWord = stringResource(R.string.map_qibla),
        interactive = interactive,
        detailed = detailed
    )
    val radiusMeters = location.accuracyMeters.coerceIn(100f, 5000f)

    // Built once per look: later fixes move the dot through recenter() instead of reloading the page.
    val context = LocalContext.current
    val mapHtml = remember(look) {
        generateMapHtml(location.latitude, location.longitude, location.accuracyMeters, look)
            .replace(MAP_LIBS_MARKER, MapAssets.inlineTags(context))
    }

    LaunchedEffect(location.latitude, location.longitude, radiusMeters, isMapLoaded) {
        if (isMapLoaded) {
            webViewRef?.evaluateJavascript(
                "window.recenter && window.recenter(${location.latitude}, ${location.longitude}, $radiusMeters);",
                null
            )
        }
    }
    // Rings follow the switch; the pulse also stops with reduced motion.
    LaunchedEffect(ringsOn, animate, isMapLoaded, mapHtml) {
        if (isMapLoaded) webViewRef?.evaluateJavascript("window.setRings && window.setRings($ringsOn, $animate);", null)
    }
    LaunchedEffect(heading, sky.qibla, isMapLoaded, mapHtml) {
        if (isMapLoaded) {
            webViewRef?.evaluateJavascript("window.setCompass && window.setCompass(${num(heading ?: 0.0)}, ${num(sky.qibla)});", null)
        }
    }
    LaunchedEffect(sky, isMapLoaded, mapHtml) {
        if (isMapLoaded) {
            val path = sky.path.joinToString(",", "[", "]") { "[${num(it.azimuth)},${num(it.elevation)}]" }
            val prayers = sky.prayers.joinToString(",", "[", "]") {
                "[${num(it.position.azimuth)},${num(it.position.elevation)},${jsString(it.name + " " + it.timeText)}]"
            }
            val now = if (sky.sunUp) "[${num(sky.now.azimuth)},${num(sky.now.elevation)}]" else "null"
            webViewRef?.evaluateJavascript(
                "window.setSky && window.setSky(${num(sky.events.riseAzimuth)}, ${num(sky.events.setAzimuth)}, $now, " +
                    "${sky.riseText?.let(::jsString) ?: "null"}, ${sky.setText?.let(::jsString) ?: "null"}, $path, $prayers);",
                null
            )
        }
    }

    // No background of its own: the page (or the dimmed wallpaper on today) shows through.
    Box(
        modifier = modifier
            .semantics { contentDescription = description }
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        // The map libraries are inlined into the page, so it never reads files.
                        allowContentAccess = false
                        allowFileAccess = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                        loadWithOverviewMode = true
                        useWideViewPort = true
                    }
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    // The page carries its own spoken summary on the Box above.
                    importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isMapLoaded = true
                        }
                    }

                    if (interactive) {
                        // On the map page every drag belongs to the map.
                        @SuppressLint("ClickableViewAccessibility")
                        setOnTouchListener { v, event ->
                            when (event.actionMasked) {
                                MotionEvent.ACTION_DOWN -> v.parent?.requestDisallowInterceptTouchEvent(true)
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.parent?.requestDisallowInterceptTouchEvent(false)
                            }
                            false
                        }
                    }

                    loadDataWithBaseURL("https://www.openstreetmap.org", mapHtml, "text/html", "UTF-8", null)
                    webViewRef = this
                    controller?.view = this
                }
            },
            modifier = Modifier.fillMaxSize(),
            // A new look (theme or accent) needs the page loaded again.
            update = { view ->
                if (view.tag != mapHtml) {
                    if (view.tag != null) {
                        isMapLoaded = false
                        view.loadDataWithBaseURL("https://www.openstreetmap.org", mapHtml, "text/html", "UTF-8", null)
                    }
                    view.tag = mapHtml
                }
            }
        )
    }
}

/** Distance rings drawn around the dot, in km. The last one is the compass ring. */
private val MAP_RING_KM = listOf(3, 5)

private const val MAP_LIBS_MARKER = "<!--MEENA_MAP_LIBS-->"

/**
 * Leaflet 1.9.4, MapLibre GL JS 5.24.1 and maplibre-gl-leaflet 0.1.4, shipped in assets/map
 * (licences in assets/map/LICENSES.txt; bundling approved 14 Nov 2026) instead of loaded from
 * unpkg.com each time. They are read once and inlined into the page, so the page needs no file
 * access and its origin stays the same for the tile servers.
 */
internal object MapAssets {
    @Volatile
    private var cached: String? = null

    /** Reads the libraries ahead of time (off the main thread), so the first map opens without a pause. */
    fun preload(context: android.content.Context) {
        runCatching { inlineTags(context.applicationContext) }
    }

    /** The licence notices for the map libraries and the bundled Selawik font, as shipped. */
    fun licences(context: android.content.Context): String =
        listOf("map/LICENSES.txt", "licences/selawik-OFL.txt").joinToString("\n\n") { path ->
            runCatching { context.assets.open(path).bufferedReader().use { it.readText() } }.getOrDefault("")
        }.trim()

    fun inlineTags(context: android.content.Context): String = cached ?: synchronized(this) {
        cached ?: buildString {
            fun read(name: String) = context.assets.open("map/$name").bufferedReader().use { it.readText() }
            append("<style>").append(read("leaflet.css")).append("</style>\n")
            append("<style>").append(read("maplibre-gl.css")).append("</style>\n")
            append("<script>").append(read("leaflet.js")).append("</script>\n")
            append("<script>").append(read("maplibre-gl.js")).append("</script>\n")
            append("<script>").append(read("leaflet-maplibre-gl.js")).append("</script>\n")
        }.also { cached = it }
    }
}

/** Everything that, when it changes, needs the map page built again. */
private data class MapLook(
    val accent: String,
    val styleUrl: String,
    val textColor: String,
    val mutedColor: String,
    val pageColor: String,
    val sunColor: String,
    val ringLabels: List<String>,
    val cardinals: List<String>,
    val qiblaWord: String,
    val interactive: Boolean,
    val detailed: Boolean
)

private fun num(v: Double?): String = v?.let { "%.1f".format(java.util.Locale.US, it) } ?: "null"

private fun jsString(text: String): String = "'" + text.replace("\\", "").replace("'", "") + "'"

/**
 * HTML for the map. Leaflet draws the tiles, the location dot and the 3 km ring; an SVG layer
 * on top draws the compass, qibla, sun and labels, and places labels so they don't overlap or
 * run off the edge. Nothing on it pretends to be weather data.
 */
private fun generateMapHtml(lat: Double, lng: Double, accuracyMeters: Float, look: MapLook): String {
    val ringKm = MAP_RING_KM.joinToString(",")
    val labels = look.ringLabels.joinToString(",") { jsString(it) }
    val cardinals = look.cardinals.joinToString(",") { jsString(it) }
    val qiblaWord = jsString(look.qiblaWord)
    val radius = accuracyMeters.coerceIn(100f, 5000f)
    val accent = look.accent
    val interactive = look.interactive
    val detailed = look.detailed
    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
$MAP_LIBS_MARKER
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  html, body { width: 100%; height: 100%; background: transparent; overflow: hidden; }
  /* Height is set from window.innerHeight in script: CSS vh and % resolve to 0 in this WebView. */
  #map { position: fixed; top: 0; left: 0; width: 100%; height: 0; background: transparent; }
  .leaflet-container { background: transparent; }
  /* Compass and labels sit over the map and never take touches. */
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
  var INTERACTIVE = $interactive;
  var DETAILED = $detailed;

  var map = L.map('map', {
    zoomControl: false,
    attributionControl: false,
    minZoom: 9,
    maxZoom: 17,
    // Quarter steps so the 5 km ring can fill the frame instead of snapping to half its size.
    zoomSnap: 0.25,
    dragging: INTERACTIVE,
    touchZoom: INTERACTIVE,
    doubleClickZoom: INTERACTIVE,
    scrollWheelZoom: INTERACTIVE,
    boxZoom: INTERACTIVE,
    keyboard: INTERACTIVE,
    preferCanvas: true
  }).setView([lat, lng], 13);

  var RING_KM = [$ringKm];
  var RING_LABELS = [$labels];
  var CARDINALS = [$cardinals];
  var QIBLA_WORD = $qiblaWord;
  var TEXT = '${look.textColor}', MUTED = '${look.mutedColor}', SUN = '${look.sunColor}', PAGE = '${look.pageColor}';
  var PULSE_MS = 3000;
  var WAVE_OPACITY = [0.6, 0.3];
  var ringsOn = false, pulseOn = false, timer = null;
  var heading = 0, qibla = null;
  var sky = { rise: null, set: null, now: null, riseText: null, setText: null, path: [], prayers: [] };

  // The WebView is laid out at 0 height inside the page and CSS heights stay 0, so size the
  // map from innerHeight in pixels, and fit again whenever that changes.
  function fitArea() {
    // With rings on, show the 5 km ring; else the ring the phone is unsure within, never closer than street level.
    var outerKm = RING_KM[RING_KM.length - 1];
    var target = ringsOn ? L.latLng(lat, lng).toBounds(outerKm * 2000) : area.getBounds();
    map.fitBounds(target, { padding: [DETAILED ? 28 : 16, DETAILED ? 28 : 16], maxZoom: 15, animate: false });
  }
  function fitToWindow() {
    var el = document.getElementById('map');
    var h = window.innerHeight;
    if (h > 0 && el.clientHeight !== h) {
      el.style.height = h + 'px';
      document.getElementById('overlay').style.height = h + 'px';
      map.invalidateSize();
      fitArea();
      drawOverlay();
    }
  }
  window.addEventListener('resize', fitToWindow);
  var fitTries = 0;
  var fitTimer = setInterval(function () {
    fitToWindow();
    if (++fitTries > 40) clearInterval(fitTimer);
  }, 250);

  // OpenFreeMap vector style (free, no key; https://openfreemap.org). Credit is shown below the map.
  var glLayer = L.maplibreGL({ style: '${look.styleUrl}' }).addTo(map);
  // The style paints its own background (near black, or off white), which showed as a box on
  // the page, so it is made see-through and the page or wallpaper shows. The style's roads and
  // names are tuned for that background (near-black roads on dark, white roads on light) and
  // vanish without it, so they are redrawn in the page's text colour, small roads fainter, with
  // names haloed in the page colour. Water and buildings become a faint tint.
  // https://maplibre.org/maplibre-style-spec/layers/
  var glMap = glLayer.getMaplibreMap();
  var restyled = false;
  function roadOpacity(id) {
    if (/motorway|trunk/.test(id)) return 0.7;
    if (/major|primary|secondary/.test(id)) return 0.55;
    if (/minor|path|service|track/.test(id)) return 0.35;
    return 0.25;
  }
  function restyle(layer) {
    var id = layer.id;
    var paint = function (k, v) { glMap.setPaintProperty(id, k, v); };
    if (layer.type === 'background') {
      paint('background-opacity', 0);
    } else if (layer.type === 'fill') {
      paint('fill-color', TEXT);
      paint('fill-opacity', /water/.test(id) ? 0.12 : 0.05);
      if (layer.paint && layer.paint['fill-outline-color']) paint('fill-outline-color', TEXT);
    } else if (layer.type === 'line') {
      // Casings and dash overlays only made sense against the old background.
      if (/casing|dashline/.test(id)) glMap.setLayoutProperty(id, 'visibility', 'none');
      else { paint('line-color', TEXT); paint('line-opacity', roadOpacity(id)); }
    } else if (layer.type === 'symbol' && layer.layout && layer.layout['text-field']) {
      paint('text-color', /place|label_(city|town|village|other|state|country)/.test(id) ? TEXT : MUTED);
      paint('text-halo-color', PAGE);
      paint('text-halo-width', 1.2);
    }
  }
  if (glMap) glMap.on('styledata', function () {
    if (restyled) return;
    try {
      var style = glMap.getStyle();
      if (!style || !style.layers) return;
      restyled = true;
      style.layers.forEach(function (layer) {
        try { restyle(layer); } catch (e) {}
      });
    } catch (e) {}
  });

  // How rough the location is: a thin ring, barely filled.
  var area = L.circle([lat, lng], {
    radius: $radius, color: accent, weight: 1, opacity: 0.8, fillColor: accent, fillOpacity: 0.06, interactive: false
  }).addTo(map);

  // Where the phone thinks it is.
  var here = L.circleMarker([lat, lng], {
    radius: 5, color: '#FFFFFF', weight: 1.5, fillColor: accent, fillOpacity: 1, interactive: false
  }).addTo(map);

  // The 3 km ring is a clear map line; the 5 km ring is the lighter compass ring in the overlay.
  // A soft wave grows out to each one (about 20 frames a second, paused when the page is hidden).
  var ring3 = L.circle([lat, lng], { radius: RING_KM[0] * 1000, color: accent, weight: 1.2, opacity: 0.85, fill: false, interactive: false });
  var waves = RING_KM.map(function () {
    return L.circle([lat, lng], { radius: 1, color: accent, weight: 1.5, opacity: 0, fillColor: accent, fillOpacity: 0, interactive: false });
  });
  function tick() {
    if (document.hidden) return;
    var now = Date.now();
    RING_KM.forEach(function (km, i) {
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

  // Overlay drawing. heading = which way the top of the phone points, from true north.
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
  // The sun sits between the rings by height: on the 5 km ring at the horizon, the 3 km ring overhead.
  function sunRadius(r3, r5, elevation) {
    return r5 - (r5 - r3) * Math.max(0, Math.min(90, elevation)) / 90;
  }

  // Labels are placed last, most important first. Each is kept inside the map and moved
  // outward (then inward) from the centre if it would cover one already placed; if it still
  // can't fit, it is left out rather than drawn on top of another.
  var pending = [];
  function label(text, x, y, color, opacity, priority) {
    if (text) pending.push({ text: text, x: x, y: y, color: color, opacity: opacity, priority: priority });
  }
  function placeLabels(c) {
    var W = overlay.clientWidth || window.innerWidth, H = window.innerHeight;
    var placed = [];
    pending.sort(function (a, b) { return a.priority - b.priority; });
    pending.forEach(function (l) {
      var w = l.text.length * 6.6 + 4, h = 15;
      var dx = l.x - c.x, dy = l.y - c.y, len = Math.sqrt(dx * dx + dy * dy) || 1;
      var tries = [0, 12, 24, -12, 36];
      for (var i = 0; i < tries.length; i++) {
        var x = l.x + dx / len * tries[i], y = l.y + dy / len * tries[i];
        x = Math.max(w / 2 + 2, Math.min(W - w / 2 - 2, x));
        y = Math.max(h / 2 + 2, Math.min(H - h / 2 - 2, y));
        var box = { l: x - w / 2, r: x + w / 2, t: y - h / 2, b: y + h / 2 };
        var hit = placed.some(function (p) { return box.l < p.r && box.r > p.l && box.t < p.b && box.b > p.t; });
        if (!hit) {
          placed.push(box);
          el('text', { x: x, y: y, 'text-anchor': 'middle', 'dominant-baseline': 'central', fill: l.color, 'fill-opacity': l.opacity }, l.text);
          return;
        }
      }
    });
    pending = [];
  }

  function drawOverlay() {
    while (overlay.firstChild) overlay.removeChild(overlay.firstChild);
    pending = [];
    if (!ringsOn || !document.getElementById('map').clientHeight) return;
    var c = map.latLngToContainerPoint([lat, lng]);
    var r3 = px(RING_KM[0]), r5 = px(RING_KM[RING_KM.length - 1]);

    // 5 km compass ring with ticks every 10 degrees, longer at 30 and 90.
    el('circle', { cx: c.x, cy: c.y, r: r5, fill: 'none', stroke: accent, 'stroke-width': 1, 'stroke-opacity': 0.35 });
    var d = '';
    for (var b = 0; b < 360; b += 10) {
      var len = (b % 90 === 0) ? 8 : (b % 30 === 0 ? 5 : 3);
      var p1 = at(c, r5, b), p2 = at(c, r5 - len, b);
      d += 'M' + p1.x.toFixed(1) + ',' + p1.y.toFixed(1) + 'L' + p2.x.toFixed(1) + ',' + p2.y.toFixed(1);
    }
    el('path', { d: d, stroke: MUTED, 'stroke-width': 1, 'stroke-opacity': 0.8, fill: 'none' });

    // North pointer: a small solid triangle on the 5 km ring, tip pointing out.
    var nTip = at(c, r5 + 9, 0), nLeft = at(c, r5 - 1, -4), nRight = at(c, r5 - 1, 4);
    el('path', { d: 'M' + nTip.x + ',' + nTip.y + 'L' + nLeft.x + ',' + nLeft.y + 'L' + nRight.x + ',' + nRight.y + 'Z', fill: accent });

    // Sun's path today (map page only): a faint dotted arc between the rings, with prayer marks.
    if (DETAILED && sky.path.length > 1) {
      var pd = '';
      sky.path.forEach(function (s, i) {
        var p = at(c, sunRadius(r3, r5, s[1]), s[0]);
        pd += (i ? 'L' : 'M') + p.x.toFixed(1) + ',' + p.y.toFixed(1);
      });
      el('path', { d: pd, stroke: SUN, 'stroke-width': 1.2, 'stroke-opacity': 0.55, 'stroke-dasharray': '2 4', fill: 'none', 'stroke-linecap': 'round' });
      sky.prayers.forEach(function (m) {
        var r = sunRadius(r3, r5, m[1]);
        var a = at(c, r - 5, m[0]), z = at(c, r + 5, m[0]);
        el('path', { d: 'M' + a.x + ',' + a.y + 'L' + z.x + ',' + z.y, stroke: SUN, 'stroke-width': 2, 'stroke-linecap': 'round' });
        var t = at(c, r + 16, m[0]);
        label(m[2], t.x, t.y, SUN, 0.9, 4);
      });
    }

    // Where the sun is now: a dotted line from the dot to it, while it's up.
    if (sky.now !== null) {
      var sr = sunRadius(r3, r5, sky.now[1]);
      var s1 = at(c, 9, sky.now[0]), s2 = at(c, sr, sky.now[0]);
      el('path', { d: 'M' + s1.x + ',' + s1.y + 'L' + s2.x + ',' + s2.y, stroke: SUN, 'stroke-width': 2,
        'stroke-dasharray': '0.1 5', 'stroke-linecap': 'round', fill: 'none' });
      el('circle', { cx: s2.x, cy: s2.y, r: 3.5, fill: SUN });
    }
    // Sunrise (filled) and sunset (open) on the 5 km ring.
    [['rise', sky.rise, sky.riseText], ['set', sky.set, sky.setText]].forEach(function (e) {
      if (e[1] === null) return;
      var p = at(c, r5, e[1]);
      el('circle', e[0] === 'rise'
        ? { cx: p.x, cy: p.y, r: 4, fill: SUN }
        : { cx: p.x, cy: p.y, r: 3.5, fill: 'none', stroke: SUN, 'stroke-width': 1.5 });
      if (DETAILED) { var t = at(c, r5 + 18, e[1]); label(e[2], t.x, t.y, SUN, 1, 2); }
    });

    // Qibla: a small square (the Kaaba) on the 3 km ring, with an arrow to it from the dot.
    if (qibla !== null) {
      var m = at(c, r3, qibla);
      var from = at(c, 9, qibla), tip = at(c, r3 - 6, qibla);
      var left = at(tip, 7, qibla + 155), right = at(tip, 7, qibla - 155);
      el('path', {
        d: 'M' + from.x + ',' + from.y + 'L' + tip.x + ',' + tip.y + 'M' + left.x + ',' + left.y + 'L' + tip.x + ',' + tip.y + 'L' + right.x + ',' + right.y,
        stroke: accent, 'stroke-width': 1.5, fill: 'none', 'stroke-linecap': 'round', 'stroke-linejoin': 'round'
      });
      el('rect', { x: m.x - 3.5, y: m.y - 3.5, width: 7, height: 7, fill: accent,
        transform: 'rotate(' + (qibla - heading) + ' ' + m.x + ' ' + m.y + ')' });
      if (DETAILED) { var w = at(c, r3 + 13, qibla); label(QIBLA_WORD, w.x, w.y, accent, 1, 1); }
    }

    // Words: n e s w first (always), then qibla, sun times, ring sizes, prayer marks.
    CARDINALS.forEach(function (word, i) {
      var p = at(c, r5 - 16, i * 90);
      label(word, p.x, p.y, i === 0 ? accent : TEXT, i === 0 ? 1 : 0.7, 0);
    });
    if (DETAILED) {
      label(RING_LABELS[0], c.x + r3 * 0.7, c.y + r3 * 0.7 - 8, accent, 0.9, 3);
      label(RING_LABELS[RING_LABELS.length - 1], c.x + r5 * 0.72 + 12, c.y + r5 * 0.72 + 6, accent, 0.6, 3);
    }
    placeLabels(c);
  }
  map.on('move zoom zoomend resize', drawOverlay);

  window.setCompass = function (newHeading, newQibla) {
    heading = newHeading || 0;
    qibla = (newQibla === null || newQibla === undefined) ? null : newQibla;
    drawOverlay();
  };
  window.setSky = function (rise, set, now, riseText, setText, path, prayers) {
    sky = { rise: rise, set: set, now: now, riseText: riseText, setText: setText, path: path || [], prayers: prayers || [] };
    drawOverlay();
  };
  window.setRings = function (on, animate) {
    var changed = on !== ringsOn;
    ringsOn = on;
    pulseOn = on && animate;
    stopPulse();
    if (on) ring3.addTo(map); else map.removeLayer(ring3);
    if (pulseOn) {
      waves.forEach(function (w) { w.addTo(map); });
      tick();
      timer = setInterval(tick, 50);
    }
    if (changed && document.getElementById('map').clientHeight > 0) fitArea();
    drawOverlay();
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
      [ring3].concat(waves).forEach(function (c) { c.setLatLng([lat, lng]); });
    }
    if (newRadius) area.setRadius(newRadius);
    if (document.getElementById('map').clientHeight > 0) fitArea();
    drawOverlay();
  };
  window.refit = function () { fitArea(); drawOverlay(); };
  window.zoomIn = function () { map.zoomIn(); };
  window.zoomOut = function () { map.zoomOut(); };
</script>
</body>
</html>
""".trimIndent()
}
