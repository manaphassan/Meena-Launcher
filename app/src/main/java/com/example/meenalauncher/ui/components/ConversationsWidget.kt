package com.example.meenalauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meenalauncher.data.system.LiveConversationItem
import com.example.meenalauncher.data.system.NotificationRepository
import com.example.meenalauncher.theme.MeenaBorder
import com.example.meenalauncher.theme.MeenaProfitGreen
import com.example.meenalauncher.theme.MeenaSurface
import com.example.meenalauncher.theme.MeenaSurfaceElevated
import com.example.meenalauncher.theme.MeenaTextMuted
import com.example.meenalauncher.theme.MeenaTextSecondary
import com.example.meenalauncher.theme.MeenaTextWhite
import kotlinx.coroutines.delay

/**
 * Authentic Windows Phone / Metro style widget for real-time incoming
 * messaging conversations (WhatsApp, Telegram, Google Messages, SMS, etc.).
 */
@Composable
fun ConversationsWidget(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val conversations by NotificationRepository.conversationsFlow.collectAsState()
    val isConnected by NotificationRepository.isListenerConnectedFlow.collectAsState()

    var isPermissionGranted by remember {
        mutableStateOf(NotificationRepository.isNotificationAccessGranted(context))
    }

    // Periodically re-check permission status when user returns from Settings
    LaunchedEffect(Unit) {
        while (true) {
            isPermissionGranted = NotificationRepository.isNotificationAccessGranted(context)
            delay(2000)
        }
    }

    CollapsibleWidget(
        title = "conversations (realtime)",
        collapsedSummary = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            if (isPermissionGranted) MeenaProfitGreen else Color(0xFFE51400),
                            RoundedCornerShape(0.dp)
                        )
                )
                Text(
                    text = when {
                        !isPermissionGranted -> "Permission Required"
                        conversations.isNotEmpty() -> "${conversations.size} active conversations"
                        else -> "Realtime Sync Active"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted
                )
            }
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Permission Banner if notification access is missing
            if (!isPermissionGranted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2A1010))
                        .border(1.dp, Color(0xFFE51400))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            NotificationRepository.openNotificationSettings(context)
                        }
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = Color(0xFFE51400),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "NOTIFICATION ACCESS REQUIRED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE51400),
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "Tap here to allow Meena Launcher to access incoming notifications so your WhatsApp, Telegram, and SMS conversations update in real-time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            lineHeight = 16.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE51400))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "ENABLE IN SETTINGS →",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            } else if (conversations.isEmpty()) {
                // Empty state when sync is active but no unread chats exist
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MeenaSurface)
                        .border(1.dp, MeenaBorder)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(MeenaProfitGreen, RoundedCornerShape(0.dp))
                            )
                            Text(
                                text = "REALTIME LISTENER ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaProfitGreen,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "All caught up! Real-time chats from WhatsApp, Telegram, SMS and messaging apps will appear here as soon as they arrive.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MeenaTextMuted,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                // Display up to 6 real-time conversations
                conversations.take(6).forEach { item ->
                    ConversationRow(
                        item = item,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            NotificationRepository.openConversation(context, item)
                        },
                        onDismiss = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            NotificationRepository.dismissNotification(item.key)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    item: LiveConversationItem,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val appColor = when {
        item.packageName.contains("whatsapp", ignoreCase = true) -> Color(0xFF25D366)
        item.packageName.contains("telegram", ignoreCase = true) -> Color(0xFF0088CC)
        item.packageName.contains("messaging", ignoreCase = true) || item.packageName.contains("mms", ignoreCase = true) -> Color(0xFF00A4EF)
        item.packageName.contains("discord", ignoreCase = true) -> Color(0xFF5865F2)
        item.packageName.contains("slack", ignoreCase = true) -> Color(0xFF4A154B)
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MeenaSurface)
            .border(1.dp, MeenaBorder)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // App / Channel Accent Strip
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(38.dp)
                .background(appColor)
        )

        // Conversation details
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.senderOrContact,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MeenaTextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "• ${item.appName}",
                        fontSize = 10.sp,
                        color = appColor
                    )
                }
                Text(
                    text = item.formattedTime,
                    fontSize = 10.sp,
                    color = MeenaTextMuted
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.messageSnippet,
                style = MaterialTheme.typography.labelSmall,
                color = MeenaTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
        }

        // Unread badge or dismiss button
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(appColor)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (item.unreadCount > 1) "${item.unreadCount}" else "✕",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
