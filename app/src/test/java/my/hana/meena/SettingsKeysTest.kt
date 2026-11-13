package my.hana.meena

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every per-widget setting saved with custom("key", ...) in SettingsScreen.kt must be read
 * somewhere else in the app. A row that saves a key nobody reads changes nothing.
 */
class SettingsKeysTest {

    private fun javaDir(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull { it.isDirectory } ?: error("src/main/java not found")

    @Test
    fun everyWidgetSettingIsReadByTheApp() {
        val sources = javaDir().walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        val settingsFile = sources.single { it.name == "SettingsScreen.kt" }
        val keys = Regex("""\bcustom\("([a-z0-9_]+)"""").findAll(settingsFile.readText())
            .map { it.groupValues[1] }.toSet()
        assertTrue("expected some widget settings in SettingsScreen.kt", keys.isNotEmpty())

        val elsewhere = sources.filter { it != settingsFile }.joinToString("\n") { it.readText() }
        val unread = keys.filterNot { elsewhere.contains("\"$it\"") }
        assertTrue("settings saved but never read: $unread", unread.isEmpty())
    }
}
