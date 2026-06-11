package com.scanni.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.scanni.app.domain.model.AppSettings
import com.scanni.app.domain.model.OcrScript
import com.scanni.app.domain.model.ThemeMode
import com.scanni.app.domain.repo.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "scanni_settings",
)

class SettingsRepositoryImpl(context: Context) : SettingsRepository {

    private val dataStore = context.applicationContext.settingsDataStore

    override val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[KEY_THEME]?.toEnumOrNull<ThemeMode>() ?: ThemeMode.SYSTEM,
            dynamicColor = prefs[KEY_DYNAMIC_COLOR] ?: false,
            autoCapture = prefs[KEY_AUTO_CAPTURE] ?: true,
            ocrScript = prefs[KEY_OCR_SCRIPT]?.toEnumOrNull<OcrScript>() ?: OcrScript.LATIN,
        )
    }

    override suspend fun current(): AppSettings = settings.first()

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME] = mode.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setAutoCapture(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_CAPTURE] = enabled }
    }

    override suspend fun setOcrScript(script: OcrScript) {
        dataStore.edit { it[KEY_OCR_SCRIPT] = script.name }
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_AUTO_CAPTURE = booleanPreferencesKey("auto_capture")
        val KEY_OCR_SCRIPT = stringPreferencesKey("ocr_script")

        inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
            runCatching { enumValueOf<T>(this) }.getOrNull()
    }
}
