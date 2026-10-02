package com.example.meenalauncher.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.meenalauncher.data.system.NotificationRepository
import com.example.meenalauncher.data.system.SpendingRepository

class MeenaNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "MeenaNotifListener"
        var instance: MeenaNotificationListenerService? = null
            private set
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        NotificationRepository.setListenerConnected(true)
        Log.d(TAG, "Notification listener connected! Syncing active notifications...")

        try {
            val active = activeNotifications ?: emptyArray()
            NotificationRepository.syncActiveNotifications(active, applicationContext)
        } catch (e: Exception) {
            Log.e(TAG, "Error querying active notifications upon connection", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance == this) {
            instance = null
        }
        NotificationRepository.setListenerConnected(false)
        Log.d(TAG, "Notification listener disconnected.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        try {
            // Forward to real-time notification & conversation repository
            NotificationRepository.onNotificationPosted(sbn, applicationContext)

            // Forward to spending repository for financial / bank transaction detection
            val extras = sbn.notification?.extras
            if (extras != null) {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error handling onNotificationPosted", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return

        try {
            NotificationRepository.onNotificationRemoved(sbn)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling onNotificationRemoved", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        NotificationRepository.setListenerConnected(false)
    }
}
