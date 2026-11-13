package my.hana.meena.ui.components

import androidx.compose.ui.res.stringResource
import my.hana.meena.R
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import my.hana.meena.ui.components.metroClickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import my.hana.meena.data.system.LiveConversationItem
import my.hana.meena.data.system.NotificationRepository
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextSecondary
import my.hana.meena.theme.MeenaProfitGreen
import my.hana.meena.theme.MeenaTextWhite
import my.hana.meena.theme.MeenaBlack

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
            title = stringResource(R.string.notif_access_title),
            message = stringResource(R.string.notif_access_question),
            confirmText = stringResource(R.string.action_yes),
            dismissText = stringResource(R.string.action_no),
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
        title = stringResource(R.string.widget_name_chats),
        collapsedSummary = {
            Text(
                text = when {
                    !isPermissionGranted -> stringResource(R.string.state_needs_quick_okay)
                    conversations.isNotEmpty() -> stringResource(R.string.state_new_count, conversations.size)
                    else -> stringResource(R.string.state_all_quiet)
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (!isPermissionGranted) MeenaTextMuted else MaterialTheme.colorScheme.primary
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)
        ) {
            if (!isPermissionGranted) {
                Text(
                    text = stringResource(R.string.inbox_chats_prompt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .metroClickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showNotificationDisclosure = true
                        }
                        .padding(vertical = my.hana.meena.theme.MeenaSpacing.s)
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
    val appColor = MeenaTextWhite

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .metroClickable(onClick = onClick)
            .padding(vertical = my.hana.meena.theme.MeenaSpacing.s)
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
                horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
            ) {
                Box(
                    modifier = Modifier
                        .size(my.hana.meena.theme.MeenaSpacing.s)
                        .background(appColor)
                )
                Text(
                    text = item.senderOrContact.lowercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    color = MeenaTextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
            ) {
                if (item.unreadCount > 1) {
                    Box(
                        modifier = Modifier
                            .background(appColor)
                            .padding(horizontal = my.hana.meena.theme.MeenaSpacing.s, vertical = 0.dp)
                    ) {
                        Text(
                            text = "${item.unreadCount}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MeenaBlack
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
            modifier = Modifier.padding(start = my.hana.meena.theme.MeenaSpacing.m, top = 0.dp)
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
                modifier = Modifier.padding(start = my.hana.meena.theme.MeenaSpacing.m, top = my.hana.meena.theme.MeenaSpacing.s)
            )
        }
    }
}

