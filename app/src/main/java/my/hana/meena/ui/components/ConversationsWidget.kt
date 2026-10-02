package my.hana.meena.ui.components

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
import my.hana.meena.data.system.LiveConversationItem
import my.hana.meena.data.system.NotificationRepository
import my.hana.meena.theme.MeenaBorder
import my.hana.meena.theme.MeenaProfitGreen
import my.hana.meena.theme.MeenaSurface
import my.hana.meena.theme.MeenaSurfaceElevated
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextSecondary
import my.hana.meena.theme.MeenaTextWhite
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

    var isPermissionGranted by remember(isConnected) {
        mutableStateOf(isConnected || NotificationRepository.isNotificationAccessGranted(context))
    }

    // Only re-check permission status when not yet connected, stops polling as soon as access is confirmed
    LaunchedEffect(isConnected) {
        if (!isConnected) {
            while (true) {
                val granted = NotificationRepository.isNotificationAccessGranted(context)
                if (granted) {
                    isPermissionGranted = true
                    break
                }
                delay(3000)
            }
        } else {
            isPermissionGranted = true
        }
    }

    CollapsibleWidget(
        title = "conversations",
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
                        !isPermissionGranted -> "permission required"
                        conversations.isNotEmpty() -> "${conversations.size} active chats"
                        else -> "connected"
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
                                contentDescription = "warning",
                                tint = Color(0xFFE51400),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "notification access required",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE51400)
                            )
                        }
                        Text(
                            text = "tap here to grant notification listener access so incoming chats update automatically.",
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
                                text = "enable in settings",
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
                                text = "messages ready",
                                style = MaterialTheme.typography.labelSmall,
                                color = MeenaProfitGreen
                            )
                        }
                        Text(
                            text = "all caught up. incoming chats from messaging apps will appear here automatically.",
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        // Line 1: Accent pill + Contact Name (weighted) + Time + Unread Count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(appColor)
                )
                Text(
                    text = item.senderOrContact.lowercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MeenaTextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (item.unreadCount > 1) {
                    Box(
                        modifier = Modifier
                            .background(appColor)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${item.unreadCount}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Text(
                    text = item.formattedTime.lowercase(),
                    fontSize = 11.sp,
                    color = MeenaTextMuted
                )
            }
        }

        // Line 2: App Channel tag
        Text(
            text = item.appName.lowercase(),
            fontSize = 11.sp,
            color = appColor,
            modifier = Modifier.padding(start = 14.dp, top = 2.dp)
        )

        // Line 3: Message preview (Vertical stack with multi-line wrap, up to 4 lines)
        if (item.messageSnippet.isNotBlank()) {
            Text(
                text = item.messageSnippet,
                style = MaterialTheme.typography.bodySmall,
                color = MeenaTextSecondary,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 14.dp, top = 4.dp)
            )
        }

        // Subtle divider separating conversation threads
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MeenaBorder.copy(alpha = 0.5f))
        )
    }
}

