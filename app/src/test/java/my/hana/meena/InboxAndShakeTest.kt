package my.hana.meena

import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.data.model.WidgetConfig
import my.hana.meena.data.system.InboxSort
import my.hana.meena.data.system.LiveNotificationItem
import my.hana.meena.data.system.ShakeDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Widget length setting, inbox sections and shake to hide (15 November 2026). */
class InboxAndShakeTest {

    // Widget length

    @Test
    fun widgetLines_shortIsThree_longIsFive() {
        assertEquals(3, MeenaUserSettings().widgetLines)
        assertEquals(5, MeenaUserSettings(shortWidgets = false).widgetLines)
    }

    // Inbox sections

    private fun n(key: String, pkg: String, at: Long, chat: Boolean = false, email: Boolean = false) =
        LiveNotificationItem(
            key = key, packageName = pkg, appName = pkg.substringAfterLast('.'), title = "t$key", text = "x",
            timestamp = at, formattedTime = "", isConversation = chat, isEmail = email
        )

    private val all = listOf(
        n("1", "com.google.android.gm", 50, email = true),
        n("2", "com.whatsapp", 40, chat = true),
        n("3", "com.shopee.my", 30),
        n("4", "com.shopee.my", 60),
        n("5", "com.google.android.gm", 70, email = true),
        n("6", "com.grabtaxi.passenger", 20)
    )

    @Test
    fun email_isNewestFirst() {
        assertEquals(listOf("5", "1"), InboxSort.email(all).map { it.key })
    }

    @Test
    fun alerts_leaveOutWhatHasItsOwnSection() {
        assertEquals(listOf("3", "4", "6"), InboxSort.alerts(all, chatsOn = true, emailOn = true).map { it.key })
        assertEquals(6, InboxSort.alerts(all, chatsOn = false, emailOn = false).size)
    }

    @Test
    fun alerts_groupByApp_newestAppFirst() {
        val groups = InboxSort.byApp(InboxSort.alerts(all, chatsOn = true, emailOn = true))
        assertEquals(listOf("com.shopee.my", "com.grabtaxi.passenger"), groups.map { it.packageName })
        assertEquals(listOf("4", "3"), groups.first().items.map { it.key })
        assertEquals("4", groups.first().latest.key)
    }

    @Test
    fun migration_putsEmailBetweenChatsAndAlerts() {
        val old = MeenaUserSettings(
            widgetConfigs = MeenaUserSettings.defaultWidgetMap() - "widget-email" +
                ("widget-conversations" to WidgetConfig("inbox", 0)) +
                ("widget-notifications" to WidgetConfig("inbox", 1)),
            layoutRevision = MeenaUserSettings.FAVOURITES_FIRST_REVISION
        )
        val inbox = old.migrate().widgetConfigs.filterValues { it.hubId == "inbox" }
            .entries.sortedBy { it.value.position }.map { it.key }
            .filter { it != "widget-music-player" }
        assertEquals(listOf("widget-conversations", "widget-email", "widget-notifications"), inbox)
    }

    // Shake to hide

    private fun detector(): Pair<ShakeDetector, IntArray> {
        val count = IntArray(1)
        return ShakeDetector { count[0]++ } to count
    }

    private val jolt = 3.2f * ShakeDetector.GRAVITY

    @Test
    fun shake_twoJoltsCountOnce() {
        val (d, count) = detector()
        d.onReading(jolt, 0f, 0f, 1_000)
        d.onReading(0f, jolt, 0f, 1_200)
        assertEquals(1, count[0])
        // Still shaking inside the cooldown: no second toggle.
        d.onReading(jolt, 0f, 0f, 1_400)
        d.onReading(jolt, 0f, 0f, 1_600)
        assertEquals(1, count[0])
    }

    @Test
    fun shake_ignoresOneBumpAndNormalMovement() {
        val (d, count) = detector()
        d.onReading(jolt, 0f, 0f, 1_000)           // one bump
        d.onReading(jolt, 0f, 0f, 2_000)           // another, too late to pair
        d.onReading(0f, 0f, 1.8f * ShakeDetector.GRAVITY, 2_100) // walking-strength
        assertEquals(0, count[0])
    }

    @Test
    fun shake_worksAgainAfterCooldown() {
        val (d, count) = detector()
        d.onReading(jolt, 0f, 0f, 1_000)
        d.onReading(jolt, 0f, 0f, 1_200)
        d.onReading(jolt, 0f, 0f, 3_000)
        d.onReading(jolt, 0f, 0f, 3_250)
        assertEquals(2, count[0])
        assertTrue(ShakeDetector.THRESHOLD_G > 2f)
    }
}
