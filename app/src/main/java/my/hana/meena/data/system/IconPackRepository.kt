package my.hana.meena.data.system

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.util.concurrent.ConcurrentHashMap

data class IconPackInfo(val packageName: String, val label: String)

/**
 * Third-party icon packs (ADW / Nova / Apex style). There is no Android API for these; packs
 * advertise themselves with launcher-specific intent filters and ship res/xml/appfilter.xml
 * (or assets/appfilter.xml). Every intent here needs a matching `<queries>` entry in the manifest.
 */
object IconPackRepository {

    private fun discoveryIntents(): List<Intent> = listOf(
        Intent("org.adw.launcher.THEMES"),
        Intent("org.adw.launcher.icons.ACTION_PICK_ICON"),
        Intent("com.novalauncher.THEME"),
        Intent("com.gau.go.launcherex.theme"),
        Intent("com.dlto.atom.launcher.THEME"),
        // Apex uses a category on MAIN rather than its own action.
        Intent(Intent.ACTION_MAIN).addCategory("com.anddoes.launcher.THEME")
    )

    private class LoadedPack(val resources: Resources, val drawableNames: Map<String, String>) {
        val idCache = ConcurrentHashMap<String, Int>()
    }

    private val packs = ConcurrentHashMap<String, LoadedPack>()

    @Suppress("DEPRECATION")
    suspend fun installedPacks(context: Context): List<IconPackInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        discoveryIntents()
            .flatMap { runCatching { pm.queryIntentActivities(it, 0) }.getOrDefault(emptyList()) }
            .mapNotNull { it.activityInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .map { IconPackInfo(it.packageName, it.applicationInfo.loadLabel(pm).toString()) }
            .sortedBy { it.label.lowercase() }
    }

    /** The pack's name, or null when it is not installed (any more). */
    @Suppress("DEPRECATION")
    suspend fun labelFor(context: Context, packageName: String): String? = withContext(Dispatchers.IO) {
        if (packageName.isEmpty()) return@withContext null
        val pm = context.packageManager
        try {
            pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /** Drops the parsed map, e.g. after the pack was updated or removed. */
    fun forget(packageName: String) {
        packs.remove(packageName)
    }

    /**
     * The pack's icon for one app, or null when the pack is gone or doesn't cover that app.
     * Parses the pack on first use, so call this off the main thread.
     */
    @SuppressLint("DiscouragedApi") // A third-party pack's drawables can only be looked up by name.
    fun iconFor(context: Context, packPackage: String, appPackage: String, appClass: String): Drawable? {
        val pack = load(context, packPackage) ?: return null
        val name = pack.drawableNames[IconPackAppFilter.componentKey(appPackage, appClass)] ?: return null
        val id = pack.idCache.getOrPut(name) { pack.resources.getIdentifier(name, "drawable", packPackage) }
        if (id == 0) return null
        return try {
            pack.resources.getDrawable(id, null)
        } catch (e: Resources.NotFoundException) {
            null
        }
    }

    private fun load(context: Context, packPackage: String): LoadedPack? {
        packs[packPackage]?.let { return it }
        val resources = try {
            context.packageManager.getResourcesForApplication(packPackage)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
        val pack = LoadedPack(resources, IconPackAppFilter.buildMap(readAppFilter(resources, packPackage)))
        packs[packPackage] = pack
        return pack
    }

    @SuppressLint("DiscouragedApi")
    private fun readAppFilter(resources: Resources, packPackage: String): List<Pair<String?, String?>> {
        val xmlId = resources.getIdentifier("appfilter", "xml", packPackage)
        if (xmlId != 0) {
            return try {
                val parser = resources.getXml(xmlId)
                try {
                    readItems(parser)
                } finally {
                    parser.close()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
        return try {
            resources.assets.open("appfilter.xml").use { stream ->
                val parser = Xml.newPullParser()
                parser.setInput(stream, "UTF-8")
                readItems(parser)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Collects `<item component=".." drawable=".."/>`; keeps what was read if the file is broken. */
    private fun readItems(parser: XmlPullParser): List<Pair<String?, String?>> {
        val items = ArrayList<Pair<String?, String?>>()
        try {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "item") {
                    items += parser.getAttributeValue(null, "component") to parser.getAttributeValue(null, "drawable")
                }
                event = parser.next()
            }
        } catch (e: Exception) {
            // A malformed pack must not break the apps list.
        }
        return items
    }
}
