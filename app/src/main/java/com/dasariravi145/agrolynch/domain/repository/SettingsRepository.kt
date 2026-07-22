package com.dasariravi145.agrolynch.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val languageCode: Flow<String>
    val isDarkMode: Flow<Boolean>
    val isAutoBackupEnabled: Flow<Boolean>
    val lastRestoreInfo: Flow<String?>
    
    suspend fun updateLanguage(code: String)
    suspend fun updateTheme(isDark: Boolean)
    suspend fun updateAutoBackup(isEnabled: Boolean)
    suspend fun updateLastRestoreInfo(info: String?)
}
