package my.hana.meena.ui.hubs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import my.hana.meena.R
import my.hana.meena.data.system.ClockInfo
import my.hana.meena.data.system.DeviceLocationHelper
import my.hana.meena.data.system.JakimSolatRepository
import my.hana.meena.data.system.Qibla
import my.hana.meena.theme.MeenaSpacing
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextWhite
import my.hana.meena.ui.components.MetroToggleRow
import my.hana.meena.ui.components.NearbyMap
import my.hana.meena.ui.components.NearbyMapController
import my.hana.meena.ui.components.mapDescription
import my.hana.meena.ui.components.metroClickable
import my.hana.meena.ui.components.rememberCompass
import my.hana.meena.ui.components.rememberSkyInfo
import my.hana.meena.ui.widgets.WidgetState
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.core.net.toUri

/**
 * The full nearby page: a large live map whose compass turns with the phone, which way to turn
 * for the qibla (with a short buzz when you face it), the sun's day and the place details.
 */
@Composable
fun NearbyMapSubPage(
    ringsOn: Boolean,
    animate: Boolean,
    is24Hour: Boolean,
    onRingsChange: (Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val location by DeviceLocationHelper.locationFlow.collectAsState()
    LaunchedEffect(Unit) { DeviceLocationHelper.refreshLocation(context) }
    val solatState by JakimSolatRepository.state.collectAsState()
    val slots = ((solatState as? WidgetState.Ready)?.data ?: (solatState as? WidgetState.Stale)?.data)?.slots.orEmpty()

    val placeName = ClockInfo.placeName(location.locationName, location.isGpsActive)
    val sky = rememberSkyInfo(location, is24Hour, slots)
    val compass = rememberCompass(active = ringsOn && location.isGpsActive, location = location)
    val controller = remember { NearbyMapController() }

    // A short buzz each time the phone comes round to face the qibla.
    val heading = compass.heading
    val turn = heading?.let { Qibla.turn(it, sky.qibla) }
    val facing = turn != null && abs(turn) <= Qibla.FACING_DEGREES
    var buzzed by remember { mutableStateOf(false) }
    LaunchedEffect(facing) {
        if (facing && !buzzed) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            buzzed = true
        }
        // Must turn clearly away before it buzzes again, so small wobbles don't keep buzzing.
        if (!facing && turn != null && abs(turn) > Qibla.FACING_DEGREES * 3) buzzed = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(my.hana.meena.theme.LocalMeenaPalette.current.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = MeenaSpacing.l, end = MeenaSpacing.l, top = MeenaSpacing.l, bottom = MeenaSpacing.m),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_thin_back),
                contentDescription = stringResource(R.string.settings_back),
                tint = MeenaTextWhite,
                modifier = Modifier
                    .size(48.dp)
                    .metroClickable(onClick = onNavigateBack)
                    .padding(MeenaSpacing.m)
            )
            Spacer(modifier = Modifier.width(MeenaSpacing.s))
            Text(
                text = stringResource(R.string.map_title),
                style = MaterialTheme.typography.displayMedium,
                color = MeenaTextWhite
            )
        }

        if (!location.isGpsActive) {
            Text(
                text = stringResource(R.string.map_no_location),
                style = MaterialTheme.typography.bodyMedium,
                color = MeenaTextMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .metroClickable { DeviceLocationHelper.refreshLocation(context) }
                    .padding(horizontal = MeenaSpacing.l)
            )
            return@Column
        }

        NearbyMap(
            location = location,
            sky = sky,
            heading = heading,
            ringsOn = ringsOn,
            animate = animate,
            interactive = true,
            detailed = true,
            description = mapDescription(placeName, sky),
            controller = controller,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MeenaSpacing.l, vertical = MeenaSpacing.m),
            verticalArrangement = Arrangement.spacedBy(MeenaSpacing.s)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(MeenaSpacing.l)) {
                MapAction(stringResource(R.string.map_zoom_in)) { controller.zoomIn() }
                MapAction(stringResource(R.string.map_zoom_out)) { controller.zoomOut() }
                MapAction(stringResource(R.string.map_recenter)) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    DeviceLocationHelper.refreshLocation(context)
                    controller.recenter()
                }
                Spacer(modifier = Modifier.weight(1f))
                MapAction(stringResource(R.string.map_open)) {
                    DeviceLocationHelper.openInExternalMaps(context, location.latitude, location.longitude, location.locationName)
                }
            }

            // Qibla: which way to turn, in big type; the bearing underneath.
            if (ringsOn) {
                val qiblaDegrees = sky.qibla.roundToInt()
                Text(
                    text = when {
                        turn == null -> stringResource(R.string.map_qibla_from_north, qiblaDegrees)
                        facing -> stringResource(R.string.map_qibla_facing)
                        turn < 0 -> stringResource(R.string.map_qibla_turn_left, abs(turn).roundToInt())
                        else -> stringResource(R.string.map_qibla_turn_right, turn.roundToInt())
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = when {
                        !compass.available -> stringResource(R.string.map_compass_missing, qiblaDegrees)
                        compass.unsteady -> stringResource(R.string.map_compass_unsteady)
                        turn != null -> stringResource(R.string.map_qibla_from_north, qiblaDegrees)
                        else -> stringResource(R.string.map_compass_waiting)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted
                )
            }

            Column(modifier = Modifier.padding(top = MeenaSpacing.s)) {
                if (placeName != null) DetailRow(stringResource(R.string.map_detail_place), placeName)
                location.region?.let { DetailRow(stringResource(R.string.map_detail_region), it) }
                DetailRow(stringResource(R.string.map_detail_coordinates), location.formattedCoordinates)
                DetailRow(
                    stringResource(R.string.map_detail_accuracy),
                    if (location.accuracyMeters < 1000f) stringResource(R.string.map_accuracy_value_m, location.accuracyMeters.coerceAtLeast(1f).toInt())
                    else stringResource(R.string.map_accuracy_value_km, "%.1f".format(java.util.Locale.UK, location.accuracyMeters / 1000f))
                )
                val minutesAgo = ((System.currentTimeMillis() - location.fixTimeMillis) / 60_000L).coerceAtLeast(0L)
                if (location.fixTimeMillis > 0L) {
                    DetailRow(
                        stringResource(R.string.map_detail_found),
                        when {
                            minutesAgo < 1L -> stringResource(R.string.map_found_just_now)
                            minutesAgo < 60L -> stringResource(R.string.map_found_minutes, minutesAgo.toInt())
                            else -> stringResource(R.string.map_found_hours, (minutesAgo / 60L).toInt())
                        }
                    )
                }
                sky.riseText?.let { DetailRow(stringResource(R.string.map_detail_sunrise), it) }
                sky.setText?.let { DetailRow(stringResource(R.string.map_detail_sunset), it) }
                DetailRow(
                    stringResource(R.string.map_detail_sun_now),
                    if (sky.sunUp) stringResource(R.string.map_sun_up, sky.now.elevation.roundToInt()) else stringResource(R.string.map_sun_down)
                )
            }

            // Without precise location Android blurs the spot on purpose; say so and point to the switch.
            if (location.accuracyMeters >= 1000f) {
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
                        .padding(vertical = MeenaSpacing.m)
                )
            }

            MetroToggleRow(
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
                    text = stringResource(R.string.map_legend),
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted
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
                                android.content.Intent(android.content.Intent.ACTION_VIEW, "https://www.openstreetmap.org/copyright".toUri())
                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {}
                    }
                    .padding(vertical = MeenaSpacing.m)
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MeenaSpacing.s),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MeenaTextMuted)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MeenaTextWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = MeenaSpacing.m)
        )
    }
}

@Composable
private fun MapAction(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .metroClickable(onClick = onClick)
            .padding(vertical = MeenaSpacing.m)
    )
}
