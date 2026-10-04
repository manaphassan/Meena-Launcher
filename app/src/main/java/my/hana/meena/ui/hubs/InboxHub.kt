package my.hana.meena.ui.hubs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import my.hana.meena.ui.components.metroClickable
import my.hana.meena.ui.components.metroFeatherIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.data.system.NotificationRepository
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextSecondary
import my.hana.meena.theme.MeenaTextWhite
import my.hana.meena.ui.components.CollapsibleWidget
import my.hana.meena.ui.components.ConversationsWidget
import my.hana.meena.ui.components.MetroDialog

@Composable
fun InboxHub(
    settings: MeenaUserSettings,
    listState: LazyListState
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val notifications by NotificationRepository.notificationsFlow.collectAsState()
    val isListenerConnected by NotificationRepository.isListenerConnectedFlow.collectAsState()
    var isNotificationAccessGranted by remember {
        mutableStateOf(NotificationRepository.isNotificationAccessGranted(context))
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

    LaunchedEffect(isListenerConnected) {
        isNotificationAccessGranted = isListenerConnected || NotificationRepository.isNotificationAccessGranted(context)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        val inboxWidgets = settings.widgetConfigs.entries
            .filter { (it.value.hubId == "inbox" || it.value.hubId == "start" && (it.key == "widget-conversations" || it.key == "widget-notifications")) && it.value.isEnabled }
            .sortedBy { it.value.position }

        items(inboxWidgets.size, key = { inboxWidgets[it].key }) { index ->
            val widgetId = inboxWidgets[index].key
            when (widgetId) {
                "widget-conversations" -> {
                    // Chats and messaging threads
                    ConversationsWidget()
                }

                "widget-notifications" -> {
                    // Alerts stream
                    CollapsibleWidget(
                        title = "alerts",
                        collapsedSummary = {
                            Text(
                                text = when {
                                    !isNotificationAccessGranted -> "needs a quick okay"
                                    notifications.isNotEmpty() -> "${notifications.size} new"
                                    else -> "all quiet"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (!isNotificationAccessGranted) MeenaTextMuted else MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!isNotificationAccessGranted) {
                                Text(
                                    text = "want your alerts here? turn on notification access. meena reads them on your phone only.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MeenaTextMuted,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .metroClickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showNotificationDisclosure = true
                                        }
                                        .padding(vertical = 8.dp)
                                )
                            } else if (notifications.isNotEmpty()) {
                                notifications.take(8).forEachIndexed { idx, notif ->
                                    val notifColor = when {
                                        notif.packageName.contains("whatsapp", ignoreCase = true) -> Color(0xFF25D366)
                                        notif.packageName.contains("telegram", ignoreCase = true) -> Color(0xFF0088CC)
                                        notif.packageName.contains("gmail", ignoreCase = true) -> Color(0xFFEA4335)
                                        notif.packageName.contains("messaging", ignoreCase = true) -> Color(0xFF00A4EF)
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                    InboxNotificationRow(
                                        modifier = Modifier.metroFeatherIn(
                                            index = idx,
                                            reducedMotion = settings.reducedMotion
                                        ),
                                        badgeColor = notifColor,
                                        app = notif.appName,
                                        title = notif.title,
                                        text = notif.text,
                                        time = notif.formattedTime,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            NotificationRepository.openNotification(context, notif)
                                        },
                                        onDismiss = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            NotificationRepository.dismissNotification(notif.key)
                                        }
                                    )
                                }

                                // Clear All Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "clear all",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextMuted,
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .metroClickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                NotificationRepository.clearAll()
                                            }
                                            .padding(vertical = 8.dp, horizontal = 4.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "all quiet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MeenaTextMuted,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(64.dp)) }
    }
}

@Composable
private fun InboxNotificationRow(
    modifier: Modifier = Modifier,
    badgeColor: Color,
    app: String,
    title: String,
    text: String,
    time: String,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .metroClickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .background(badgeColor)
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = app.lowercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = time.lowercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MeenaTextWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (text.isNotBlank()) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .metroClickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "x",
                fontSize = 13.sp,
                color = MeenaTextMuted
            )
        }
    }
}
