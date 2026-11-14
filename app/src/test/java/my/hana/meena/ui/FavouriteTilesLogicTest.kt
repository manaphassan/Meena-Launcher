package my.hana.meena.ui

import my.hana.meena.data.system.DeviceAppInfo
import my.hana.meena.data.system.FavouriteRules
import my.hana.meena.data.system.LiveNotificationItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FavouriteTilesLogicTest {

    @Test
    fun `pinned apps cap at 6 and maintain order`() {
        val pinnedAppIds = listOf(
            "app1", "app2", "app3", "app4", "app5", "app6", "app7", "app8", "app9"
        )
        val installedApps = (1..10).map {
            DeviceAppInfo(
                packageName = "app$it",
                label = "App $it",
                firstLetter = 'a',
                version = "1.0",
                iconBitmap = null
            )
        }

        val result = getPinnedApps(pinnedAppIds, installedApps)

        assertEquals(6, result.size)
        assertEquals("app1", result[0].packageName)
        assertEquals("app6", result[5].packageName)
    }

    @Test
    fun `toggle refuses a seventh pin but always unpins`() {
        val six = (1..6).map { "app$it" }
        assertNull(FavouriteRules.toggle(six, "app7"))
        assertEquals(six - "app3", FavouriteRules.toggle(six, "app3"))
        assertEquals(listOf("app1", "app2"), FavouriteRules.toggle(listOf("app1"), "app2"))
    }

    @Test
    fun `pins for removed apps do not count towards the limit`() {
        val six = (1..6).map { "app$it" }
        val result = FavouriteRules.toggle(six, "app7") { it != "app2" }
        assertEquals(six + "app7", result)
    }

    @Test
    fun `notification counts group by package name`() {
        val notifs = listOf(
            LiveNotificationItem(key = "1", packageName = "app1", appName = "App 1", title = "A", text = "A", timestamp = 0, formattedTime = "0:00"),
            LiveNotificationItem(key = "2", packageName = "app1", appName = "App 1", title = "B", text = "B", timestamp = 0, formattedTime = "0:00"),
            LiveNotificationItem(key = "3", packageName = "app2", appName = "App 2", title = "C", text = "C", timestamp = 0, formattedTime = "0:00")
        )

        val result = getNotificationCounts(notifs)

        assertEquals(2, result["app1"])
        assertEquals(1, result["app2"])
        assertEquals(null, result["app3"])
    }

    private fun getPinnedApps(
        pinnedAppIds: List<String>,
        installedApps: List<DeviceAppInfo>
    ): List<DeviceAppInfo> =
        FavouriteRules.visible(pinnedAppIds) { pkg -> installedApps.find { it.packageName == pkg } }

    private fun getNotificationCounts(notifications: List<LiveNotificationItem>): Map<String, Int> {
        return notifications.groupingBy { it.packageName }.eachCount()
    }
}
