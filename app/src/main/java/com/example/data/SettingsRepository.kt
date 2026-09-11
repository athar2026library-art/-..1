package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    
    private val FONT_SIZE = floatPreferencesKey("font_size")
    private val DARK_MODE = booleanPreferencesKey("dark_mode")
    private val VIBRATION = booleanPreferencesKey("vibration")
    
    private val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    private val HIDE_VIRTUES = booleanPreferencesKey("hide_virtues")
    private val HIDE_SOURCES = booleanPreferencesKey("hide_sources")
    
    private val LAST_READ_CATEGORY = stringPreferencesKey("last_read_category")
    private val LAST_READ_INDEX = intPreferencesKey("last_read_index")
    private val LAST_READ_REMAINING = intPreferencesKey("last_read_remaining")

    val fontSizeFlow: Flow<Float> = dataStore.data.map { it[FONT_SIZE] ?: 24f }
    val darkModeFlow: Flow<Boolean> = dataStore.data.map { it[DARK_MODE] ?: true }
    val vibrationFlow: Flow<Boolean> = dataStore.data.map { it[VIBRATION] ?: true }
    
    val keepScreenOnFlow: Flow<Boolean> = dataStore.data.map { it[KEEP_SCREEN_ON] ?: true }
    val hideVirtuesFlow: Flow<Boolean> = dataStore.data.map { it[HIDE_VIRTUES] ?: false }
    val hideSourcesFlow: Flow<Boolean> = dataStore.data.map { it[HIDE_SOURCES] ?: false }
    
    val lastReadCategoryFlow: Flow<String> = dataStore.data.map { it[LAST_READ_CATEGORY] ?: "" }
    val lastReadIndexFlow: Flow<Int> = dataStore.data.map { it[LAST_READ_INDEX] ?: 0 }
    val lastReadRemainingFlow: Flow<Int> = dataStore.data.map { it[LAST_READ_REMAINING] ?: 0 }

    suspend fun saveLastReadState(category: String, index: Int, remaining: Int) {
        dataStore.edit {
            it[LAST_READ_CATEGORY] = category
            it[LAST_READ_INDEX] = index
            it[LAST_READ_REMAINING] = remaining
        }
    }

    suspend fun clearLastReadState() {
        dataStore.edit {
            it.remove(LAST_READ_CATEGORY)
            it.remove(LAST_READ_INDEX)
            it.remove(LAST_READ_REMAINING)
        }
    }

    suspend fun setFontSize(size: Float) {
        dataStore.edit { it[FONT_SIZE] = size }
    }

    suspend fun setDarkMode(isDark: Boolean) {
        dataStore.edit { it[DARK_MODE] = isDark }
    }
    
    suspend fun setVibration(enabled: Boolean) {
        dataStore.edit { it[VIBRATION] = enabled }
    }
    
    suspend fun setKeepScreenOn(enabled: Boolean) {
        dataStore.edit { it[KEEP_SCREEN_ON] = enabled }
    }

    suspend fun setHideVirtues(hide: Boolean) {
        dataStore.edit { it[HIDE_VIRTUES] = hide }
    }

    suspend fun setHideSources(hide: Boolean) {
        dataStore.edit { it[HIDE_SOURCES] = hide }
    }
}
