package com.example.meenalauncher.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meenalauncher.theme.MeenaBlack
import com.example.meenalauncher.theme.MeenaSurfaceElevated

@Composable
fun CornerNavHub(
    isLeftHanded: Boolean,
    isScrolling: Boolean,
    onNavigateToApps: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDialer: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenEmail: () -> Unit,
    onOpenCalculator: () -> Unit,
    onOpenTaskSwitcher: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(false) }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "fabRotation"
    )

    // Full screen scrim when expanded to dismiss on outside tap
    if (isExpanded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { isExpanded = false }
        )
    }

    // Anchor Container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(22.dp),
        contentAlignment = if (isLeftHanded) Alignment.BottomStart else Alignment.BottomEnd
    ) {
        // Hide FAB smoothly when scrolling
        AnimatedVisibility(
            visible = !isScrolling,
            enter = fadeIn(tween(180)) + scaleIn(tween(180)),
            exit = fadeOut(tween(180)) + scaleOut(tween(180))
        ) {
            Box(contentAlignment = if (isLeftHanded) Alignment.BottomStart else Alignment.BottomEnd) {

                // 1. VERTICAL TRAY (4 App Shortcuts stacking upwards)
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(tween(200)) + scaleIn(tween(200)),
                    exit = fadeOut(tween(150)) + scaleOut(tween(150))
                ) {
                    Column(
                        modifier = Modifier.padding(bottom = 58.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = if (isLeftHanded) Alignment.Start else Alignment.End
                    ) {
                        NavSubButton(icon = Icons.Default.Calculate, label = "Calc") {
                            isExpanded = false
                            onOpenCalculator()
                        }
                        NavSubButton(icon = Icons.Default.Email, label = "Email") {
                            isExpanded = false
                            onOpenEmail()
                        }
                        NavSubButton(icon = Icons.AutoMirrored.Filled.Message, label = "Messages") {
                            isExpanded = false
                            onOpenMessages()
                        }
                        NavSubButton(icon = Icons.Default.Phone, label = "Phone", isPrimary = true) {
                            isExpanded = false
                            onOpenDialer()
                        }
                    }
                }

                // 2. HORIZONTAL TRAY (App, Camera, Search, Setting)
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(tween(200)) + scaleIn(tween(200)),
                    exit = fadeOut(tween(150)) + scaleOut(tween(150))
                ) {
                    Row(
                        modifier = Modifier.padding(
                            start = if (isLeftHanded) 58.dp else 0.dp,
                            end = if (isLeftHanded) 0.dp else 58.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        if (isLeftHanded) {
                            NavSubButton(icon = Icons.Default.GridView, label = "app") {
                                isExpanded = false
                                onNavigateToApps()
                            }
                            NavSubButton(icon = Icons.Default.CameraAlt, label = "camera") {
                                isExpanded = false
                                onOpenCamera()
                            }
                            NavSubButton(icon = Icons.Default.Search, label = "search") {
                                isExpanded = false
                                onOpenSearch()
                            }
                            NavSubButton(icon = Icons.Default.Settings, label = "setting") {
                                isExpanded = false
                                onOpenSettings()
                            }
                        } else {
                            NavSubButton(icon = Icons.Default.Settings, label = "setting") {
                                isExpanded = false
                                onOpenSettings()
                            }
                            NavSubButton(icon = Icons.Default.Search, label = "search") {
                                isExpanded = false
                                onOpenSearch()
                            }
                            NavSubButton(icon = Icons.Default.CameraAlt, label = "camera") {
                                isExpanded = false
                                onOpenCamera()
                            }
                            NavSubButton(icon = Icons.Default.GridView, label = "app") {
                                isExpanded = false
                                onNavigateToApps()
                            }
                        }
                    }
                }

                // 3. MAIN ANCHOR BUTTON (Windows Phone ⊞ / ✕, Rounded Floating Action Button)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            color = if (isExpanded) Color.White else MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isExpanded = false
                                    onOpenTaskSwitcher()
                                },
                                onTap = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isExpanded = !isExpanded
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⊞",
                        fontSize = 26.sp,
                        color = if (isExpanded) MeenaBlack else Color.White,
                        modifier = Modifier.rotate(rotationAngle)
                    )
                }
            }
        }
    }
}

@Composable
private fun NavSubButton(
    icon: ImageVector,
    label: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(
                color = if (isPrimary) MaterialTheme.colorScheme.primary else MeenaSurfaceElevated,
                shape = CircleShape
            )
            .border(
                1.dp,
                if (isPrimary) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.25f),
                CircleShape
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label.lowercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 8.sp,
                lineHeight = 10.sp
            )
        }
    }
}
