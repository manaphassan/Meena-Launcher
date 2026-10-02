package com.example.meenalauncher.data.system

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.meenalauncher.service.MeenaNotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data model for an active real-time Android status bar notification.
 */
data class LiveNotificationItem(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val formattedTime: String,
    val isConversation: Boolean = false,
    val contentIntent: PendingIntent? = null
)

/**
 * Data model for a real-time messaging conversation / chat thread (e.g. WhatsApp, Telegram, SMS).
 */
data class LiveConversationItem(
    val key: String,
    val packageName: String,
    val appName: String,
    val senderOrContact: String,
    val conversationTitle: String? = null,
    val messageSnippet: String,
    val timestamp: Long,
    val formattedTime: String,
    val unreadCount: Int = 1,
    val contentIntent: PendingIntent? = null
)

object NotificationRepository {
    private const val TAG = "NotificationRepo"
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    // Known messaging & chat apps package names
    private val MESSAGING_PACKAGES = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
        "org.telegram.plus",
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.samsung.android.messaging",
        "com.facebook.orca",
        "com.facebook.mlite",
        "org.thoughtcrime.securesms",
        "com.discord",
        "com.Slack",
        "com.viber.voip",
        "com.skype.raider",
        "jp.naver.line.android",
        "com.tencent.mm",
        "com.instagram.android"
    )

    private val _notificationsFlow = MutableStateFlow<List<LiveNotificationItem>>(emptyList())
    val notificationsFlow: StateFlow<List<LiveNotificationItem>> = _notificationsFlow.asStateFlow()

    private val _conversationsFlow = MutableStateFlow<List<LiveConversationItem>>(emptyList())
    val conversationsFlow: StateFlow<List<LiveConversationItem>> = _conversationsFlow.asStateFlow()

    private val _isListenerConnectedFlow = MutableStateFlow(false)
    val isListenerConnectedFlow: StateFlow<Boolean> = _isListenerConnectedFlow.asStateFlow()

    fun setListenerConnected(connected: Boolean) {
        _isListenerConnectedFlow.value = connected
    }

    /**
     * Checks if the user has granted Notification Listener Permission in Android Settings.
     */
    fun isNotificationAccessGranted(context: Context): Boolean {
        return try {
            val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
            enabledPackages.contains(context.packageName)
        } catch (e: Exception) {
            // Fallback for custom ROMs
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            !TextUtils.isEmpty(flat) && flat.contains(context.packageName)
        }
    }

    /**
     * Launches the system settings page where user can grant Notification Listener access.
     */
    fun openNotificationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch notification settings", e)
        }
    }

    /**
     * Called when MeenaNotificationListenerService connects.
     * Synchronizes all currently active notifications on the device.
     */
    fun syncActiveNotifications(sbns: Array<StatusBarNotification>?, context: Context) {
        if (sbns == null || sbns.isEmpty()) {
            _notificationsFlow.value = emptyList()
            _conversationsFlow.value = emptyList()
            return
        }

        val notifList = mutableListOf<LiveNotificationItem>()
        val convList = mutableListOf<LiveConversationItem>()

        for (sbn in sbns) {
            val parsed = parseNotification(sbn, context) ?: continue
            notifList.add(parsed)

            val conv = parseConversation(sbn, parsed)
            if (conv != null) {
                convList.add(conv)
            }
        }

        // Deduplicate conversations and sort descending by timestamp
        val dedupedConversations = convList
            .groupBy { "${it.packageName}::${it.senderOrContact}" }
            .map { (_, items) ->
                val latest = items.maxByOrNull { it.timestamp } ?: items.first()
                latest.copy(unreadCount = items.size)
            }
            .sortedByDescending { it.timestamp }
            .take(6)

        _notificationsFlow.value = notifList.sortedByDescending { it.timestamp }.take(20)
        _conversationsFlow.value = dedupedConversations
        Log.d(TAG, "Synced active notifications: ${notifList.size} notifs, ${dedupedConversations.size} conversations")
    }

    /**
     * Called whenever a new notification is posted or updated by Android.
     */
    fun onNotificationPosted(sbn: StatusBarNotification, context: Context) {
        val parsed = parseNotification(sbn, context) ?: return

        // 1. Update Notifications Flow
        val currentNotifs = _notificationsFlow.value.toMutableList()
        currentNotifs.removeAll { it.key == parsed.key }
        currentNotifs.add(0, parsed)
        _notificationsFlow.value = currentNotifs.sortedByDescending { it.timestamp }.take(20)

        // 2. Update Conversations Flow if it's a message
        val conv = parseConversation(sbn, parsed)
        if (conv != null) {
            val currentConvs = _conversationsFlow.value.toMutableList()
            // Deduplicate by package and sender/chat title
            currentConvs.removeAll { it.packageName == conv.packageName && it.senderOrContact == conv.senderOrContact }
            currentConvs.add(0, conv)
            _conversationsFlow.value = currentConvs.sortedByDescending { it.timestamp }.take(6)
        }
    }

    /**
     * Called whenever a notification is dismissed / removed.
     */
    fun onNotificationRemoved(sbn: StatusBarNotification) {
        val key = sbn.key
        _notificationsFlow.value = _notificationsFlow.value.filterNot { it.key == key }
        _conversationsFlow.value = _conversationsFlow.value.filterNot { it.key == key }
    }

    /**
     * Parse raw StatusBarNotification into clean LiveNotificationItem
     */
    private fun parseNotification(sbn: StatusBarNotification, context: Context): LiveNotificationItem? {
        val notif = sbn.notification ?: return null
        val extras = notif.extras ?: return null
        val packageName = sbn.packageName ?: return null

        // Ignore Meena's own notifications and ongoing system persistent services
        if (packageName == context.packageName) return null
        if ((notif.flags and Notification.FLAG_ONGOING_EVENT) != 0 && notif.category != Notification.CATEGORY_CALL) {
            // Filter ongoing system background persistent notifications unless it's an incoming call
            return null
        }

        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val rawText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val rawBigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val rawConvTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()

        val title = (rawConvTitle ?: rawTitle ?: "").trim()
        val text = (if (!rawText.isNullOrBlank()) rawText else rawBigText ?: "").trim()

        if (title.isBlank() && text.isBlank()) return null

        val pm = context.packageManager
        val appLabel = try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }

        val isMsg = notif.category == Notification.CATEGORY_MESSAGE ||
                notif.category == Notification.CATEGORY_SOCIAL ||
                packageName in MESSAGING_PACKAGES ||
                !rawConvTitle.isNullOrBlank()

        val postTime = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis()

        return LiveNotificationItem(
            key = sbn.key,
            packageName = packageName,
            appName = appLabel,
            title = title.ifBlank { appLabel },
            text = text,
            timestamp = postTime,
            formattedTime = timeFormat.format(Date(postTime)),
            isConversation = isMsg,
            contentIntent = notif.contentIntent
        )
    }

    /**
     * Extracts conversation thread details from a notification.
     */
    private fun parseConversation(sbn: StatusBarNotification, parsedNotif: LiveNotificationItem): LiveConversationItem? {
        if (!parsedNotif.isConversation) return null

        val notif = sbn.notification ?: return null
        val extras = notif.extras ?: return null
        val convTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()

        val senderOrContact = when {
            !convTitle.isNullOrBlank() -> convTitle
            !title.isNullOrBlank() -> title
            else -> parsedNotif.appName
        }

        return LiveConversationItem(
            key = sbn.key,
            packageName = parsedNotif.packageName,
            appName = parsedNotif.appName,
            senderOrContact = senderOrContact,
            conversationTitle = convTitle,
            messageSnippet = parsedNotif.text.ifBlank { "Sent a message" },
            timestamp = parsedNotif.timestamp,
            formattedTime = parsedNotif.formattedTime,
            unreadCount = 1,
            contentIntent = parsedNotif.contentIntent
        )
    }

    /**
     * Dismiss a specific notification from launcher and status bar.
     */
    fun dismissNotification(key: String) {
        try {
            MeenaNotificationListenerService.instance?.cancelNotification(key)
        } catch (e: Exception) {
            Log.w(TAG, "Could not cancel notification via service: $key")
        }
        _notificationsFlow.value = _notificationsFlow.value.filterNot { it.key == key }
        _conversationsFlow.value = _conversationsFlow.value.filterNot { it.key == key }
    }

    /**
     * Clear all notifications in Meena Launcher and cancel in status bar.
     */
    fun clearAll() {
        try {
            MeenaNotificationListenerService.instance?.cancelAllNotifications()
        } catch (e: Exception) {
            Log.w(TAG, "Could not cancel all notifications via service")
        }
        _notificationsFlow.value = emptyList()
        _conversationsFlow.value = emptyList()
    }

    /**
     * Helper to open the clicked notification via PendingIntent or package manager.
     */
    fun openNotification(context: Context, item: LiveNotificationItem) {
        if (item.contentIntent != null) {
            try {
                item.contentIntent.send()
                return
            } catch (e: Exception) {
                Log.w(TAG, "PendingIntent failed, falling back to launch intent", e)
            }
        }
        launchApp(context, item.packageName)
    }

    /**
     * Helper to open the clicked conversation via PendingIntent or package manager.
     */
    fun openConversation(context: Context, item: LiveConversationItem) {
        if (item.contentIntent != null) {
            try {
                item.contentIntent.send()
                return
            } catch (e: Exception) {
                Log.w(TAG, "PendingIntent failed, falling back to launch intent", e)
            }
        }
        launchApp(context, item.packageName)
    }

    private fun launchApp(context: Context, packageName: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package: $packageName", e)
        }
    }
}
