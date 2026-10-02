package com.example.meenalauncher.data.system

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LiveNotificationItem(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val formattedTime: String
)

object NotificationRepository {
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
    private val _notificationsFlow = MutableStateFlow<List<LiveNotificationItem>>(emptyList())
    val notificationsFlow: StateFlow<List<LiveNotificationItem>> = _notificationsFlow.asStateFlow()

    fun addNotification(
        packageName: String,
        appName: String,
        title: String,
        text: String
    ) {
        if (title.isBlank() && text.isBlank()) return
        val now = System.currentTimeMillis()
        val item = LiveNotificationItem(
            id = "notif-$now-${(0..999).random()}",
            packageName = packageName,
            appName = appName,
            title = title.ifBlank { appName },
            text = text,
            timestamp = now,
            formattedTime = timeFormat.format(Date(now))
        )
        val current = _notificationsFlow.value.toMutableList()
        // Deduplicate recent notification with identical title and text from same package
        current.removeAll { it.packageName == packageName && it.title == item.title && it.text == item.text }
        current.add(0, item)
        // Keep latest 20 notifications
        _notificationsFlow.value = current.take(20)
    }

    fun removeNotification(id: String) {
        _notificationsFlow.value = _notificationsFlow.value.filterNot { it.id == id }
    }

    fun clearAll() {
        _notificationsFlow.value = emptyList()
    }
}
