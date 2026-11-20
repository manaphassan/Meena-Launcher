package my.hana.meena

import android.appwidget.AppWidgetManager
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.data.preferences.SettingsRepository
import my.hana.meena.data.system.AppWidgetRepository
import my.hana.meena.data.system.FavouriteRules
import my.hana.meena.data.system.ShortcutRepository
import my.hana.meena.theme.MeenaLauncherTheme
import my.hana.meena.ui.components.MetroDialog
import my.hana.meena.ui.components.MetroMessage
import androidx.core.graphics.toColorInt

/**
 * Shown when another app asks the home screen to pin something: a shortcut such as a WhatsApp
 * chat ("add shortcut" in the app), or one of its widgets. Yes puts it on today; no drops it.
 * https://developer.android.com/reference/android/content/pm/LauncherApps.PinItemRequest
 */
class PinItemActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { finish(); return }
        val launcherApps = getSystemService(LauncherApps::class.java)
        val request = runCatching { launcherApps.getPinItemRequest(intent) }.getOrNull()
        if (request == null || !request.isValid) { finish(); return }

        val isShortcut = request.requestType == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT
        val label = if (isShortcut) {
            request.shortcutInfo?.let { (it.shortLabel ?: it.longLabel)?.toString() }
        } else {
            request.getAppWidgetProviderInfo(this)?.loadLabel(packageManager)
        }?.lowercase().orEmpty()

        val settingsRepository = SettingsRepository(applicationContext)
        setContent {
            val settings by settingsRepository.userSettingsFlow.collectAsState(initial = null)
            val loaded = settings ?: return@setContent
            val accent = runCatching { Color(loaded.accentHex.toColorInt()) }
                .getOrDefault(my.hana.meena.theme.MeenaCyan)
            val scope = rememberCoroutineScope()
            val fullMsg = stringResource(R.string.favourites_full)
            val failedMsg = stringResource(R.string.pin_failed)
            val addedMsg = stringResource(R.string.pin_added)
            MeenaLauncherTheme(baseTheme = loaded.baseTheme, accentColor = accent, fontScalePercent = loaded.fontScalePercent) {
                MetroDialog(
                    title = stringResource(if (isShortcut) R.string.pin_shortcut_title else R.string.pin_widget_title),
                    message = stringResource(
                        if (isShortcut) R.string.pin_shortcut_question else R.string.pin_widget_question,
                        label
                    ),
                    confirmText = stringResource(R.string.action_yes),
                    dismissText = stringResource(R.string.action_no),
                    onConfirm = {
                        scope.launch {
                            val message = withContext(Dispatchers.IO) {
                                val current = settingsRepository.userSettingsFlow.first()
                                if (isShortcut) acceptShortcut(request, current, settingsRepository, fullMsg, failedMsg, addedMsg)
                                else acceptWidget(request, current, settingsRepository, failedMsg, addedMsg)
                            }
                            MetroMessage.show(message)
                            finish()
                        }
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }

    private suspend fun acceptShortcut(
        request: LauncherApps.PinItemRequest,
        current: MeenaUserSettings,
        repo: SettingsRepository,
        fullMsg: String,
        failedMsg: String,
        addedMsg: String
    ): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return failedMsg
        val info = request.shortcutInfo ?: return failedMsg
        val key = FavouriteRules.shortcutKey(info.`package`, info.id)
        val alreadyThere = key in current.pinnedAppIds
        if (!alreadyThere && current.pinnedAppIds.size >= FavouriteRules.MAX) return fullMsg
        if (!runCatching { request.accept() }.getOrDefault(false)) return failedMsg
        ShortcutRepository.rememberPinned(this, info)
        if (!alreadyThere) repo.saveSettings(current.copy(pinnedAppIds = current.pinnedAppIds + key))
        return addedMsg
    }

    private suspend fun acceptWidget(
        request: LauncherApps.PinItemRequest,
        current: MeenaUserSettings,
        repo: SettingsRepository,
        failedMsg: String,
        addedMsg: String
    ): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return failedMsg
        val widgetId = runCatching { AppWidgetRepository.allocate(this) }.getOrNull() ?: return failedMsg
        // The requesting app has already set the widget up, so no setup screen is shown.
        val accepted = runCatching {
            request.accept(Bundle().apply { putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId) })
        }.getOrDefault(false)
        if (!accepted) {
            AppWidgetRepository.delete(this, widgetId)
            return failedMsg
        }
        repo.saveSettings(withHostedWidget(current, widgetId))
        return addedMsg
    }

    companion object {
        /** Adds a widget ID to today and turns the app widgets widget on, so it shows straight away. */
        fun withHostedWidget(settings: MeenaUserSettings, widgetId: Int): MeenaUserSettings {
            val configs = settings.widgetConfigs.toMutableMap()
            val key = "widget-app-widgets"
            val cfg = configs[key] ?: MeenaUserSettings.defaultWidgetMap().getValue(key)
            configs[key] = cfg.copy(isEnabled = true)
            return settings.copy(
                hostedWidgetIds = (settings.hostedWidgetIds + widgetId).distinct(),
                widgetConfigs = configs
            )
        }
    }
}
