package my.hana.meena.ui

import my.hana.meena.data.system.DeviceAppInfo
import my.hana.meena.data.system.FavouriteRules
import my.hana.meena.data.system.LiveNotificationItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `a group is one slot and holds up to four apps`() {
        val grouped = FavouriteRules.makeGroup(listOf("mae", "grab", "notes"), 0)
        assertTrue(FavouriteRules.isGroup(grouped[0]))
        var list = FavouriteRules.addToGroup(grouped, 0, "tng")!!
        // grab was a tile of its own; it moves into the group instead of being in two places.
        list = FavouriteRules.addToGroup(list, 0, "grab")!!
        list = FavouriteRules.addToGroup(list, 0, "wallet")!!
        assertEquals(listOf("mae", "tng", "grab", "wallet"), FavouriteRules.members(list[0]))
        assertEquals(listOf(list[0], "notes"), list)
        assertNull(FavouriteRules.addToGroup(list, 0, "fifth"))
        assertTrue(FavouriteRules.contains(list, "wallet"))
        assertEquals(listOf("mae", "tng", "grab", "wallet", "notes"), FavouriteRules.allPackages(list))
    }

    @Test
    fun `taking apps out of a group shrinks it back to a tile`() {
        val group = FavouriteRules.groupId(listOf("a", "b"))
        assertEquals(listOf("b", "c"), FavouriteRules.remove(listOf(group, "c"), "a"))
        // Unpinning from the apps page works for grouped apps too.
        assertEquals(listOf("a"), FavouriteRules.toggle(listOf(group), "b"))
    }

    @Test
    fun `visible shows groups with any app still installed`() {
        val group = FavouriteRules.groupId(listOf("gone", "here"))
        val slots = FavouriteRules.visible(listOf(group, "gone", "x")) { pkg -> pkg.takeIf { it != "gone" } }
        assertEquals(2, slots.size)
        assertEquals(listOf("here"), (slots[0] as my.hana.meena.data.system.FavouriteSlot.Group).apps)
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
            .map { (it as my.hana.meena.data.system.FavouriteSlot.App).app }

    private fun getNotificationCounts(notifications: List<LiveNotificationItem>): Map<String, Int> {
        return notifications.groupingBy { it.packageName }.eachCount()
    }
}
