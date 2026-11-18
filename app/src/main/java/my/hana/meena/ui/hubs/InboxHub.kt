package my.hana.meena.ui.hubs

import my.hana.meena.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
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
    listState: LazyListState,
    onOpenNowPlaying: () -> Unit = {}
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
    val chatMax = (settings.widgetConfigs["widget-conversations"]?.maxLinesOrItems ?: lines).coerceIn(1, 12)
    val emailMax = (settings.widgetConfigs["widget-email"]?.maxLinesOrItems ?: lines).coerceIn(1, 12)
    val alertMax = (settings.widgetConfigs["widget-notifications"]?.maxLinesOrItems ?: lines).coerceIn(1, 12)
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
                "widget-music-player" -> {
                    val nowPlaying by my.hana.meena.data.system.MediaPlayerRepository.nowPlaying.collectAsState()
                    val track = nowPlaying
                    if (track != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = my.hana.meena.theme.MeenaSpacing.s)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .metroClickable(
                                        onClickLabel = stringResource(R.string.now_playing_show),
                                        onClick = onOpenNowPlaying
                                    )
                            ) {
                            Text(
                                text = track.title.lowercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MeenaTextWhite,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            if (track.artist.isNotBlank()) {
                                Text(
                                    text = track.artist.lowercase(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MeenaTextMuted,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = my.hana.meena.theme.MeenaSpacing.s),
                                horizontalArrangement = Arrangement.spacedBy(my.hana.meena.theme.MeenaSpacing.l),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_thin_skip_prev),
                                    contentDescription = stringResource(R.string.now_playing_previous),
                                    tint = MeenaTextWhite,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .metroClickable { my.hana.meena.data.system.MediaPlayerRepository.previous() }
                                        .padding(my.hana.meena.theme.MeenaSpacing.m)
                                )
                                val playPauseLabel = stringResource(if (track.isPlaying) R.string.now_playing_pause else R.string.now_playing_play)
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .metroClickable(onClickLabel = playPauseLabel) { my.hana.meena.data.system.MediaPlayerRepository.playPause() }
                                        .semantics { contentDescription = playPauseLabel }
                                ) {
                                    my.hana.meena.ui.components.AnimatedPlayPause(
                                        isPlaying = track.isPlaying,
                                        color = MeenaTextWhite,
                                        size = 24.dp,
                                        animate = !settings.reducedMotion
                                    )
                                }
                                Icon(
                                    painter = painterResource(R.drawable.ic_thin_skip_next),
                                    contentDescription = stringResource(R.string.now_playing_next),
                                    tint = MeenaTextWhite,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .metroClickable { my.hana.meena.data.system.MediaPlayerRepository.next() }
                                        .padding(my.hana.meena.theme.MeenaSpacing.m)
                                )
                            }
                        }
                    }
                }

                "widget-conversations" -> {
                    // Chats and messaging threads
                    ConversationsWidget(is24Hour = settings.is24HourFormat, maxItems = chatMax)
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
                                    emails.take(emailMax).forEachIndexed { idx, mail ->
                                        InboxNotificationRow(
                                            modifier = Modifier
                                                .metroFeatherIn(index = idx, reducedMotion = settings.reducedMotion)
                                                .sensitive(),
                                            badgeColor = MaterialTheme.colorScheme.primary,
                                            app = mail.appName,
                                            title = mail.title,
                                            text = mail.text,
                                            time = MalaysianEnglish.clockTime(mail.timestamp, settings.is24HourFormat),
                                            picture = mail.picture,
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
                                    MoreLine(hidden = emails.size - emailMax)
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
                                var expandedApps by remember { mutableStateOf(emptySet<String>()) }
                                alertGroups.take(alertMax).forEachIndexed { idx, group ->
                                    val isExpanded = group.packageName in expandedApps
                                    val items = if (isExpanded) group.items else listOf(group.latest)
                                    // App header with count - tappable to expand/collapse if multiple
                                    if (group.items.size > 1) {
                                        Text(
                                            text = stringResource(R.string.inbox_app_count, group.appName, group.items.size),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isExpanded) MaterialTheme.colorScheme.primary else MeenaTextMuted,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .metroClickable {
                                                    expandedApps = if (isExpanded) expandedApps - group.packageName
                                                    else expandedApps + group.packageName
                                                }
                                                .padding(vertical = my.hana.meena.theme.MeenaSpacing.s)
                                        )
                                    }
                                    items.forEachIndexed { itemIdx, notif ->
                                        InboxNotificationRow(
                                            modifier = Modifier
                                                .metroFeatherIn(index = idx + itemIdx, reducedMotion = settings.reducedMotion)
                                                .sensitive(),
                                            badgeColor = MeenaTextWhite,
                                            app = if (group.items.size == 1) group.appName else "",
                                            title = notif.title,
                                            text = notif.text,
                                            time = MalaysianEnglish.clockTime(notif.timestamp, settings.is24HourFormat),
                                            picture = notif.picture,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                NotificationRepository.openNotification(context, notif)
                                            },
                                            onDismiss = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                NotificationRepository.dismissNotification(notif.key)
                                            },
                                            onReply = if (notif.replyAction != null) { text ->
                                                NotificationRepository.sendReply(context, notif, text)
                                            } else null
                                        )
                                    }
                                }
                                MoreLine(hidden = alertGroups.size - alertMax)

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
    picture: androidx.compose.ui.graphics.ImageBitmap? = null,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    onReply: ((String) -> Unit)? = null
) {
    var showReplyField by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var replySent by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
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
                // Photo sneak peek
                if (picture != null) {
                    androidx.compose.foundation.Image(
                        bitmap = picture,
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .padding(top = my.hana.meena.theme.MeenaSpacing.s)
                            .fillMaxWidth()
                            .heightIn(max = 120.dp)
                    )
                }
                // Reply link and sent confirmation
                if (onReply != null && !showReplyField) {
                    Text(
                        text = if (replySent) stringResource(R.string.inbox_reply_sent)
                               else stringResource(R.string.inbox_reply),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (replySent) MeenaProfitGreen else MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .heightIn(min = 32.dp)
                            .metroClickable {
                                replySent = false
                                showReplyField = true
                            }
                            .padding(top = my.hana.meena.theme.MeenaSpacing.s)
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
                    painter = painterResource(id = R.drawable.ic_thin_close),
                    contentDescription = stringResource(R.string.inbox_dismiss),
                    tint = MeenaTextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Inline reply field
        if (onReply != null && showReplyField) {
            val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
            val underline = MaterialTheme.colorScheme.primary
            LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = my.hana.meena.theme.MeenaSpacing.m),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = replyText,
                    onValueChange = { replyText = it.take(500) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MeenaTextWhite),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(underline),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Send
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSend = {
                            if (replyText.isNotBlank()) {
                                onReply(replyText.trim())
                                replyText = ""
                                showReplyField = false
                                replySent = true
                            }
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .focusRequester(focusRequester)
                        .drawBehind {
                            val y = size.height - 1.dp.toPx()
                            drawLine(underline, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                        }
                        .padding(vertical = my.hana.meena.theme.MeenaSpacing.m),
                    decorationBox = { field ->
                        if (replyText.isEmpty()) {
                            Text(
                                stringResource(R.string.inbox_reply_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MeenaTextMuted
                            )
                        }
                        field()
                    }
                )
                Text(
                    text = stringResource(R.string.inbox_reply),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (replyText.isBlank()) MeenaTextMuted else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .metroClickable {
                            if (replyText.isNotBlank()) {
                                onReply(replyText.trim())
                                replyText = ""
                                showReplyField = false
                                replySent = true
                            }
                        }
                        .padding(
                            start = my.hana.meena.theme.MeenaSpacing.m,
                            top = my.hana.meena.theme.MeenaSpacing.m,
                            bottom = my.hana.meena.theme.MeenaSpacing.m
                        )
                )
            }
        }
    }
}

