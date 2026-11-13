package my.hana.meena.data.system

/**
 * Pure helpers for an icon pack's appfilter.xml (`<item component="ComponentInfo{pkg/cls}"
 * drawable="name"/>`). Free of Android types so they run in plain JVM tests.
 */
object IconPackAppFilter {

    private const val PREFIX = "ComponentInfo{"
    private const val SUFFIX = "}"

    fun componentKey(packageName: String, className: String): String = "$packageName/$className"

    /**
     * Turns `ComponentInfo{com.foo/com.foo.Main}` (or a bare `com.foo/com.foo.Main`) into the key
     * `com.foo/com.foo.Main`. A class starting with "." gets the app name in front, the same way
     * `ComponentName.unflattenFromString` reads it. Returns null for anything malformed.
     */
    fun parseComponent(raw: String?): String? {
        var value = raw?.trim() ?: return null
        if (value.startsWith(PREFIX) && value.endsWith(SUFFIX)) {
            value = value.substring(PREFIX.length, value.length - SUFFIX.length).trim()
        }
        val slash = value.indexOf('/')
        if (slash <= 0 || slash == value.length - 1) return null
        val pkg = value.substring(0, slash).trim()
        var cls = value.substring(slash + 1).trim()
        if (pkg.isEmpty() || cls.isEmpty()) return null
        if ((pkg + cls).any { it == '{' || it == '}' || it == '/' || it.isWhitespace() }) return null
        if (cls.startsWith(".")) cls = pkg + cls
        return componentKey(pkg, cls)
    }

    /**
     * Builds component key to drawable name from (component, drawable) attribute pairs.
     * Entries with a bad component or a blank drawable are skipped; a later entry for the same
     * component wins, as in Lawnchair's reader.
     */
    fun buildMap(items: Iterable<Pair<String?, String?>>): Map<String, String> {
        val map = HashMap<String, String>()
        for ((component, drawable) in items) {
            val key = parseComponent(component) ?: continue
            val name = drawable?.trim().orEmpty()
            if (name.isEmpty()) continue
            map[key] = name
        }
        return map
    }
}
