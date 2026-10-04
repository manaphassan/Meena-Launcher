package my.hana.meena.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import my.hana.meena.theme.MeenaTextMuted

/**
 * Standard Meena UI dialog complying strictly with Windows Phone Metro charter:
 * - Lowercase text everywhere
 * - No cards, borders, or elevation; solid black background
 * - Square corners (0dp)
 * - Title, concise human-readable message or question
 * - Side-by-side "yes" / "no" buttons at the bottom (>= 48dp touch target)
 */
@Composable
fun MetroDialog(
    title: String,
    message: String,
    confirmText: String = "yes",
    dismissText: String = "no",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        val animatable = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = tween<Float>(180, easing = FastOutSlowInEasing)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = animatable.value
                    scaleX = 0.96f + (0.04f * animatable.value)
                    scaleY = 0.96f + (0.04f * animatable.value)
                    rotationX = (1f - animatable.value) * 4f
                    cameraDistance = 24f * density
                }
                .background(Color.Black)
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = title.lowercase(),
                    style = MaterialTheme.typography.displayMedium,
                    fontSize = 24.sp,
                    color = Color.White
                )
                Text(
                    text = message.lowercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MeenaTextMuted
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .background(MaterialTheme.colorScheme.primary)
                            .metroClickable(onClick = onConfirm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = confirmText.lowercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .background(Color(0xFF161616))
                            .metroClickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dismissText.lowercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MeenaTextMuted
                        )
                    }
                }
            }
        }
    }
}
