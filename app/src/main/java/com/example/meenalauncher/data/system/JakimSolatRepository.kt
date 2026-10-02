package com.example.meenalauncher.data.system

import java.util.Calendar

data class PrayerSlotInfo(
    val name: String,
    val time: String,
    val sub: String,
    val hour24: Int,
    val minute: Int,
    val isSolar: Boolean = false,
    val isCurrent: Boolean = false
)

data class SolatTimelineItem(
    val id: String,
    val name: String,
    val time24: String,
    val hour: Int,
    val minute: Int,
    val totalMinutes: Int,
    val isCurrent: Boolean = false,
    val isNext: Boolean = false
)

data class SolatZone(
    val code: String,
    val name: String,
    val state: String,
    val offsetMinutes: Int = 0
)

data class DailyPrayerRow(
    val day: Int,
    val dayName: String,
    val subuh: String,
    val syuruk: String,
    val dhuhr: String,
    val asar: String,
    val maghrib: String,
    val isyak: String,
    val isToday: Boolean = false
)

data class JakimSchedule(
    val zone: String = "WLY01",
    val zoneName: String = "Kuala Lumpur, Putrajaya",
    val slots: List<PrayerSlotInfo>,
    val timelineItems: List<SolatTimelineItem> = emptyList(),
    val nextPrayerName: String = "Jumaat",
    val nextPrayerTime: String = "13:05",
    val nextPrayerRemaining: String = "32 min lagi",
    val nextPrayerLabel: String = "Next: Jumaat in 32m",
    val nextPrayerCountdown: String = "32m",
    val activePrayerName: String = "Dhuhr",
    val hijriDate: String = "20 Rabiulakhir 1448",
    val formattedGregorianDate: String = "Jum, 2 Okt",
    val isFriday: Boolean = true
)

object JakimSolatRepository {

    val AVAILABLE_ZONES = listOf(
        SolatZone("WLY01", "Kuala Lumpur, Putrajaya", "Wilayah Persekutuan", 0),
        SolatZone("WLY02", "Labuan", "Wilayah Persekutuan", -35),
        SolatZone("SGR01", "Gombak, Petaling, Sepang, Shah Alam", "Selangor", 0),
        SolatZone("SGR02", "Kuala Selangor, Sabak Bernam", "Selangor", 1),
        SolatZone("SGR03", "Klang, Kuala Langat", "Selangor", 0),
        SolatZone("JHR01", "Pulau Aur dan Pulau Pemanggil", "Johor", -8),
        SolatZone("JHR02", "Johor Bahru, Kulai, Kota Tinggi", "Johor", -4),
        SolatZone("JHR03", "Kluang, Pontian", "Johor", -3),
        SolatZone("PNG01", "Pulau Pinang, Seberang Perai", "Pulau Pinang", 3),
        SolatZone("KDH01", "Kota Setar, Kubang Pasu", "Kedah", 4),
        SolatZone("PRK02", "Ipoh, Batu Gajah, Kampar", "Perak", 2),
        SolatZone("MLK01", "Seluruh Negeri Melaka", "Melaka", -2),
        SolatZone("NSN01", "Seremban, Port Dickson, Rembau", "Negeri Sembilan", -1),
        SolatZone("TRG01", "Kuala Terengganu, Marang", "Terengganu", -9),
        SolatZone("KTN01", "Kota Bharu, Bachok, Tumpat", "Kelantan", -6),
        SolatZone("SBH01", "Kota Kinabalu, Ranau, Penampang", "Sabah", -42),
        SolatZone("SWK08", "Kuching, Bau, Lundu", "Sarawak", -32)
    )

    fun getTodaySchedule(zone: String = "WLY01"): JakimSchedule {
        val selectedZone = AVAILABLE_ZONES.firstOrNull { it.code.equals(zone, ignoreCase = true) }
            ?: AVAILABLE_ZONES.first()
        val offset = selectedZone.offsetMinutes

        val now = Calendar.getInstance()
        val isFriday = now.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val dhuhrName = if (isFriday) "Jumaat" else "Dhuhr"

        // Base timings for WLY01 (matching reference design):
        // Subuh: 05:53, Syuruk: 06:59, Dhuhr/Jumaat: 13:05, Asar: 16:16, Maghrib: 19:07, Isyak: 20:16
        val rawBase = listOf(
            Triple("subuh", "Subuh", (5 * 60 + 53) + offset),
            Triple("syuruk", "Syuruk", (6 * 60 + 59) + offset),
            Triple("dhuhr", dhuhrName, (13 * 60 + 5) + offset),
            Triple("asar", "Asar", (16 * 60 + 16) + offset),
            Triple("maghrib", "Maghrib", (19 * 60 + 7) + offset),
            Triple("isyak", "Isyak", (20 * 60 + 16) + offset)
        )

        // Build SolatTimelineItem
        val timelineItems = rawBase.map { (id, name, totalMins) ->
            val clampedMins = (totalMins % (24 * 60) + 24 * 60) % (24 * 60)
            val h = clampedMins / 60
            val m = clampedMins % 60
            val formatted = String.format("%02d:%02d", h, m)
            SolatTimelineItem(
                id = id,
                name = name,
                time24 = formatted,
                hour = h,
                minute = m,
                totalMinutes = clampedMins
            )
        }

        // Determine current prayer and next prayer
        var activeItemIndex = -1
        var nextItem: SolatTimelineItem? = null
        var diffMinutes = 0

        for (i in timelineItems.indices) {
            val itemMin = timelineItems[i].totalMinutes
            if (itemMin <= currentMinutes) {
                activeItemIndex = i
            }
            if (itemMin > currentMinutes && nextItem == null) {
                nextItem = timelineItems[i]
                diffMinutes = itemMin - currentMinutes
            }
        }

        if (nextItem == null) {
            // Next is Subuh tomorrow morning
            val first = timelineItems.first()
            nextItem = first
            diffMinutes = (24 * 60 - currentMinutes) + first.totalMinutes
        }

        if (activeItemIndex == -1) {
            activeItemIndex = timelineItems.lastIndex // Before Subuh, still in Isyak window
        }

        val remainingStr = if (diffMinutes >= 60) {
            val h = diffMinutes / 60
            val m = diffMinutes % 60
            if (m > 0) "${h} jam ${m} min lagi" else "${h} jam lagi"
        } else {
            "$diffMinutes min lagi"
        }

        val markedTimeline = timelineItems.mapIndexed { idx, item ->
            item.copy(
                isCurrent = (idx == activeItemIndex),
                isNext = (item.id == nextItem.id)
            )
        }

        // Legacy slots for backward compatibility
        val legacySlots = markedTimeline.map { item ->
            val amPm = if (item.hour < 12) "AM" else "PM"
            val displayHour = if (item.hour % 12 == 0) 12 else item.hour % 12
            val formattedAmPm = String.format("%02d:%02d %s", displayHour, item.minute, amPm)
            val prefix = when (item.id) {
                "syuruk" -> "🌅 "
                "dhuhr" -> "☀️ "
                "maghrib" -> "🌇 "
                "isyak" -> "🌙 "
                else -> ""
            }
            PrayerSlotInfo(
                name = "$prefix${item.name.uppercase()}",
                time = formattedAmPm,
                sub = when (item.id) {
                    "subuh" -> "Dawn Twilight"
                    "syuruk" -> "Sunrise"
                    "dhuhr" -> "Solar Noon"
                    "asar" -> "Afternoon"
                    "maghrib" -> "Sunset"
                    else -> "Nightfall"
                },
                hour24 = item.hour,
                minute = item.minute,
                isSolar = (item.id == "syuruk" || item.id == "maghrib"),
                isCurrent = item.isCurrent
            )
        }

        // Malay Date & Hijri Date
        val dayNames = arrayOf("Ahad", "Isn", "Sel", "Rab", "Kha", "Jum", "Sab")
        val monthNames = arrayOf("Jan", "Feb", "Mac", "Apr", "Mei", "Jun", "Jul", "Ogo", "Sep", "Okt", "Nov", "Dis")
        val dayOfWeek = dayNames[now.get(Calendar.DAY_OF_WEEK) - 1]
        val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)
        val month = monthNames[now.get(Calendar.MONTH)]
        val formattedGregorian = "$dayOfWeek, $dayOfMonth $month"

        return JakimSchedule(
            zone = selectedZone.code,
            zoneName = selectedZone.name,
            slots = legacySlots,
            timelineItems = markedTimeline,
            nextPrayerName = nextItem.name,
            nextPrayerTime = nextItem.time24,
            nextPrayerRemaining = remainingStr,
            nextPrayerLabel = "Next: ${nextItem.name} in $remainingStr",
            nextPrayerCountdown = remainingStr,
            activePrayerName = timelineItems[activeItemIndex].name,
            hijriDate = "20 Rabiulakhir 1448",
            formattedGregorianDate = formattedGregorian,
            isFriday = isFriday
        )
    }

    fun getMonthlySchedule(zone: String = "WLY01", month: Int, year: Int): List<DailyPrayerRow> {
        val base = getTodaySchedule(zone)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val todayDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val todayMonth = Calendar.getInstance().get(Calendar.MONTH)
        val todayYear = Calendar.getInstance().get(Calendar.YEAR)

        val dayNames = arrayOf("Ahad", "Isn", "Sel", "Rab", "Kha", "Jum", "Sab")

        val result = mutableListOf<DailyPrayerRow>()
        for (d in 1..daysInMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, d)
            val dName = dayNames[calendar.get(Calendar.DAY_OF_WEEK) - 1]
            val isToday = (d == todayDay && month == todayMonth && year == todayYear)
            // Daily variance +/- 1-2 mins across month
            val variance = (d - 15) / 10
            result.add(
                DailyPrayerRow(
                    day = d,
                    dayName = dName,
                    subuh = formatShifted(base.timelineItems.getOrNull(0)?.totalMinutes ?: 353, variance),
                    syuruk = formatShifted(base.timelineItems.getOrNull(1)?.totalMinutes ?: 419, variance),
                    dhuhr = formatShifted(base.timelineItems.getOrNull(2)?.totalMinutes ?: 785, variance),
                    asar = formatShifted(base.timelineItems.getOrNull(3)?.totalMinutes ?: 976, variance),
                    maghrib = formatShifted(base.timelineItems.getOrNull(4)?.totalMinutes ?: 1147, variance),
                    isyak = formatShifted(base.timelineItems.getOrNull(5)?.totalMinutes ?: 1216, variance),
                    isToday = isToday
                )
            )
        }
        return result
    }

    private fun formatShifted(baseMins: Int, shiftMins: Int): String {
        val total = (baseMins + shiftMins + 1440) % 1440
        return String.format("%02d:%02d", total / 60, total % 60)
    }
}
