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
import com.lakshman.roadmaplab.domain.Targets
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
        val theme = stringPreferencesKey("theme_v2")
        val firstUseDay = longPreferencesKey("first_use_day")
        val lastBackupDay = longPreferencesKey("last_backup_day")
        val restartDismissedDay = longPreferencesKey("restart_dismissed_day")
        val lowMoodHiddenUntil = longPreferencesKey("low_mood_hidden_until")
        val showEvidenceLog = booleanPreferencesKey("show_evidence_log")
        val v2Intro = booleanPreferencesKey("v2_intro")
        val tWeekday = intPreferencesKey("target_weekday")
        val tSaturday = intPreferencesKey("target_saturday")
        val tSunday = intPreferencesKey("target_sunday")
        val tReduced = intPreferencesKey("target_reduced")
        val tCatchup = intPreferencesKey("target_catchup")
        val cue = stringPreferencesKey("cue")
        val weeklyReminder = booleanPreferencesKey("weekly_reminder")
        val freshStartDay = longPreferencesKey("fresh_start_day")
        val freshStartAsked = booleanPreferencesKey("fresh_start_asked")
    }

    /** Older looks ("soft", "classic") are the light theme now. */
    private fun theme(v: String?): String = when (v) {
        "dark", "system" -> v
        else -> "light"
    }

    private fun read(p: Preferences): AppSettings {
        val d = AppSettings()
        val t = Targets()
        return AppSettings(
            onboarded = p[K.onboarded] ?: d.onboarded,
            mode = Mode.of(p[K.mode]),
            reducedPlan = p[K.reducedPlan] ?: d.reducedPlan,
            reminderOn = p[K.reminderOn] ?: d.reminderOn,
            reminderHour = p[K.reminderHour] ?: d.reminderHour,
            reminderMinute = p[K.reminderMinute] ?: d.reminderMinute,
            theme = theme(p[K.theme]),
            firstUseDay = p[K.firstUseDay] ?: d.firstUseDay,
            lastBackupDay = p[K.lastBackupDay] ?: d.lastBackupDay,
            restartDismissedDay = p[K.restartDismissedDay] ?: d.restartDismissedDay,
            lowMoodHiddenUntil = p[K.lowMoodHiddenUntil] ?: d.lowMoodHiddenUntil,
            showEvidenceLog = p[K.showEvidenceLog] ?: d.showEvidenceLog,
            v2Intro = p[K.v2Intro] ?: d.v2Intro,
            targets = Targets(p[K.tWeekday] ?: t.weekday, p[K.tSaturday] ?: t.saturday, p[K.tSunday] ?: t.sunday,
                p[K.tReduced] ?: t.reduced, p[K.tCatchup] ?: t.catchup),
            cue = p[K.cue] ?: d.cue,
            weeklyReminder = p[K.weeklyReminder] ?: d.weeklyReminder,
            freshStartDay = p[K.freshStartDay] ?: d.freshStartDay,
            freshStartAsked = p[K.freshStartAsked] ?: d.freshStartAsked,
        )
    }

    val flow: Flow<AppSettings> = store.data.map { read(it) }

    suspend fun current(): AppSettings = flow.first()

    suspend fun update(change: (AppSettings) -> AppSettings): AppSettings {
        var result = AppSettings()
        store.edit { p ->
            val s = change(read(p))
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
            p[K.v2Intro] = s.v2Intro
            p[K.tWeekday] = s.targets.weekday
            p[K.tSaturday] = s.targets.saturday
            p[K.tSunday] = s.targets.sunday
            p[K.tReduced] = s.targets.reduced
            p[K.tCatchup] = s.targets.catchup
            p[K.cue] = s.cue
            p[K.weeklyReminder] = s.weeklyReminder
            p[K.freshStartDay] = s.freshStartDay
            p[K.freshStartAsked] = s.freshStartAsked
            result = s
        }
        return result
    }
}
