package com.example.meenalauncher.data.system

import java.util.Calendar
import java.util.Locale

data class PrayerSlotInfo(
    val name: String,
    val time: String,
    val sub: String,
    val hour24: Int,
    val minute: Int,
    val isSolar: Boolean = false,
    val isCurrent: Boolean = false
)

data class JakimSchedule(
    val zone: String = "WLY01",
    val zoneName: String = "Kuala Lumpur & Putrajaya",
    val slots: List<PrayerSlotInfo>,
    val nextPrayerLabel: String,
    val nextPrayerCountdown: String
)

object JakimSolatRepository {

    fun getTodaySchedule(zone: String = "WLY01"): JakimSchedule {
        val zoneName = when (zone) {
            "SGR01" -> "Shah Alam, Klang, Petaling"
            "JHR02" -> "Johor Bahru, Kulai"
            "PNG01" -> "Pulau Pinang, Butterworth"
            "SBH01" -> "Kota Kinabalu, Ranau"
            "SWK01" -> "Kuching, Bau, Lundu"
            else -> "Kuala Lumpur & Putrajaya (WLY01)"
        }

        // Base times for KL WLY01
        val baseSlots = listOf(
            PrayerSlotInfo("FAJR (SUBUH)", "05:52 AM", "Dawn Twilight", 5, 52),
            PrayerSlotInfo("🌅 SYURUK", "06:58 AM", "Sunrise", 6, 58, isSolar = true),
            PrayerSlotInfo("☀️ DHUHR", "01:06 PM", "Solar Noon", 13, 6),
            PrayerSlotInfo("ASR (ASAR)", "04:16 PM", "Afternoon", 16, 16),
            PrayerSlotInfo("🌇 MAGHRIB", "07:09 PM", "Sunset", 19, 9, isSolar = true),
            PrayerSlotInfo("🌙 ISHA", "08:18 PM", "Nightfall", 20, 18)
        )

        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        var nextSlot: PrayerSlotInfo? = null
        var diffMinutes = 0
        var currentSlotIndex = -1

        for (i in baseSlots.indices) {
            val slotMin = baseSlots[i].hour24 * 60 + baseSlots[i].minute
            if (slotMin <= currentMinutes) {
                currentSlotIndex = i
            }
            if (slotMin > currentMinutes && nextSlot == null) {
                nextSlot = baseSlots[i]
                diffMinutes = slotMin - currentMinutes
            }
        }

        if (nextSlot == null) {
            // Next is Fajr tomorrow
            nextSlot = baseSlots.first()
            val fajrMin = nextSlot.hour24 * 60 + nextSlot.minute
            diffMinutes = (24 * 60 - currentMinutes) + fajrMin
        }

        val hours = diffMinutes / 60
        val mins = diffMinutes % 60
        val countdown = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        val nextLabel = "Next: ${nextSlot.name.replace("🌅 ", "").replace("☀️ ", "").replace("🌇 ", "").replace("🌙 ", "")} in $countdown"

        val markedSlots = baseSlots.mapIndexed { index, slot ->
            slot.copy(isCurrent = (index == currentSlotIndex))
        }

        return JakimSchedule(
            zone = zone,
            zoneName = zoneName,
            slots = markedSlots,
            nextPrayerLabel = nextLabel,
            nextPrayerCountdown = countdown
        )
    }
}
