package my.hana.meena

import my.hana.meena.data.system.BatteryRules
import my.hana.meena.data.system.BatterySample
import my.hana.meena.data.system.ChargeAdvice
import my.hana.meena.data.system.ChargeSpeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryRulesTest {

    @Test
    fun speed_followsAndroidSettingsThresholds() {
        // 0.5 A at 5 V = 2.5 W: slow. 1.2 A at 5 V = 6 W: normal. 2 A at 9 V = 18 W: fast.
        assertEquals(ChargeSpeed.SLOW, BatteryRules.speed(500_000, 5_000_000))
        assertEquals(ChargeSpeed.NORMAL, BatteryRules.speed(1_200_000, 5_000_000))
        assertEquals(ChargeSpeed.FAST, BatteryRules.speed(2_000_000, 9_000_000))
        // A missing voltage counts as 5 V, so 2 A is 10 W.
        assertEquals(ChargeSpeed.FAST, BatteryRules.speed(2_000_000, -1))
        assertNull(BatteryRules.speed(-1, 5_000_000))
    }

    @Test
    fun advice_atTheUsualMarks() {
        assertEquals(ChargeAdvice.PLUG_IN_NOW, BatteryRules.advice(8, charging = false, full = false))
        assertEquals(ChargeAdvice.PLUG_IN_SOON, BatteryRules.advice(18, charging = false, full = false))
        assertEquals(ChargeAdvice.NOT_YET, BatteryRules.advice(50, charging = false, full = false))
        assertEquals(ChargeAdvice.NOT_YET, BatteryRules.advice(50, charging = true, full = false))
        assertEquals(ChargeAdvice.UNPLUG_NOW, BatteryRules.advice(82, charging = true, full = false))
        assertEquals(ChargeAdvice.FULL, BatteryRules.advice(100, charging = true, full = true))
    }

    @Test
    fun minutesTo_usesOnlyTheCurrentPlugState() {
        val min = 60_000L
        val now = 10_000L * min
        val samples = listOf(
            BatterySample(now - 200 * min, 40, charging = true),
            BatterySample(now - 60 * min, 80, charging = false),
            BatterySample(now - 30 * min, 77, charging = false),
            BatterySample(now, 74, charging = false)
        )
        // 6% in 60 minutes is 10 minutes a percent; 74% left is 740 minutes.
        assertEquals(740, BatteryRules.minutesTo(samples, now, charging = false, target = 0))
        // The charging readings are an older session, so there is nothing to go on while charging.
        assertNull(BatteryRules.minutesTo(samples, now, charging = true, target = 100))
    }

    @Test
    fun minutesTo_waitsForEnoughToGoOn() {
        val min = 60_000L
        val now = 1_000L * min
        val tooSoon = listOf(BatterySample(now - 5 * min, 60, false), BatterySample(now, 58, false))
        assertNull(BatteryRules.minutesTo(tooSoon, now, charging = false, target = 0))
        val tooLittle = listOf(BatterySample(now - 40 * min, 60, false), BatterySample(now, 59, false))
        assertNull(BatteryRules.minutesTo(tooLittle, now, charging = false, target = 0))
    }

    @Test
    fun encodeDecode_roundTripsAndSkipsJunk() {
        val samples = listOf(BatterySample(1L, 50, true), BatterySample(2L, 49, false))
        assertEquals(samples, BatteryRules.decode(BatteryRules.encode(samples)))
        assertEquals(listOf(BatterySample(5L, 10, false)), BatteryRules.decode("x,y;5,10,0;6,300,1"))
    }
}
