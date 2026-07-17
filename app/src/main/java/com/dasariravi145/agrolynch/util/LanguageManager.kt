package com.dasariravi145.agrolynch.util

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
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
import kotlinx.coroutines.withTimeoutOrNull
import java.util.*
import timber.log.Timber

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
        return try {
            runBlocking {
                withTimeoutOrNull(2000) {
                    context.settingsDataStore.data.map { preferences ->
                        preferences[LANGUAGE_KEY] ?: "en"
                    }.first()
                } ?: "en"
            }
        } catch (e: Exception) {
            Timber.e(e, "LanguageManager: getLanguageCodeSync failed, falling back to 'en'")
            "en"
        }
    }

    suspend fun saveLanguageCode(context: Context, languageCode: String) {
        Timber.d("LANGUAGE_SAVE_STARTED: $languageCode")
        context.settingsDataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = languageCode
            preferences[IS_LANGUAGE_SELECTED] = true
        }
        Timber.d("LANGUAGE_SAVE_COMPLETED: $languageCode")
    }

    fun applyLocale(context: Context, languageCode: String): Context {
        Timber.tag("Language").d("Applying locale: $languageCode")
        val locale = if (languageCode.contains("-")) {
            val parts = languageCode.split("-")
            Locale.Builder().setLanguage(parts[0]).setRegion(parts[1]).build()
        } else {
            Locale(languageCode)
        }
        
        Locale.setDefault(locale)
        
        val resources = context.resources
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        
        // This is necessary to update the resources for the current context
        resources.updateConfiguration(config, resources.displayMetrics)
        
        val localizedContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            context.createConfigurationContext(config)
        } else {
            context
        }

        // Hilt's hiltViewModel() requires the context to be an Activity to create HiltViewModelFactory.
        // createConfigurationContext returns a ContextImpl on API 24+, which breaks Hilt.
        // We wrap it in a ContextWrapper that delegates to localizedContext for resources/logic
        // but returns the original context (the Activity) as baseContext for Hilt's findActivity() check.
        return object : ContextWrapper(localizedContext) {
            override fun getBaseContext(): Context = context
        }
    }
}
