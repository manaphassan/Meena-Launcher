package my.hana.meena.data.system

/** One app's alerts, newest first, for the inbox's grouped alerts list. */
data class AlertGroup(
    val packageName: String,
    val appName: String,
    val items: List<LiveNotificationItem>
) {
    val latest: LiveNotificationItem get() = items.first()
}

/** How the inbox sorts notifications into chats, email and alerts. Free of Android so it can be unit-tested. */
object InboxSort {

    /** Email only, newest first. */
    fun email(all: List<LiveNotificationItem>): List<LiveNotificationItem> =
        all.filter { it.isEmail }.sortedByDescending { it.timestamp }

    /**
     * Everything that isn't shown elsewhere: chats live in the chats section when it's on, and email
     * in the email section when it's on, so nothing shows twice.
     */
    fun alerts(all: List<LiveNotificationItem>, chatsOn: Boolean, emailOn: Boolean): List<LiveNotificationItem> =
        all.filterNot { (chatsOn && it.isConversation) || (emailOn && it.isEmail) }

    /** Alerts grouped by app, the app with the newest alert first. */
    fun byApp(alerts: List<LiveNotificationItem>): List<AlertGroup> =
        alerts.groupBy { it.packageName }
            .map { (pkg, items) ->
                val sorted = items.sortedByDescending { it.timestamp }
                AlertGroup(pkg, sorted.first().appName, sorted)
            }
            .sortedByDescending { it.latest.timestamp }
}
