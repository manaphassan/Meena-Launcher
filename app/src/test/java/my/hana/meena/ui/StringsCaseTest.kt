package my.hana.meena.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * StringsCaseTest: Automated gate enforcing strict Metro lowercase copy rule.
 *
 * Scans res/values/strings.xml and fails if any string value has an uppercase letter,
 * except for proper nouns and placeholders specified in strings-case-allowlist.txt.
 */
class StringsCaseTest {

    private fun findProjectRoot(): File {
        val candidates = listOf(
            File("."),
            File(".."),
            File("app"),
            File("../app")
        )
        for (dir in candidates) {
            if (File(dir, "src/main/res/values/strings.xml").exists()) {
                return dir.canonicalFile
            }
            if (File(dir, "app/src/main/res/values/strings.xml").exists()) {
                return File(dir, "app").canonicalFile
            }
        }
        return File(".").canonicalFile
    }

    private fun loadAllowlist(): Pair<Set<String>, List<Regex>> {
        val root = findProjectRoot()
        val candidates = listOf(
            File(root, "strings-case-allowlist.txt"),
            File(root, "../app/strings-case-allowlist.txt"),
            File("strings-case-allowlist.txt"),
            File("app/strings-case-allowlist.txt")
        )
        val allowlistFile = candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("strings-case-allowlist.txt not found")

        val exactTokens = mutableSetOf<String>()
        val regexes = mutableListOf<Regex>()

        allowlistFile.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                if (trimmed.contains("%") || trimmed.contains("[") || trimmed.contains("\\")) {
                    regexes.add(Regex(trimmed))
                } else {
                    exactTokens.add(trimmed)
                }
            }
        }
        return Pair(exactTokens, regexes)
    }

    @Test
    fun allStringsAreLowercase_exceptAllowlist() {
        val root = findProjectRoot()
        val stringsXmlFile = File(root, "src/main/res/values/strings.xml")
        assertTrue("strings.xml must exist at ${stringsXmlFile.absolutePath}", stringsXmlFile.exists())

        val (allowlistTokens, allowlistRegexes) = loadAllowlist()

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(stringsXmlFile)
        doc.documentElement.normalize()

        val stringNodes = doc.getElementsByTagName("string")
        val violations = mutableListOf<String>()

        val defaultPlaceholderRegex = Regex("""%[0-9]+\$[a-zA-Z]|%[a-zA-Z]""")

        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i)
            if (node is Element) {
                val name = node.getAttribute("name")
                val text = node.textContent ?: ""

                // 1. If key is explicitly allowlisted, skip
                if (allowlistTokens.contains(name)) {
                    continue
                }

                // 2. Strip standard placeholders (%1$s, %d, etc.)
                var processed = defaultPlaceholderRegex.replace(text, "")

                // 3. Strip custom regex allowlists
                for (regex in allowlistRegexes) {
                    processed = regex.replace(processed, "")
                }

                // 4. Strip exact token allowlists (e.g. proper noun "Meena")
                for (token in allowlistTokens) {
                    processed = processed.replace(token, "")
                }

                // 5. Check remaining text for uppercase characters
                val uppercaseChars = processed.filter { it.isUpperCase() }
                if (uppercaseChars.isNotEmpty()) {
                    violations.add(
                        "String '$name' has uppercase characters: '$uppercaseChars' in value: \"$text\""
                    )
                }
            }
        }

        assertTrue(
            "Found unallowlisted uppercase text in strings.xml:\n" + violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    /** Every user-facing value: plain strings plus plural and array items. */
    private fun allValues(): List<Pair<String, String>> {
        val file = File(findProjectRoot(), "src/main/res/values/strings.xml")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val out = mutableListOf<Pair<String, String>>()
        for (tag in listOf("string", "item")) {
            val nodes = doc.getElementsByTagName(tag)
            for (i in 0 until nodes.length) {
                val el = nodes.item(i) as? Element ?: continue
                val name = el.getAttribute("name").ifEmpty {
                    (el.parentNode as? Element)?.getAttribute("name").orEmpty() + "[" + el.getAttribute("quantity") + "]"
                }
                out += name to (el.textContent ?: "")
            }
        }
        return out
    }

    @Test
    fun pluralItemsAreLowercase() {
        val bad = allValues().filter { (name, _) -> name.contains("[") }
            .filter { (_, text) -> text.replace(Regex("""%[0-9]+\$[a-zA-Z]|%[a-zA-Z]"""), "").any { it.isUpperCase() } }
        assertTrue("uppercase in plurals: $bad", bad.isEmpty())
    }

    // GEMINI.md rule 8: copy is jargon-free.
    @Test
    fun noBannedWords() {
        val banned = listOf("telemetry", "ram", "ltpo", "realtime sync", "permission", "intent", "package", "canvas", "proactive", "context")
        val bad = allValues().flatMap { (name, text) ->
            banned.filter { Regex("""\b${Regex.escape(it)}\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) }.map { "$name: $it" }
        }
        assertTrue("banned words in strings.xml:\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    // GEMINI.md rule 7: no emoji or symbol glyphs. The middle dot separator, degree sign and the
    // map attribution's copyright sign are typography, not decoration, so they stay allowed.
    @Test
    fun noSymbolGlyphs() {
        val symbols = setOf('•', '✕', '×', '▲', '▼', '→', '←', '↑', '↓', '⚡', '✓', '✔', '★', '☆')
        val bad = allValues().filter { (_, text) ->
            text.any { it in symbols } ||
                text.codePoints().anyMatch { it in 0x1F300..0x1FAFF || it in 0x2600..0x27BF } ||
                Regex("""(^|\s)\+(\s|$)""").containsMatchIn(text)
        }
        assertTrue("symbols in strings.xml:\n" + bad.joinToString("\n"), bad.isEmpty())
    }
}
