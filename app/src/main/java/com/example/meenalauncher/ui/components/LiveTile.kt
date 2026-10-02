package com.example.meenalauncher.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun LiveTile(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    isFlipped: Boolean = false,
    onClick: () -> Unit = {},
    frontContent: @Composable () -> Unit,
    backContent: (@Composable () -> Unit)? = null
) {
    // 3D Tilt rotation state
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }

    // Smooth return to resting state
    val animatedTiltX by animateFloatAsState(
        targetValue = tiltX,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "tiltX"
    )
    val animatedTiltY by animateFloatAsState(
        targetValue = tiltY,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "tiltY"
    )
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "scale"
    )

    // Flip 3D rotation
    val flipRotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "flipRotation"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationX = animatedTiltX
                rotationY = animatedTiltY + flipRotation
                scaleX = animatedScale
                scaleY = animatedScale
                cameraDistance = 12f * density
            }
            .background(backgroundColor, RoundedCornerShape(0.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        tiltX = -((offset.y - centerY) / centerY) * 12f
                        tiltY = ((offset.x - centerX) / centerX) * 12f
                        scale = 0.96f
                        tryAwaitRelease()
                        tiltX = 0f
                        tiltY = 0f
                        scale = 1f
                    },
                    onTap = { onClick() }
                )
            }
    ) {
        if (flipRotation <= 90f || backContent == null) {
            frontContent()
        } else {
            // Mirror back content horizontally so it reads normally when flipped
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
            ) {
                backContent()
            }
        }
    }
}
