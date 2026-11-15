package my.hana.meena.ui.hubs

import my.hana.meena.R
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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import my.hana.meena.data.system.InboxSort
import my.hana.meena.data.system.MalaysianEnglish
import my.hana.meena.data.system.NotificationRepository
import my.hana.meena.ui.components.sensitive
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextSecondary
import my.hana.meena.theme.MeenaProfitGreen
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
    // Chats and email have their own sections when they're on; alerts keep the rest, grouped by app,
    // so nothing shows twice.
    val chatsOn = settings.widgetConfigs["widget-conversations"]?.isEnabled != false
    val emailOn = settings.widgetConfigs["widget-email"]?.isEnabled != false
    val emails = remember(notifications) { InboxSort.email(notifications) }
    val alertGroups = remember(notifications, chatsOn, emailOn) {
        InboxSort.byApp(InboxSort.alerts(notifications, chatsOn, emailOn))
    }
    val lines = settings.widgetLines
    val isListenerConnected by NotificationRepository.isListenerConnectedFlow.collectAsState()
    var isNotificationAccessGranted by remember {
        mutableStateOf(NotificationRepository.isNotificationAccessGranted(context))
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

    LaunchedEffect(isListenerConnected) {
        isNotificationAccessGranted = isListenerConnected || NotificationRepository.isNotificationAccessGranted(context)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = my.hana.meena.theme.MeenaSpacing.l),
        verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)
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
                    ConversationsWidget(is24Hour = settings.is24HourFormat, maxItems = lines)
                }

                "widget-email" -> {
                    // Email from the phone's mail apps (Gmail and others), newest first
                    CollapsibleWidget(
                        title = stringResource(R.string.widget_name_email),
                        collapsedSummary = {
                            Text(
                                text = when {
                                    !isNotificationAccessGranted -> stringResource(R.string.state_needs_quick_okay)
                                    emails.isNotEmpty() -> stringResource(R.string.state_new_count, emails.size)
                                    else -> stringResource(R.string.state_all_quiet)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (!isNotificationAccessGranted) MeenaTextMuted else MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
                        ) {
                            when {
                                !isNotificationAccessGranted -> AccessPrompt(
                                    text = stringResource(R.string.inbox_email_prompt),
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showNotificationDisclosure = true
                                    }
                                )
                                emails.isEmpty() -> QuietLine()
                                else -> {
                                    emails.take(lines).forEachIndexed { idx, mail ->
                                        InboxNotificationRow(
                                            modifier = Modifier
                                                .metroFeatherIn(index = idx, reducedMotion = settings.reducedMotion)
                                                .sensitive(),
                                            badgeColor = MaterialTheme.colorScheme.primary,
                                            app = mail.appName,
                                            title = mail.title,
                                            text = mail.text,
                                            time = MalaysianEnglish.clockTime(mail.timestamp, settings.is24HourFormat),
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                NotificationRepository.openNotification(context, mail)
                                            },
                                            onDismiss = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                NotificationRepository.dismissNotification(mail.key)
                                            }
                                        )
                                    }
                                    MoreLine(hidden = emails.size - lines)
                                }
                            }
                        }
                    }
                }

                "widget-notifications" -> {
                    // Alerts stream
                    CollapsibleWidget(
                        title = stringResource(R.string.widget_name_alerts),
                        collapsedSummary = {
                            Text(
                                text = when {
                                    !isNotificationAccessGranted -> stringResource(R.string.state_needs_quick_okay)
                                    alertGroups.isNotEmpty() -> stringResource(R.string.state_new_count, alertGroups.sumOf { it.items.size })
                                    else -> stringResource(R.string.state_all_quiet)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (!isNotificationAccessGranted) MeenaTextMuted else MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.s)
                        ) {
                            if (!isNotificationAccessGranted) {
                                Text(
                                    text = stringResource(R.string.inbox_alerts_prompt),
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
                            } else if (alertGroups.isNotEmpty()) {
                                // One row per app: its newest alert, with how many it has waiting.
                                alertGroups.take(lines).forEachIndexed { idx, group ->
                                    val notif = group.latest
                                    InboxNotificationRow(
                                        modifier = Modifier
                                            .metroFeatherIn(index = idx, reducedMotion = settings.reducedMotion)
                                            .sensitive(),
                                        badgeColor = MeenaTextWhite,
                                        app = if (group.items.size > 1) stringResource(R.string.inbox_app_count, group.appName, group.items.size) else group.appName,
                                        title = notif.title,
                                        text = notif.text,
                                        time = MalaysianEnglish.clockTime(notif.timestamp, settings.is24HourFormat),
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            NotificationRepository.openNotification(context, notif)
                                        },
                                        onDismiss = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            group.items.forEach { NotificationRepository.dismissNotification(it.key) }
                                        }
                                    )
                                }
                                MoreLine(hidden = alertGroups.size - lines)

                                // Clear All Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = my.hana.meena.theme.MeenaSpacing.s),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = stringResource(R.string.action_clear_all),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MeenaTextMuted,
                                        modifier = Modifier
                                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                            .metroClickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                NotificationRepository.clearAll()
                                            }
                                            .padding(vertical = my.hana.meena.theme.MeenaSpacing.s, horizontal = my.hana.meena.theme.MeenaSpacing.s)
                                    )
                                }
                            } else {
                                QuietLine()
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
private fun QuietLine() {
    Text(
        text = stringResource(R.string.state_all_quiet),
        style = MaterialTheme.typography.bodySmall,
        color = MeenaTextMuted,
        modifier = Modifier.padding(vertical = my.hana.meena.theme.MeenaSpacing.s)
    )
}

/** "2 more" under a shortened list; nothing when everything fits. */
@Composable
private fun MoreLine(hidden: Int) {
    if (hidden <= 0) return
    Text(
        text = stringResource(R.string.inbox_more, hidden),
        style = MaterialTheme.typography.labelSmall,
        color = MeenaTextMuted,
        modifier = Modifier.padding(vertical = my.hana.meena.theme.MeenaSpacing.s)
    )
}

@Composable
private fun AccessPrompt(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MeenaTextMuted,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .metroClickable(onClick = onClick)
            .padding(vertical = my.hana.meena.theme.MeenaSpacing.s)
    )
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
            .padding(vertical = my.hana.meena.theme.MeenaSpacing.s),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.m)
    ) {
        Box(
            modifier = Modifier
                .padding(top = my.hana.meena.theme.MeenaSpacing.s)
                .size(my.hana.meena.theme.MeenaSpacing.s)
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
                fontWeight = FontWeight.Normal,
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
            Icon(
                painter = painterResource(id = my.hana.meena.R.drawable.ic_thin_close),
                contentDescription = stringResource(my.hana.meena.R.string.inbox_dismiss),
                tint = MeenaTextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

