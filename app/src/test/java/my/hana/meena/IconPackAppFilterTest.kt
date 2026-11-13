package my.hana.meena

import my.hana.meena.data.system.IconPackAppFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IconPackAppFilterTest {

    @Test
    fun parseComponent_readsComponentInfoWrapper() {
        assertEquals(
            "com.android.chrome/com.google.android.apps.chrome.Main",
            IconPackAppFilter.parseComponent("ComponentInfo{com.android.chrome/com.google.android.apps.chrome.Main}")
        )
    }

    @Test
    fun parseComponent_acceptsBareComponentAndTrimsSpaces() {
        assertEquals("com.foo/com.foo.Main", IconPackAppFilter.parseComponent("  com.foo/com.foo.Main "))
        assertEquals("com.foo/com.foo.Main", IconPackAppFilter.parseComponent("ComponentInfo{ com.foo/com.foo.Main }"))
    }

    @Test
    fun parseComponent_expandsShortClassName() {
        assertEquals("com.foo/com.foo.ui.Main", IconPackAppFilter.parseComponent("ComponentInfo{com.foo/.ui.Main}"))
    }

    @Test
    fun parseComponent_rejectsMalformed() {
        listOf(
            null,
            "",
            "ComponentInfo{}",
            "ComponentInfo{com.foo}",
            "ComponentInfo{/com.foo.Main}",
            "ComponentInfo{com.foo/}",
            "ComponentInfo{com.foo/com.foo.Main",
            "ComponentInfo{:LAUNCHER_ACTION_APP_DRAWER}",
            "ComponentInfo{com.foo/com.foo/Main}",
            "ComponentInfo{com foo/com.foo.Main}"
        ).forEach { raw ->
            assertNull("expected null for '$raw'", IconPackAppFilter.parseComponent(raw))
        }
    }

    @Test
    fun buildMap_mapsComponentsToDrawables_andSkipsBadEntries() {
        val map = IconPackAppFilter.buildMap(
            listOf(
                "ComponentInfo{com.android.chrome/com.google.android.apps.chrome.Main}" to "chrome",
                "ComponentInfo{com.whatsapp/com.whatsapp.Main}" to " whatsapp ",
                "ComponentInfo{com.broken}" to "broken",
                "ComponentInfo{com.blank/com.blank.Main}" to "",
                "ComponentInfo{com.nodrawable/com.nodrawable.Main}" to null,
                null to "orphan"
            )
        )
        assertEquals(
            mapOf(
                "com.android.chrome/com.google.android.apps.chrome.Main" to "chrome",
                "com.whatsapp/com.whatsapp.Main" to "whatsapp"
            ),
            map
        )
    }

    @Test
    fun buildMap_laterEntryWins() {
        val map = IconPackAppFilter.buildMap(
            listOf(
                "ComponentInfo{com.foo/com.foo.Main}" to "foo_old",
                "ComponentInfo{com.foo/.Main}" to "foo_new"
            )
        )
        assertEquals(mapOf("com.foo/com.foo.Main" to "foo_new"), map)
    }

    @Test
    fun buildMap_keysMatchComponentKey() {
        val map = IconPackAppFilter.buildMap(listOf("ComponentInfo{com.foo/com.foo.Main}" to "foo"))
        assertEquals("foo", map[IconPackAppFilter.componentKey("com.foo", "com.foo.Main")])
        assertTrue(IconPackAppFilter.buildMap(emptyList()).isEmpty())
    }
}
