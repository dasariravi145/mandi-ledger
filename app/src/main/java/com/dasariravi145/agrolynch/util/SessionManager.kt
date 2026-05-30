package com.dasariravi145.agrolynch.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

class SessionManager(private val context: Context) {
    companion object {
        private val PIN_KEY = stringPreferencesKey("user_pin")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
    }

    val userPin: Flow<String?> = context.authDataStore.data.map { it[PIN_KEY] }
    val userId: Flow<String?> = context.authDataStore.data.map { it[USER_ID_KEY] }

    suspend fun saveSession(uid: String, pin: String) {
        context.authDataStore.edit { prefs ->
            prefs[USER_ID_KEY] = uid
            prefs[PIN_KEY] = pin
        }
    }

    suspend fun clearSession() {
        context.authDataStore.edit { it.clear() }
    }
}
