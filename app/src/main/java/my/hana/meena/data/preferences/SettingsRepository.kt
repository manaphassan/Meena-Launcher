package my.hana.meena.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import my.hana.meena.data.model.MeenaUserSettings
import my.hana.meena.data.system.KeystoreSecretsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "meena_settings")

class SettingsRepository(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val KEY_SETTINGS_JSON = stringPreferencesKey("key_user_settings_json")

    val userSettingsFlow: Flow<MeenaUserSettings> = context.dataStore.data.map { preferences ->
        val rawJson = preferences[KEY_SETTINGS_JSON]
        if (rawJson != null) {
            try {
                json.decodeFromString<MeenaUserSettings>(rawJson).migrate()
            } catch (e: Exception) {
                MeenaUserSettings()
            }
        } else {
            MeenaUserSettings()
        }
    }

    suspend fun saveSettings(settings: MeenaUserSettings) {
        val serialized = json.encodeToString(settings)
        context.dataStore.edit { preferences ->
            preferences[KEY_SETTINGS_JSON] = serialized
        }
    }

    fun exportToJson(settings: MeenaUserSettings): String {
        return json.encodeToString(settings)
    }

    suspend fun importFromJson(jsonString: String): Boolean {
        return try {
            val parsed = json.decodeFromString<MeenaUserSettings>(jsonString)
            // Never keep a key in plain settings; old backups may still carry one.
            KeystoreSecretsManager.moveLegacyGeminiKey(context, parsed.geminiApiKey)
            saveSettings(parsed.copy(geminiApiKey = ""))
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Builds before the Keystore store saved the Gemini key in plain settings. Move it into
     * encrypted storage, and erase the plain copy only once the encrypted copy reads back.
     */
    suspend fun migrateLegacySecrets() {
        context.dataStore.edit { preferences ->
            val rawJson = preferences[KEY_SETTINGS_JSON] ?: return@edit
            val parsed = runCatching { json.decodeFromString<MeenaUserSettings>(rawJson) }.getOrNull() ?: return@edit
            if (parsed.geminiApiKey.isBlank()) return@edit
            if (KeystoreSecretsManager.moveLegacyGeminiKey(context, parsed.geminiApiKey)) {
                preferences[KEY_SETTINGS_JSON] = json.encodeToString(parsed.copy(geminiApiKey = ""))
            }
        }
    }

    suspend fun resetToFactoryDefaults() {
        saveSettings(MeenaUserSettings())
    }
}

