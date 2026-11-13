package my.hana.meena.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.hana.meena.R
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextWhite

/** Square tiles for the apps opened most from Meena. Same tile look as favourites, no editing. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FrequentAppTiles(
    packageNames: List<String>,
    isMonochrome: Boolean,
    onLaunchApp: (String) -> Unit
) {
    val apps = packageNames.mapNotNull { my.hana.meena.data.system.InstalledAppsRepository.getCachedApp(it) }
    if (apps.isEmpty()) {
        Text(
            text = stringResource(R.string.frequent_apps_empty),
            style = MaterialTheme.typography.labelSmall,
            color = MeenaTextMuted
        )
        return
    }

    val tileColor = MaterialTheme.colorScheme.primary
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s),
        verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s),
        maxItemsInEachRow = 4
    ) {
        apps.forEach { app ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(tileColor)
                    .metroClickable { onLaunchApp(app.packageName) }
            ) {
                val glyph = if (isMonochrome) app.glyphBitmap else null
                val bitmap = glyph ?: app.iconBitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = app.label,
                        modifier = Modifier.align(Alignment.Center).size(if (glyph != null) 40.dp else 32.dp),
                        colorFilter = if (glyph != null) ColorFilter.tint(my.hana.meena.theme.MeenaOnAccent) else null
                    )
                }
                Text(
                    text = app.label.lowercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = my.hana.meena.theme.MeenaOnAccent,
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.BottomStart).padding(my.hana.meena.theme.MeenaSpacing.s)
                )
            }
        }
        val remainder = (4 - apps.size % 4) % 4
        repeat(remainder) { Spacer(modifier = Modifier.weight(1f).aspectRatio(1f)) }
    }
}
