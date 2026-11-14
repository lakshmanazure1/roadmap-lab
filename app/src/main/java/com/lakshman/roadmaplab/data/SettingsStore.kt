package com.lakshman.roadmaplab.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lakshman.roadmaplab.domain.AppSettings
import com.lakshman.roadmaplab.domain.Mode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.labSettings: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Small settings kept in DataStore (mode, reminder, theme…). */
class SettingsStore(context: Context) {
    private val store = context.applicationContext.labSettings

    private object K {
        val onboarded = booleanPreferencesKey("onboarded")
        val mode = stringPreferencesKey("mode")
        val reducedPlan = booleanPreferencesKey("reduced_plan")
        val reminderOn = booleanPreferencesKey("reminder_on")
        val reminderHour = intPreferencesKey("reminder_hour")
        val reminderMinute = intPreferencesKey("reminder_minute")
        val theme = stringPreferencesKey("theme")
        val firstUseDay = longPreferencesKey("first_use_day")
        val lastBackupDay = longPreferencesKey("last_backup_day")
        val restartDismissedDay = longPreferencesKey("restart_dismissed_day")
        val lowMoodHiddenUntil = longPreferencesKey("low_mood_hidden_until")
        val showEvidenceLog = booleanPreferencesKey("show_evidence_log")
    }

    val flow: Flow<AppSettings> = store.data.map { p ->
        val d = AppSettings()
        AppSettings(
            onboarded = p[K.onboarded] ?: d.onboarded,
            mode = Mode.of(p[K.mode]),
            reducedPlan = p[K.reducedPlan] ?: d.reducedPlan,
            reminderOn = p[K.reminderOn] ?: d.reminderOn,
            reminderHour = p[K.reminderHour] ?: d.reminderHour,
            reminderMinute = p[K.reminderMinute] ?: d.reminderMinute,
            theme = p[K.theme] ?: d.theme,
            firstUseDay = p[K.firstUseDay] ?: d.firstUseDay,
            lastBackupDay = p[K.lastBackupDay] ?: d.lastBackupDay,
            restartDismissedDay = p[K.restartDismissedDay] ?: d.restartDismissedDay,
            lowMoodHiddenUntil = p[K.lowMoodHiddenUntil] ?: d.lowMoodHiddenUntil,
            showEvidenceLog = p[K.showEvidenceLog] ?: d.showEvidenceLog,
        )
    }

    suspend fun current(): AppSettings = flow.first()

    suspend fun update(change: (AppSettings) -> AppSettings): AppSettings {
        var result = AppSettings()
        store.edit { p ->
            val old = AppSettings(
                onboarded = p[K.onboarded] ?: false,
                mode = Mode.of(p[K.mode]),
                reducedPlan = p[K.reducedPlan] ?: false,
                reminderOn = p[K.reminderOn] ?: false,
                reminderHour = p[K.reminderHour] ?: 21,
                reminderMinute = p[K.reminderMinute] ?: 0,
                theme = p[K.theme] ?: "dark",
                firstUseDay = p[K.firstUseDay] ?: 0,
                lastBackupDay = p[K.lastBackupDay] ?: 0,
                restartDismissedDay = p[K.restartDismissedDay] ?: 0,
                lowMoodHiddenUntil = p[K.lowMoodHiddenUntil] ?: 0,
                showEvidenceLog = p[K.showEvidenceLog] ?: true,
            )
            val s = change(old)
            p[K.onboarded] = s.onboarded
            p[K.mode] = s.mode.key
            p[K.reducedPlan] = s.reducedPlan
            p[K.reminderOn] = s.reminderOn
            p[K.reminderHour] = s.reminderHour
            p[K.reminderMinute] = s.reminderMinute
            p[K.theme] = s.theme
            p[K.firstUseDay] = s.firstUseDay
            p[K.lastBackupDay] = s.lastBackupDay
            p[K.restartDismissedDay] = s.restartDismissedDay
            p[K.lowMoodHiddenUntil] = s.lowMoodHiddenUntil
            p[K.showEvidenceLog] = s.showEvidenceLog
            result = s
        }
        return result
    }
}
