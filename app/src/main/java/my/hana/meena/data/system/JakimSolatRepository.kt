package my.hana.meena.data.system

import java.util.Calendar
import my.hana.meena.ui.widgets.RefreshPolicy
import my.hana.meena.ui.widgets.WidgetSpec
import my.hana.meena.ui.widgets.WidgetState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val zone: String = "",
    val zoneName: String = "",
    val slots: List<PrayerSlotInfo> = emptyList(),
    val timelineItems: List<SolatTimelineItem> = emptyList(),
    val nextPrayerName: String = "",
    val nextPrayerTime: String = "",
    val nextPrayerRemaining: String = "",
    val nextPrayerLabel: String = "where are you?",
    val nextPrayerCountdown: String = "",
    val activePrayerName: String = "",
    val hijriDate: String = "",
    val formattedGregorianDate: String = "",
    val isFriday: Boolean = false
)

object JakimSolatRepository : WidgetSpec<JakimSchedule> {
    override val id = "widget-solat"
    override val title = "solat"
    override val source = "JAKIM e-Solat"
    override val refreshPolicy = RefreshPolicy.PERIODIC_MINUTES
    override val defaultRefreshMinutes = 720 // 12h

    private val _state = MutableStateFlow<WidgetState<JakimSchedule>>(WidgetState.NeedsSetup)
    val state: StateFlow<WidgetState<JakimSchedule>> = _state.asStateFlow()

    override fun fetchInitialState(): WidgetState<JakimSchedule> = _state.value

    private var currentZone: String = ""

    fun setZone(zone: String) {
        currentZone = zone
    }

    override suspend fun refresh(): WidgetState<JakimSchedule> {
        if (currentZone.isBlank()) {
            _state.value = WidgetState.NeedsSetup
            return _state.value
        }
        
        _state.value = WidgetState.Loading
        try {
            val client = okhttp3.OkHttpClient()
            val request = okhttp3.Request.Builder()
                .url("https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=today&zone=$currentZone")
                .build()

            val response = kotlinx.coroutines.Dispatchers.IO.let {
                kotlinx.coroutines.withContext(it) {
                    client.newCall(request).execute()
                }
            }

            if (!response.isSuccessful || response.body == null) {
                _state.value = WidgetState.Error("HTTP Error: ${response.code}")
                return _state.value
            }

            val jsonString = response.body!!.string()
            val jsonObject = org.json.JSONObject(jsonString)
            
            if (jsonObject.getString("status") != "OK!") {
                _state.value = WidgetState.Error("API Error: ${jsonObject.getString("status")}")
                return _state.value
            }

            val prayerTimesArray = jsonObject.getJSONArray("prayerTime")
            if (prayerTimesArray.length() == 0) {
                _state.value = WidgetState.Error("No prayer times found")
                return _state.value
            }

            val today = prayerTimesArray.getJSONObject(0)
            val zoneName = AVAILABLE_ZONES.find { it.code == currentZone }?.name ?: currentZone
            
            val slots = listOf(
                PrayerSlotInfo("imsak", today.getString("imsak").take(5), "", today.getString("imsak").substring(0,2).toInt(), today.getString("imsak").substring(3,5).toInt(), isSolar = true),
                PrayerSlotInfo("subuh", today.getString("fajr").take(5), "", today.getString("fajr").substring(0,2).toInt(), today.getString("fajr").substring(3,5).toInt()),
                PrayerSlotInfo("syuruk", today.getString("syuruk").take(5), "", today.getString("syuruk").substring(0,2).toInt(), today.getString("syuruk").substring(3,5).toInt(), isSolar = true),
                PrayerSlotInfo("zohor", today.getString("dhuhr").take(5), "", today.getString("dhuhr").substring(0,2).toInt(), today.getString("dhuhr").substring(3,5).toInt()),
                PrayerSlotInfo("asar", today.getString("asr").take(5), "", today.getString("asr").substring(0,2).toInt(), today.getString("asr").substring(3,5).toInt()),
                PrayerSlotInfo("maghrib", today.getString("maghrib").take(5), "", today.getString("maghrib").substring(0,2).toInt(), today.getString("maghrib").substring(3,5).toInt()),
                PrayerSlotInfo("isyak", today.getString("isha").take(5), "", today.getString("isha").substring(0,2).toInt(), today.getString("isha").substring(3,5).toInt())
            )

            // Timeline items map directly from slots except we might filter out non-prayers depending on settings
            val timelineItems = slots.map { 
                SolatTimelineItem(it.name, it.name, it.time, it.hour24, it.minute, it.hour24 * 60 + it.minute)
            }

            val schedule = JakimSchedule(
                zone = currentZone,
                zoneName = zoneName,
                slots = slots,
                timelineItems = timelineItems,
                hijriDate = today.getString("hijri"),
                formattedGregorianDate = today.getString("date"),
                isFriday = today.getString("day").equals("Friday", ignoreCase = true)
            )

            _state.value = WidgetState.Ready(schedule, System.currentTimeMillis())
        } catch (e: Exception) {
            _state.value = WidgetState.Error("Failed to fetch solat data", e)
        }
        return _state.value
    }

    val AVAILABLE_ZONES = listOf(
        SolatZone("WLY01", "Kuala Lumpur, Putrajaya", "Wilayah Persekutuan"),
        SolatZone("WLY02", "Labuan", "Wilayah Persekutuan"),
        SolatZone("SGR01", "Gombak, Petaling, Sepang, Hulu Langat, Hulu Selangor, Shah Alam", "Selangor"),
        SolatZone("SGR02", "Kuala Selangor, Sabak Bernam", "Selangor"),
        SolatZone("SGR03", "Klang, Kuala Langat", "Selangor"),
        SolatZone("JHR01", "Pulau Aur dan Pulau Pemanggil", "Johor"),
        SolatZone("JHR02", "Johor Bahru, Kota Tinggi, Mersing, Kulai", "Johor"),
        SolatZone("JHR03", "Kluang, Pontian", "Johor"),
        SolatZone("PNG01", "Seluruh Negeri Pulau Pinang", "Pulau Pinang"),
        SolatZone("KDH01", "Kota Setar, Kubang Pasu, Pokok Sena", "Kedah"),
        SolatZone("PRK01", "Tapah, Slim River, Tanjung Malim", "Perak"),
        SolatZone("PRK02", "Kuala Kangsar, Sungai Siput, Ipoh, Batu Gajah, Kampar", "Perak"),
        SolatZone("MLK01", "Seluruh Negeri Melaka", "Melaka"),
        SolatZone("NGS03", "Port Dickson, Seremban", "Negeri Sembilan"),
        SolatZone("TRG01", "Kuala Terengganu, Marang, Kuala Nerus", "Terengganu"),
        SolatZone("KTN01", "Bachok, Kota Bharu, Machang, Pasir Mas, Pasir Puteh, Tanah Merah, Tumpat, Kuala Krai", "Kelantan"),
        SolatZone("SBH07", "Kota Kinabalu, Ranau, Kota Belud, Tuaran, Penampang, Papar, Putatan", "Sabah"),
        SolatZone("SWK08", "Kuching, Bau, Lundu, Sematan", "Sarawak")
    )

    suspend fun fetchMonthlySchedule(zone: String): List<DailyPrayerRow> {
        return kotlinx.coroutines.Dispatchers.IO.let {
            kotlinx.coroutines.withContext(it) {
                try {
                    val client = okhttp3.OkHttpClient()
                    val request = okhttp3.Request.Builder()
                        .url("https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=month&zone=$zone")
                        .build()

                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful || response.body == null) return@withContext emptyList()

                    val jsonString = response.body!!.string()
                    val jsonObject = org.json.JSONObject(jsonString)
                    if (jsonObject.getString("status") != "OK!") return@withContext emptyList()

                    val prayerTimesArray = jsonObject.getJSONArray("prayerTime")
                    val rows = mutableListOf<DailyPrayerRow>()
                    
                    val now = Calendar.getInstance()
                    val currentDay = now.get(Calendar.DAY_OF_MONTH)
                    
                    for (i in 0 until prayerTimesArray.length()) {
                        val dayObj = prayerTimesArray.getJSONObject(i)
                        val dateParts = dayObj.getString("date").split("-") // e.g. "01-Oct-2026"
                        val dayNum = dateParts[0].toIntOrNull() ?: (i + 1)
                        
                        rows.add(
                            DailyPrayerRow(
                                day = dayNum,
                                dayName = dayObj.getString("day"),
                                subuh = dayObj.getString("fajr").take(5),
                                syuruk = dayObj.getString("syuruk").take(5),
                                dhuhr = dayObj.getString("dhuhr").take(5),
                                asar = dayObj.getString("asr").take(5),
                                maghrib = dayObj.getString("maghrib").take(5),
                                isyak = dayObj.getString("isha").take(5),
                                isToday = dayNum == currentDay
                            )
                        )
                    }
                    rows
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }
}


