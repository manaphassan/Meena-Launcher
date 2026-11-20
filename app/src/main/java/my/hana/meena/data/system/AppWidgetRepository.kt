package my.hana.meena.data.system

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Process

/**
 * Hosts other apps' Android widgets on the today page.
 * https://developer.android.com/develop/ui/views/appwidgets/host
 *
 * The widget IDs Meena owns are kept in settings ([my.hana.meena.data.model.MeenaUserSettings.hostedWidgetIds]).
 * Widgets are drawn by their own apps; Meena only gives them a place and a size.
 */
object AppWidgetRepository {

    /** Fixed for the lifetime of the app: Android ties widget IDs to this host ID. */
    const val HOST_ID = 0x4D45

    @Volatile
    private var host: AppWidgetHost? = null

    fun host(context: Context): AppWidgetHost =
        host ?: synchronized(this) {
            host ?: AppWidgetHost(context.applicationContext, HOST_ID).also { host = it }
        }

    fun manager(context: Context): AppWidgetManager = AppWidgetManager.getInstance(context.applicationContext)

    /** Widgets get updates only while the host listens; call from the activity's onStart. */
    fun startListening(context: Context) {
        runCatching { host(context).startListening() }
    }

    fun stopListening(context: Context) {
        runCatching { host(context).stopListening() }
    }

    /** Every widget the phone's apps offer, by app then name. */
    fun providers(context: Context): List<AppWidgetProviderInfo> {
        val pm = context.packageManager
        return runCatching { manager(context).getInstalledProvidersForProfile(Process.myUserHandle()) }
            .getOrNull().orEmpty()
            .sortedWith(compareBy({ appLabel(context, it).lowercase() }, { it.loadLabel(pm).lowercase() }))
    }

    fun info(context: Context, widgetId: Int): AppWidgetProviderInfo? =
        runCatching { manager(context).getAppWidgetInfo(widgetId) }.getOrNull()

    fun appLabel(context: Context, info: AppWidgetProviderInfo): String {
        val pm = context.packageManager
        return runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
        }.getOrDefault(info.provider.packageName)
    }

    fun allocate(context: Context): Int = host(context).allocateAppWidgetId()

    /** True when Android lets Meena bind without asking (the person ticked "always allow" before). */
    fun bindIfAllowed(context: Context, widgetId: Int, provider: ComponentName): Boolean =
        runCatching { manager(context).bindAppWidgetIdIfAllowed(widgetId, provider) }.getOrDefault(false)

    /** True when the widget must show its own setup screen before it can be placed. */
    fun needsConfigure(info: AppWidgetProviderInfo): Boolean {
        if (info.configure == null) return false
        // CONFIGURATION_OPTIONAL is new in Android 12; before that the bit is never set.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val optional = AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL
            val reconfigurable = AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE
            if (info.widgetFeatures and optional != 0 && info.widgetFeatures and reconfigurable != 0) return false
        }
        return true
    }

    fun delete(context: Context, widgetId: Int) {
        runCatching { host(context).deleteAppWidgetId(widgetId) }
    }

    fun createView(context: Context, widgetId: Int, info: AppWidgetProviderInfo): AppWidgetHostView =
        host(context).createView(context, widgetId, info)

    /** Tells the widget how big it is, in dp, so it can lay itself out. */
    @Suppress("DEPRECATION")
    fun reportSize(view: AppWidgetHostView, widthDp: Int, heightDp: Int) {
        runCatching { view.updateAppWidgetSize(Bundle(), widthDp, heightDp, widthDp, heightDp) }
    }

    /** Height to give a widget: what it asks for (AppWidgetProviderInfo sizes are in px), within reason. */
    fun heightDp(info: AppWidgetProviderInfo, density: Float): Int {
        val asked = (info.minHeight / density).toInt()
        return asked.coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP)
    }

    const val MIN_HEIGHT_DP = 72
    const val MAX_HEIGHT_DP = 480
    /** How tall a widget can be dragged. */
    const val RESIZE_MAX_DP = 640
}
