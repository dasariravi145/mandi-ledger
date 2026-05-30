package com.dasariravi145.agrolynch.util

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dasariravi145.agrolynch.data.repository.settingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.*

object LanguageManager {
    private val LANGUAGE_KEY = stringPreferencesKey("language_code")
    private val IS_LANGUAGE_SELECTED = booleanPreferencesKey("is_language_selected")

    fun getLanguageCode(context: Context): Flow<String> {
        return context.settingsDataStore.data.map { preferences ->
            preferences[LANGUAGE_KEY] ?: "en"
        }
    }

    fun isLanguageSelected(context: Context): Flow<Boolean> {
        return context.settingsDataStore.data.map { preferences ->
            preferences[IS_LANGUAGE_SELECTED] ?: false
        }
    }

    fun getLanguageCodeSync(context: Context): String {
        return runBlocking {
            context.settingsDataStore.data.map { preferences ->
                preferences[LANGUAGE_KEY] ?: "en"
            }.first()
        }
    }

    suspend fun saveLanguageCode(context: Context, languageCode: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = languageCode
            preferences[IS_LANGUAGE_SELECTED] = true
        }
    }

    fun applyLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        
        // This is necessary to update the resources for the current context
        resources.updateConfiguration(config, resources.displayMetrics)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            context.createConfigurationContext(config)
        } else {
            context
        }
    }
}
