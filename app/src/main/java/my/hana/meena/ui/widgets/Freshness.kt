package my.hana.meena.ui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import my.hana.meena.R

enum class AgeUnit { JUST_NOW, MINUTES, HOURS, DAYS }

data class Age(val unit: AgeUnit, val amount: Long)

object Freshness {
    private const val MINUTE_MS = 60_000L
    private const val HOUR_MS = 60 * MINUTE_MS
    private const val DAY_MS = 24 * HOUR_MS

    /** Buckets the time since [updatedAt]. Future times (clock changes) count as just now. */
    fun ageOf(updatedAt: Long, now: Long): Age {
        val diff = now - updatedAt
        return when {
            diff < MINUTE_MS -> Age(AgeUnit.JUST_NOW, 0)
            diff < HOUR_MS -> Age(AgeUnit.MINUTES, diff / MINUTE_MS)
            diff < DAY_MS -> Age(AgeUnit.HOURS, diff / HOUR_MS)
            else -> Age(AgeUnit.DAYS, diff / DAY_MS)
        }
    }
}

/** Current time that re-emits every minute, so "updated x ago" labels keep counting up on screen. */
@Composable
fun rememberNow(intervalMs: Long = 60_000L): Long {
    val now by produceState(initialValue = System.currentTimeMillis(), intervalMs) {
        while (true) {
            delay(intervalMs)
            value = System.currentTimeMillis()
        }
    }
    return now
}

@Composable
fun updatedAgoText(updatedAt: Long): String {
    val age = Freshness.ageOf(updatedAt, rememberNow())
    return when (age.unit) {
        AgeUnit.JUST_NOW -> stringResource(R.string.updated_just_now)
        AgeUnit.MINUTES -> stringResource(R.string.updated_minutes_ago, age.amount)
        AgeUnit.HOURS -> stringResource(R.string.updated_hours_ago, age.amount)
        AgeUnit.DAYS -> stringResource(R.string.updated_days_ago, age.amount)
    }
}
