package com.dasariravi145.agrolynch.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dasariravi145.agrolynch.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val languageKey = stringPreferencesKey("language_code")
    private val themeKey = booleanPreferencesKey("is_dark_mode")
    private val autoBackupKey = booleanPreferencesKey("auto_backup")

    override val languageCode: Flow<String> = context.settingsDataStore.data.map { it[languageKey] ?: "en" }
    override val isDarkMode: Flow<Boolean> = context.settingsDataStore.data.map { it[themeKey] ?: false }
    override val isAutoBackupEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[autoBackupKey] ?: true }

    override suspend fun updateLanguage(code: String) {
        context.settingsDataStore.edit { it[languageKey] = code }
    }

    override suspend fun updateTheme(isDark: Boolean) {
        context.settingsDataStore.edit { it[themeKey] = isDark }
    }

    override suspend fun updateAutoBackup(isEnabled: Boolean) {
        context.settingsDataStore.edit { it[autoBackupKey] = isEnabled }
    }
}
