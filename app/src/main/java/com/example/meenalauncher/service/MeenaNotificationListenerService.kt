package com.example.meenalauncher.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.meenalauncher.data.system.SpendingRepository

class MeenaNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val extras = sbn.notification?.extras ?: return
        val packageName = sbn.packageName ?: ""
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        if (title.isNotBlank() || text.isNotBlank()) {
            SpendingRepository.parseAndRecordFromNotification(
                packageName = packageName,
                title = title,
                text = text
            )
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
