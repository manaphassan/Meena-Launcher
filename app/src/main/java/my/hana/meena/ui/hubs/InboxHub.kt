package my.hana.meena.ui.hubs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.data.system.NotificationRepository
import my.hana.meena.theme.MeenaBorder
import my.hana.meena.theme.MeenaProfitGreen
import my.hana.meena.theme.MeenaSurface
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextSecondary
import my.hana.meena.theme.MeenaTextWhite
import my.hana.meena.ui.components.CollapsibleWidget
import my.hana.meena.ui.components.ConversationsWidget

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

    LaunchedEffect(isListenerConnected) {
        isNotificationAccessGranted = isListenerConnected || NotificationRepository.isNotificationAccessGranted(context)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
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
                    // Chats and instant messaging threads
                    ConversationsWidget()
                }

                "widget-notifications" -> {
                    // Alert stream
                    CollapsibleWidget(
                        title = "notification stream",
                        collapsedSummary = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (isNotificationAccessGranted) MeenaProfitGreen else Color(0xFFE51400),
                                            RoundedCornerShape(0.dp)
                                        )
                                )
                                Text(
                                    text = when {
                                        !isNotificationAccessGranted -> "access required"
                                        notifications.isNotEmpty() -> "${notifications.size} unread notifications"
                                        else -> "listening"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MeenaTextMuted
                                )
                            }
                        }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (!isNotificationAccessGranted) {
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFE51400),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "notification access required",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFE51400)
                                            )
                                            Text(
                                                text = "tap to grant access so notifications show up",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            } else if (notifications.isNotEmpty()) {
                                notifications.take(8).forEach { notif ->
                                    val notifColor = when {
                                        notif.packageName.contains("whatsapp", ignoreCase = true) -> Color(0xFF25D366)
                                        notif.packageName.contains("telegram", ignoreCase = true) -> Color(0xFF0088CC)
                                        notif.packageName.contains("gmail", ignoreCase = true) -> Color(0xFFEA4335)
                                        notif.packageName.contains("messaging", ignoreCase = true) -> Color(0xFF00A4EF)
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                    InboxNotificationRow(
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
                                        text = "clear all notifications",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = MeenaTextMuted,
                                        modifier = Modifier
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                NotificationRepository.clearAll()
                                            }
                                            .padding(vertical = 8.dp, horizontal = 4.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MeenaSurface)
                                        .border(1.dp, MeenaBorder)
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(MeenaProfitGreen, RoundedCornerShape(0.dp))
                                        )
                                        Text(
                                            text = "all caught up • listening",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MeenaTextMuted
                                        )
                                    }
                                }
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
    badgeColor: Color,
    app: String,
    title: String,
    text: String,
    time: String,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MeenaSurface)
            .border(1.dp, MeenaBorder)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(34.dp)
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
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
                Text(time, style = MaterialTheme.typography.labelSmall, color = MeenaTextMuted)
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Text("✕", fontSize = 11.sp, color = MeenaTextMuted)
        }
    }
}
