package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
    private val AUTO_DND = booleanPreferencesKey("auto_dnd")
    private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    private val AUTO_PLAY = booleanPreferencesKey("auto_play")
    private val READER_FONT = stringPreferencesKey("reader_font")
    private val DAILY_GOAL = intPreferencesKey("daily_tasbeeh_goal")
    private val TASBIH_TARGET = intPreferencesKey("tasbih_target")
    private val FAVORITES = stringSetPreferencesKey("favorite_zekr_ids")

    val fontSizeFlow: Flow<Float> = dataStore.data.map { it[FONT_SIZE] ?: 24f }
    val darkModeFlow: Flow<Boolean> = dataStore.data.map { it[DARK_MODE] ?: true }
    val vibrationFlow: Flow<Boolean> = dataStore.data.map { it[VIBRATION] ?: true }
    val keepScreenOnFlow: Flow<Boolean> = dataStore.data.map { it[KEEP_SCREEN_ON] ?: true }
    val autoDndFlow: Flow<Boolean> = dataStore.data.map { it[AUTO_DND] ?: false }
    val hideVirtuesFlow: Flow<Boolean> = dataStore.data.map { it[HIDE_VIRTUES] ?: false }
    val hideSourcesFlow: Flow<Boolean> = dataStore.data.map { it[HIDE_SOURCES] ?: false }
    val onboardingCompleteFlow: Flow<Boolean> = dataStore.data.map { it[ONBOARDING_COMPLETE] ?: false }
    val notificationsEnabledFlow: Flow<Boolean> = dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }
    val autoPlayFlow: Flow<Boolean> = dataStore.data.map { it[AUTO_PLAY] ?: false }
    val readerFontFlow: Flow<String> = dataStore.data.map { it[READER_FONT] ?: "amiri" }
    val dailyGoalFlow: Flow<Int> = dataStore.data.map { it[DAILY_GOAL] ?: 100 }
    val tasbihTargetFlow: Flow<Int> = dataStore.data.map { it[TASBIH_TARGET] ?: 33 }
    val favoritesFlow: Flow<Set<Int>> = dataStore.data.map { prefs ->
        prefs[FAVORITES]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

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

    suspend fun setFontSize(size: Float) { dataStore.edit { it[FONT_SIZE] = size } }
    suspend fun setDarkMode(isDark: Boolean) { dataStore.edit { it[DARK_MODE] = isDark } }
    suspend fun setVibration(enabled: Boolean) { dataStore.edit { it[VIBRATION] = enabled } }
    suspend fun setKeepScreenOn(enabled: Boolean) { dataStore.edit { it[KEEP_SCREEN_ON] = enabled } }
    suspend fun setAutoDnd(enabled: Boolean) { dataStore.edit { it[AUTO_DND] = enabled } }
    suspend fun setHideVirtues(hide: Boolean) { dataStore.edit { it[HIDE_VIRTUES] = hide } }
    suspend fun setOnboardingComplete() { dataStore.edit { it[ONBOARDING_COMPLETE] = true } }
    suspend fun setHideSources(hide: Boolean) { dataStore.edit { it[HIDE_SOURCES] = hide } }
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }
    suspend fun setAutoPlay(enabled: Boolean) { dataStore.edit { it[AUTO_PLAY] = enabled } }
    suspend fun setReaderFont(key: String) { dataStore.edit { it[READER_FONT] = key } }
    suspend fun setDailyGoal(goal: Int) { dataStore.edit { it[DAILY_GOAL] = goal.coerceIn(10, 100_000) } }
    suspend fun setTasbihTarget(target: Int) { dataStore.edit { it[TASBIH_TARGET] = target.coerceIn(0, 100_000) } }
    suspend fun toggleFavorite(id: Int) {
        dataStore.edit { prefs ->
            val current = prefs[FAVORITES] ?: emptySet()
            val key = id.toString()
            prefs[FAVORITES] = if (key in current) current - key else current + key
        }
    }
}
