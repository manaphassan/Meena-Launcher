package my.hana.meena.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import kotlinx.coroutines.delay
import my.hana.meena.data.system.LiveConversationItem
import my.hana.meena.data.system.NotificationRepository
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextSecondary
import my.hana.meena.theme.MeenaTextWhite

/**
 * Authentic Meena UI widget for messaging conversations (WhatsApp, Telegram, Messages, SMS).
 * Adheres strictly to Meena UI principles: no cards, no borders, pure typographic hierarchy,
 * generous spacing, casual lowercase copy, and zero decoration.
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
    var showNotificationDisclosure by remember { mutableStateOf(false) }

    if (showNotificationDisclosure) {
        MetroDialog(
            title = "notification access",
            message = "meena reads incoming notifications solely on your phone to show alerts and chats. your messages and notifications are processed locally in memory and never sent to any server, cloud, or third party.\n\ndo you want to open notification access settings?",
            confirmText = "yes",
            dismissText = "no",
            onConfirm = {
                showNotificationDisclosure = false
                NotificationRepository.openNotificationSettings(context)
            },
            onDismiss = { showNotificationDisclosure = false }
        )
    }

    LaunchedEffect(isConnected) {
        if (!isConnected) {
            while (true) {
                val granted = NotificationRepository.isNotificationAccessGranted(context)
                if (granted) {
                    isPermissionGranted = true
                    break
                }
                delay(3000L)
            }
        } else {
            isPermissionGranted = true
        }
    }

    CollapsibleWidget(
        title = "chats",
        collapsedSummary = {
            Text(
                text = when {
                    !isPermissionGranted -> "needs a quick okay"
                    conversations.isNotEmpty() -> "${conversations.size} new"
                    else -> "all quiet"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (!isPermissionGranted) MeenaTextMuted else MaterialTheme.colorScheme.primary
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isPermissionGranted) {
                Text(
                    text = "want your chats here? turn on notification access. meena reads them on your phone only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showNotificationDisclosure = true
                        }
                        .padding(vertical = 8.dp)
                )
            } else if (conversations.isEmpty()) {
                Text(
                    text = "all quiet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                conversations.take(6).forEach { item ->
                    ConversationRow(
                        item = item,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            NotificationRepository.openConversation(context, item)
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
    onClick: () -> Unit
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp)
    ) {
        // Line 1: Contact name + time + unread count
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
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted
                )
            }
        }

        // Line 2: App Channel tag
        Text(
            text = item.appName.lowercase(),
            style = MaterialTheme.typography.labelSmall,
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
    }
}
