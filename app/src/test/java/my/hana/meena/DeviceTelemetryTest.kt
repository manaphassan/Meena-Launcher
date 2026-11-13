package my.hana.meena

import my.hana.meena.data.system.DeviceTelemetryHelper
import my.hana.meena.data.system.RealDeviceTelemetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceTelemetryTest {

    @Test
    fun nothingIsKnown_untilThePhoneReportsIt() {
        val t = RealDeviceTelemetry()
        assertNull(t.batteryPercent)
        assertNull(t.freeMemoryGb)
        assertNull(t.memoryUsedPercent)
        assertNull(t.freeStorageGb)
        assertNull(t.storageUsedPercent)
        assertNull(t.screenBrightnessPercent)
    }

    @Test
    fun battery_needsAValidLevelAndScale() {
        assertEquals(54, DeviceTelemetryHelper.batteryPercent(54, 100))
        assertEquals(50, DeviceTelemetryHelper.batteryPercent(128, 256))
        assertNull(DeviceTelemetryHelper.batteryPercent(-1, 100))
        assertNull(DeviceTelemetryHelper.batteryPercent(50, 0))
    }

    @Test
    fun usedPercent_rejectsNonsense() {
        assertEquals(75, DeviceTelemetryHelper.usedPercent(400, 100))
        assertNull(DeviceTelemetryHelper.usedPercent(0, 0))
        assertNull(DeviceTelemetryHelper.usedPercent(100, 200))
    }

    @Test
    fun brightness_onlyWithinAndroidsRange() {
        assertEquals(100, DeviceTelemetryHelper.brightnessPercent(255))
        assertEquals(0, DeviceTelemetryHelper.brightnessPercent(0))
        assertNull(DeviceTelemetryHelper.brightnessPercent(4095))
    }

    @Test
    fun gigabytes_toOneDecimal() {
        assertEquals(1.5, DeviceTelemetryHelper.toGb(1_610_612_736L), 0.0)
    }
}
