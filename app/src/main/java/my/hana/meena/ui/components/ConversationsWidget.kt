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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusRequester
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
    modifier: Modifier = Modifier,
    is24Hour: Boolean = true,
    maxItems: Int = 5
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
            } else if (conversations.isEmpty()) {
                Text(
                    text = stringResource(R.string.state_all_quiet),
                    style = MaterialTheme.typography.bodySmall,
                    color = MeenaTextMuted,
                    modifier = Modifier.padding(vertical = my.hana.meena.theme.MeenaSpacing.s)
                )
            } else {
                conversations.take(maxItems).forEach { item ->
                    ConversationRow(
                        item = item,
                        is24Hour = is24Hour,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            NotificationRepository.openConversation(context, item)
                        },
                        onReply = if (item.replyAction != null) { text ->
                            NotificationRepository.sendReply(context, item, text)
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    item: LiveConversationItem,
    is24Hour: Boolean,
    onClick: () -> Unit,
    onReply: ((String) -> Unit)? = null
) {
    val appColor = MeenaTextWhite
    var showReplyField by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var replySent by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .metroClickable(onClick = onClick)
            .sensitive()
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
                    text = my.hana.meena.data.system.MalaysianEnglish.clockTime(item.timestamp, is24Hour),
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

        // Photo sneak peek
        if (item.picture != null) {
            androidx.compose.foundation.Image(
                bitmap = item.picture,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .padding(start = my.hana.meena.theme.MeenaSpacing.m, top = my.hana.meena.theme.MeenaSpacing.s)
                    .fillMaxWidth()
                    .heightIn(max = 120.dp)
            )
        }

        // Reply link
        if (onReply != null && !showReplyField) {
            Text(
                text = if (replySent) stringResource(my.hana.meena.R.string.inbox_reply_sent)
                       else stringResource(my.hana.meena.R.string.inbox_reply),
                style = MaterialTheme.typography.labelSmall,
                color = if (replySent) MeenaProfitGreen else androidx.compose.material3.MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .metroClickable {
                        replySent = false
                        showReplyField = true
                    }
                    .padding(start = my.hana.meena.theme.MeenaSpacing.m, top = my.hana.meena.theme.MeenaSpacing.s)
            )
        }

        // Inline reply field
        if (onReply != null && showReplyField) {
            val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
            val underline = MaterialTheme.colorScheme.primary
            androidx.compose.runtime.LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
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
                            drawLine(underline, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1.dp.toPx())
                        }
                        .padding(vertical = my.hana.meena.theme.MeenaSpacing.m),
                    decorationBox = { field ->
                        if (replyText.isEmpty()) {
                            Text(
                                stringResource(my.hana.meena.R.string.inbox_reply_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MeenaTextMuted
                            )
                        }
                        field()
                    }
                )
                Text(
                    text = stringResource(my.hana.meena.R.string.inbox_reply),
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

