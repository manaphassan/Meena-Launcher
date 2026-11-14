package my.hana.meena.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 9 startup gates: cold under 1.2 s, warm under 0.5 s (median timeToInitialDisplayMs).
 * Run with the phone unlocked and Meena not set as the current home app:
 * ./gradlew :macrobenchmark:connectedBenchmarkAndroidTest
 * https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun cold() = startup(StartupMode.COLD)

    @Test
    fun warm() = startup(StartupMode.WARM)

    private fun startup(mode: StartupMode) = rule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = mode,
        compilationMode = CompilationMode.DEFAULT,
        setupBlock = { pressHome() }
    ) {
        startActivityAndWait()
    }

    private companion object {
        const val TARGET_PACKAGE = "my.hana.meena"
    }
}
