package com.tracky.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    private val context: Context
) {

    companion object {
        private val DAILY_REPORT_ENABLED = booleanPreferencesKey("daily_report_enabled")
        private val DAILY_REPORT_HOUR = intPreferencesKey("daily_report_hour")
        private val DAILY_REPORT_MINUTE = intPreferencesKey("daily_report_minute")
        private val DARK_MODE = stringPreferencesKey("dark_mode")
        private val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
    }

    fun isDailyReportEnabled(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[DAILY_REPORT_ENABLED] ?: true
        }
    }

    suspend fun setDailyReportEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DAILY_REPORT_ENABLED] = enabled
        }
    }

    fun getReportHour(): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[DAILY_REPORT_HOUR] ?: 20
        }
    }

    fun getReportMinute(): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[DAILY_REPORT_MINUTE] ?: 0
        }
    }

    suspend fun setReportTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[DAILY_REPORT_HOUR] = hour
            preferences[DAILY_REPORT_MINUTE] = minute
        }
    }

    fun getDarkMode(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[DARK_MODE] ?: "system"
        }
    }

    suspend fun setDarkMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE] = mode
        }
    }

    fun isFirstLaunch(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[FIRST_LAUNCH] ?: true
        }
    }

    suspend fun setFirstLaunch(isFirst: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FIRST_LAUNCH] = isFirst
        }
    }
}
