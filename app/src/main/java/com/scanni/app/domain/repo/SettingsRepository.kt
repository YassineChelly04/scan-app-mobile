package com.scanni.app.domain.repo

import com.scanni.app.domain.model.AppSettings
import com.scanni.app.domain.model.OcrScript
import com.scanni.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun current(): AppSettings

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setDynamicColor(enabled: Boolean)

    suspend fun setAutoCapture(enabled: Boolean)

    suspend fun setOcrScript(script: OcrScript)
}
