package my.hana.meena.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.regex.Pattern

/**
 * CharterLintTest: Automated gate enforcing Meena UI charter rules across all UI code.
 * Scans app/src/main/java (ui packages and MainActivity.kt) for violations of Metro purity:
 * - CircleShape
 * - .border(
 * - Card(
 * - Surface( with elevation
 * - Toast
 * - Icons. (Default, Filled, Rounded, AutoMirrored.Filled)
 * - FontWeight.Bold/SemiBold/ExtraBold
 * - fontSize below 12.sp
 * - Color(0x and Color.White/Gray/Black outside theme/
 * - emoji and the symbols • ✕ ▲ ▼ → ⚡
 * - numeric padding( or spacedBy( literals not in {0, 6, 12, 24}
 * - uppercase() with allowlist for matching logic
 *
 * Ratchet mechanism:
 * Baseline counts are loaded from app/charter-baseline.txt. The test fails if any count
 * rises above its baseline. Each subsequent phase lowers the baseline until all reach 0.
 */
class CharterLintTest {

    private fun findProjectRoot(): File {
        val candidates = listOf(
            File("."),
            File(".."),
            File("app"),
            File("../app")
        )
        for (dir in candidates) {
            if (File(dir, "src/main/java").isDirectory) {
                return dir.canonicalFile
            }
            if (File(dir, "app/src/main/java").isDirectory) {
                return File(dir, "app").canonicalFile
            }
        }
        return File(".").canonicalFile
    }

    private fun getScannedUiFiles(): List<File> {
        val root = findProjectRoot()
        val javaDir = File(root, "src/main/java")
        val files = mutableListOf<File>()

        javaDir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "kt") {
                val normalizedPath = file.invariantSeparatorsPath
                // Include UI packages and MainActivity (the primary UI host)
                // Exclude theme/ from color literal scans
                if (normalizedPath.contains("/ui/") || file.name == "MainActivity.kt") {
                    files.add(file)
                }
            }
        }
        return files.sortedBy { it.name }
    }

    private fun loadBaseline(): Map<String, Int> {
        val root = findProjectRoot()
        val baselineCandidates = listOf(
            File(root, "charter-baseline.txt"),
            File(root, "../app/charter-baseline.txt"),
            File("charter-baseline.txt"),
            File("app/charter-baseline.txt")
        )
        val baselineFile = baselineCandidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("charter-baseline.txt not found in project root or app/")

        val counts = mutableMapOf<String, Int>()
        baselineFile.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                val parts = trimmed.split("=", limit = 2)
                if (parts.size == 2) {
                    counts[parts[0].trim()] = parts[1].trim().toInt()
                }
            }
        }
        return counts
    }

    data class ScanResults(
        val circleShape: Int,
        val border: Int,
        val card: Int,
        val elevation: Int,
        val toast: Int,
        val icons: Int,
        val fontWeight: Int,
        val fontSizeBelow12sp: Int,
        val colorHex: Int,
        val colorNamed: Int,
        val colorLiterals: Int,
        val emojiAndSymbols: Int,
        val nonStandardSpacing: Int,
        val uppercaseDisplay: Int
    ) {
        fun toMap(): Map<String, Int> = mapOf(
            "CircleShape" to circleShape,
            "border" to border,
            "Card" to card,
            "elevation" to elevation,
            "Toast" to toast,
            "Icons" to icons,
            "FontWeight" to fontWeight,
            "fontSizeBelow12sp" to fontSizeBelow12sp,
            "ColorHex" to colorHex,
            "ColorNamed" to colorNamed,
            "ColorLiterals" to colorLiterals,
            "emojiAndSymbols" to emojiAndSymbols,
            "nonStandardSpacing" to nonStandardSpacing,
            "uppercaseDisplay" to uppercaseDisplay
        )
    }

    private fun performScan(): ScanResults {
        val files = getScannedUiFiles()
        assertTrue("Scanned UI files must not be empty", files.isNotEmpty())

        var circleShapeCount = 0
        var borderCount = 0
        var cardCount = 0
        var elevationCount = 0
        var toastCount = 0
        var iconsCount = 0
        var fontWeightCount = 0
        var fontSizeBelow12Count = 0
        var colorHexCount = 0
        var colorNamedCount = 0
        var emojiAndSymbolsCount = 0
        var nonStandardSpacingCount = 0
        var uppercaseDisplayCount = 0

        val allowedSpacing = setOf(0, 6, 12, 24)
        val spacingRegex = Regex("""(?:padding|spacedBy)\s*\(([^)]*)\)""", RegexOption.MULTILINE)
        val dpRegex = Regex("""(\d+(?:\.\d+)?)\.dp""")
        val fontSizeRegex = Regex("""\bfontSize\s*=\s*(\d+(?:\.\d+)?)\.sp\b""")
        val colorHexRegex = Regex("""\bColor\(0x[0-9a-fA-F]+""")
        val colorNamedRegex = Regex("""\bColor\.(White|Gray|Black|DarkGray|LightGray)\b""")
        val iconsRegex = Regex("""\bIcons\.(Default|Filled|Rounded|AutoMirrored\.Filled)\b""")
        val fontWeightRegex = Regex("""\bFontWeight\.(Bold|SemiBold|ExtraBold|Black)\b""")
        val symbolChars = listOf('•', '✕', '▲', '▼', '→', '⚡')

        for (file in files) {
            val content = file.readText()

            // 1. CircleShape
            circleShapeCount += Regex("""\bCircleShape\b""").findAll(content).count()

            // 2. .border(
            borderCount += Regex("""\.border\(""").findAll(content).count()

            // 3. Card(
            cardCount += Regex("""Card\(""").findAll(content).count()

            // 4. Surface( with elevation
            elevationCount += Regex("""Surface\s*\([^)]*elevation""").findAll(content).count()

            // 5. Toast
            toastCount += Regex("""\bToast\b""").findAll(content).count()

            // 6. Icons. (Default, Filled, Rounded, AutoMirrored.Filled)
            iconsCount += iconsRegex.findAll(content).count()

            // 7. FontWeight.Bold/SemiBold/ExtraBold
            fontWeightCount += fontWeightRegex.findAll(content).count()

            // 8. fontSize below 12.sp
            fontSizeRegex.findAll(content).forEach { match ->
                val sizeVal = match.groupValues[1].toDoubleOrNull() ?: 12.0
                if (sizeVal < 12.0) {
                    fontSizeBelow12Count++
                }
            }

            // 9. Color literals outside theme/
            colorHexCount += colorHexRegex.findAll(content).count()
            colorNamedCount += colorNamedRegex.findAll(content).count()

            // 10. emoji and symbols • ✕ ▲ ▼ → ⚡
            for (sym in symbolChars) {
                emojiAndSymbolsCount += content.count { it == sym }
            }
            // Check surrogate pairs for emojis
            for (i in 0 until content.length - 1) {
                val code = Character.codePointAt(content, i)
                if (code in 0x1F300..0x1FAFF) {
                    emojiAndSymbolsCount++
                }
            }

            // 11. Numeric padding or spacedBy not in {0, 6, 12, 24}
            spacingRegex.findAll(content).forEach { match ->
                val args = match.groupValues[1]
                dpRegex.findAll(args).forEach { dpMatch ->
                    val dpValue = dpMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                    val intVal = dpValue.toInt()
                    if (intVal !in allowedSpacing || dpValue != intVal.toDouble()) {
                        nonStandardSpacingCount++
                    }
                }
            }

            // 12. uppercase() with allowlist for matching logic
            // Allowlist: SettingsScreen.kt symbol/name matching logic
            file.forEachLine { line ->
                if (line.contains(".uppercase(") || line.contains("?.uppercase(")) {
                    val isAllowlisted = line.contains("newSymbol.trim().uppercase()")
                    if (!isAllowlisted) {
                        uppercaseDisplayCount++
                    }
                }
            }
        }

        return ScanResults(
            circleShape = circleShapeCount,
            border = borderCount,
            card = cardCount,
            elevation = elevationCount,
            toast = toastCount,
            icons = iconsCount,
            fontWeight = fontWeightCount,
            fontSizeBelow12sp = fontSizeBelow12Count,
            colorHex = colorHexCount,
            colorNamed = colorNamedCount,
            colorLiterals = colorHexCount + colorNamedCount,
            emojiAndSymbols = emojiAndSymbolsCount,
            nonStandardSpacing = nonStandardSpacingCount,
            uppercaseDisplay = uppercaseDisplayCount
        )
    }

    @Test
    fun charterGates_doNotExceedBaseline() {
        val baseline = loadBaseline()
        val current = performScan()
        val currentMap = current.toMap()

        println("=== Charter Gate Scan Results ===")
        val violations = mutableListOf<String>()

        for ((key, count) in currentMap) {
            val baseCount = baseline[key]
                ?: throw AssertionError("Missing key '$key' in charter-baseline.txt")
            println("  $key: current=$count, baseline=$baseCount")

            if (count > baseCount) {
                violations.add("$key rose above baseline: current=$count > baseline=$baseCount (+$${count - baseCount})")
            }
        }

        // Ratchet down: a baseline above the real count is stale and hides regressions.
        for ((key, count) in currentMap) {
            val baseCount = baseline[key] ?: continue
            if (count < baseCount) {
                violations.add("$key baseline is stale: current=$count < baseline=$baseCount (lower charter-baseline.txt)")
            }
        }

        assertTrue(
            "Charter violations detected:\n" + violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun charterGates_scannedFilesCountIsValid() {
        val files = getScannedUiFiles()
        // After Phase 1 (StartHub, CornerNavHub, LiveTile deleted): 20 files in ui/ + MainActivity.kt = 21 files
        assertTrue("Expected at least 20 UI files, found ${files.size}", files.size >= 20)
    }
}
