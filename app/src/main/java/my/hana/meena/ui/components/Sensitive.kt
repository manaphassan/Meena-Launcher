package my.hana.meena.ui.components

import android.os.Build
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import my.hana.meena.data.system.PrivacyVeil
import my.hana.meena.theme.MeenaSurfaceElevated

/**
 * Marks private content (messages, email, notes, money). While [PrivacyVeil] is on it is blurred
 * (Android 12+, where Compose blur works) or covered with a flat block on older phones, and
 * screen readers skip it too. This is the only blur Meena uses besides the news picture filter.
 */
fun Modifier.sensitive(): Modifier = composed {
    val hidden by PrivacyVeil.hidden.collectAsState()
    if (!hidden) return@composed this
    val cover = MeenaSurfaceElevated
    val veiled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        blur(10.dp, BlurredEdgeTreatment.Rectangle)
    } else {
        drawWithContent { drawRect(cover) }
    }
    veiled.clearAndSetSemantics { }
}
