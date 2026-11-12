package my.hana.meena.data.system

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DeviceCalendarEvent(
    val id: Long,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val isAllDay: Boolean,
    val location: String = "",
    val calendarColor: Int = 0,
    val formattedTime: String = "",
    val formattedDate: String = ""
)

data class CalendarAccount(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val color: Int,
    val isVisible: Boolean
)

sealed interface CalendarState {
    object NeedsPermission : CalendarState
    data class Ready(
        val events: List<DeviceCalendarEvent>,
        val lastUpdated: Long = System.currentTimeMillis()
    ) : CalendarState
}

/**
 * Manages device calendar integration via CalendarContract.Instances.
 * Operates purely under READ_CALENDAR permission without requiring cloud accounts or OAuth.
 */
object DeviceCalendarRepository {

    private val _state = MutableStateFlow<CalendarState>(CalendarState.NeedsPermission)
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    private var observerRegistered = false

    fun hasCalendarPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun registerObserver(context: Context) {
        if (observerRegistered) return
        try {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    refresh(context)
                }
            }
            context.contentResolver.registerContentObserver(
                CalendarContract.Events.CONTENT_URI,
                true,
                observer
            )
            observerRegistered = true
        } catch (_: Exception) {}
    }

    /**
     * Query available calendars from device to display in settings
     */
    fun getAvailableCalendars(context: Context): List<CalendarAccount> {
        if (!hasCalendarPermission(context)) return emptyList()
        val list = mutableListOf<CalendarAccount>()
        try {
            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.CALENDAR_COLOR,
                CalendarContract.Calendars.VISIBLE
            )
            val cursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                "${CalendarContract.Calendars.ACCOUNT_NAME} ASC"
            )
            cursor?.use { c ->
                val idIdx = c.getColumnIndex(CalendarContract.Calendars._ID)
                val nameIdx = c.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accountIdx = c.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                val colorIdx = c.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)
                val visibleIdx = c.getColumnIndex(CalendarContract.Calendars.VISIBLE)
                while (c.moveToNext()) {
                    list.add(
                        CalendarAccount(
                            id = c.getLong(idIdx),
                            displayName = c.getString(nameIdx) ?: "calendar",
                            accountName = c.getString(accountIdx) ?: "",
                            color = if (colorIdx >= 0) c.getInt(colorIdx) else 0,
                            isVisible = if (visibleIdx >= 0) c.getInt(visibleIdx) == 1 else true
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return list
    }

    fun refresh(
        context: Context,
        daysAhead: Int = 7,
        showAllDay: Boolean = true,
        visibleCalendarIds: Set<Long>? = null
    ) {
        if (!hasCalendarPermission(context)) {
            _state.value = CalendarState.NeedsPermission
            return
        }

        registerObserver(context)

        try {
            val nowCal = Calendar.getInstance()
            // Start of today at 00:00
            nowCal.set(Calendar.HOUR_OF_DAY, 0)
            nowCal.set(Calendar.MINUTE, 0)
            nowCal.set(Calendar.SECOND, 0)
            nowCal.set(Calendar.MILLISECOND, 0)
            val startMillis = nowCal.timeInMillis

            // End time N days ahead
            val endCal = Calendar.getInstance().apply {
                timeInMillis = startMillis
                add(Calendar.DAY_OF_YEAR, daysAhead.coerceIn(1, 14))
            }
            val endMillis = endCal.timeInMillis

            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, startMillis)
            ContentUris.appendId(builder, endMillis)

            val projection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.EVENT_LOCATION,
                CalendarContract.Instances.DISPLAY_COLOR,
                CalendarContract.Instances.CALENDAR_ID
            )

            val cursor = context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )

            val eventList = mutableListOf<DeviceCalendarEvent>()
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val todayDayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val todayYear = Calendar.getInstance().get(Calendar.YEAR)

            cursor?.use { c ->
                val idIdx = c.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val titleIdx = c.getColumnIndex(CalendarContract.Instances.TITLE)
                val beginIdx = c.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = c.getColumnIndex(CalendarContract.Instances.END)
                val allDayIdx = c.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                val locationIdx = c.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                val colorIdx = c.getColumnIndex(CalendarContract.Instances.DISPLAY_COLOR)
                val calIdIdx = c.getColumnIndex(CalendarContract.Instances.CALENDAR_ID)

                while (c.moveToNext()) {
                    val id = c.getLong(idIdx)
                    val title = c.getString(titleIdx)?.lowercase() ?: "untitled event"
                    val begin = c.getLong(beginIdx)
                    val end = c.getLong(endIdx)
                    val isAllDay = c.getInt(allDayIdx) == 1
                    val location = c.getString(locationIdx)?.lowercase() ?: ""
                    val color = if (colorIdx >= 0) c.getInt(colorIdx) else 0
                    val calId = if (calIdIdx >= 0) c.getLong(calIdIdx) else -1L

                    // Filter out unselected calendars if specified
                    if (visibleCalendarIds != null && calId != -1L && !visibleCalendarIds.contains(calId)) {
                        continue
                    }

                    // Filter all day events if disabled
                    if (!showAllDay && isAllDay) {
                        continue
                    }

                    val eventCal = Calendar.getInstance().apply { timeInMillis = begin }
                    val isToday = eventCal.get(Calendar.YEAR) == todayYear && eventCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear
                    val isTomorrow = eventCal.get(Calendar.YEAR) == todayYear && eventCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear + 1

                    val dayLabel = when {
                        isToday -> "today"
                        isTomorrow -> "tomorrow"
                        else -> SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(begin)).lowercase()
                    }

                    val timeLabel = if (isAllDay) {
                        "all day"
                    } else {
                        "${timeFormat.format(Date(begin))} – ${timeFormat.format(Date(end))}"
                    }

                    eventList.add(
                        DeviceCalendarEvent(
                            id = id,
                            title = title,
                            startTime = begin,
                            endTime = end,
                            isAllDay = isAllDay,
                            location = location,
                            calendarColor = color,
                            formattedTime = timeLabel,
                            formattedDate = dayLabel
                        )
                    )
                }
            }

            _state.value = CalendarState.Ready(eventList)
        } catch (e: Exception) {
            _state.value = CalendarState.Ready(emptyList())
        }
    }

    fun openNewEvent(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openEventDetail(context: Context, eventId: Long) {
        try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            val intent = Intent(Intent.ACTION_VIEW)
                .setData(uri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    /** Opens the calendar app on a day (VIEW content://com.android.calendar/time/<ms>). */
    fun openDay(context: Context, dayStartMillis: Long) {
        try {
            val builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
            ContentUris.appendId(builder, dayStartMillis)
            val intent = Intent(Intent.ACTION_VIEW)
                .setData(builder.build())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
