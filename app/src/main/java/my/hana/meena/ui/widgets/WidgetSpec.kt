package my.hana.meena.ui.widgets

sealed class WidgetState<out T> {
    object NeedsSetup : WidgetState<Nothing>()
    object Loading : WidgetState<Nothing>()
    data class Ready<T>(val data: T, val updatedAt: Long) : WidgetState<T>()
    data class Stale<T>(val data: T, val updatedAt: Long) : WidgetState<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : WidgetState<Nothing>()
    object Empty : WidgetState<Nothing>()
}

enum class RefreshPolicy {
    SYSTEM_CALLBACK, // Clock, battery, etc
    PUSH,            // NotificationListenerService
    OBSERVER,        // CalendarContract
    PERIODIC_MINUTES, // Weather, News, Finance (15-30m)
    MANUAL           // User triggered
}

interface WidgetSpec<T> {
    val id: String
    val title: String
    val source: String
    val refreshPolicy: RefreshPolicy
    val defaultRefreshMinutes: Int? get() = null

    // Setting schema could be defined here later for the UI generator
    // val settingsSchema: List<SettingDefinition>

    fun fetchInitialState(): WidgetState<T>
    suspend fun refresh(): WidgetState<T>
}
