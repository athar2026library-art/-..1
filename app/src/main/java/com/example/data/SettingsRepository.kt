package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    
    private val FONT_SIZE = floatPreferencesKey("font_size")
    private val DARK_MODE = booleanPreferencesKey("dark_mode")
    private val VIBRATION = booleanPreferencesKey("vibration")

    val fontSizeFlow: Flow<Float> = dataStore.data.map { preferences ->
        preferences[FONT_SIZE] ?: 24f
    }

    val darkModeFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DARK_MODE] ?: true
    }
    
    val vibrationFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[VIBRATION] ?: true
    }

    suspend fun setFontSize(size: Float) {
        dataStore.edit { preferences ->
            preferences[FONT_SIZE] = size
        }
    }

    suspend fun setDarkMode(isDark: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_MODE] = isDark
        }
    }
    
    suspend fun setVibration(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[VIBRATION] = enabled
        }
    }
}
