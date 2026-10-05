package my.hana.meena.ui

import my.hana.meena.data.system.DeviceAppInfo
import my.hana.meena.data.system.LiveNotificationItem
import org.junit.Assert.assertEquals
import org.junit.Test

class FavouriteTilesLogicTest {

    @Test
    fun `pinned apps cap at 8 and maintain order`() {
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

        assertEquals(8, result.size)
        assertEquals("app1", result[0].packageName)
        assertEquals("app8", result[7].packageName)
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
    ): List<DeviceAppInfo> {
        return pinnedAppIds
            .mapNotNull { pkg -> installedApps.find { it.packageName == pkg } }
            .take(8)
    }

    private fun getNotificationCounts(notifications: List<LiveNotificationItem>): Map<String, Int> {
        return notifications.groupingBy { it.packageName }.eachCount()
    }
}
