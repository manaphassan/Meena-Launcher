package com.example.meenalauncher.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.meenalauncher.data.model.MeenaUserSettings
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
                json.decodeFromString<MeenaUserSettings>(rawJson)
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
            saveSettings(parsed)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun resetToFactoryDefaults() {
        saveSettings(MeenaUserSettings())
    }
}
