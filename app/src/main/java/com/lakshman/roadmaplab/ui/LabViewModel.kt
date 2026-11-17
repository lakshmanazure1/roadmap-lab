package com.lakshman.roadmaplab.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lakshman.roadmaplab.RoadmapLabApp
import com.lakshman.roadmaplab.data.LabRepository
import com.lakshman.roadmaplab.domain.AppSettings
import com.lakshman.roadmaplab.domain.Assessment
import com.lakshman.roadmaplab.domain.DayKind
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.EventType
import com.lakshman.roadmaplab.domain.Evidence
import com.lakshman.roadmaplab.domain.Rating
import com.lakshman.roadmaplab.domain.WeeklyReview
import com.lakshman.roadmaplab.reminder.Reminders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

/** A short message at the bottom of the screen, with an optional action (e.g. Undo). */
data class Note(val text: String, val action: String? = null, val onAction: (() -> Unit)? = null)

/** Everything a screen can ask for. Screens only see this, so they can be drawn in tests without a database. */
interface Actions {
    fun setTask(id: String, type: EventType, minutes: Int = 0, independent: Boolean = false, note: String = "", day: Long? = null)
    fun addAssessment(a: Assessment)
    fun tick(day: Long, id: String, done: Boolean, minutes: Int)
    fun saveEvidence(e: Evidence, onSaved: (Long) -> Unit = {})
    fun deleteEvidence(id: Long)
    fun setDayKind(day: Long, kind: DayKind?)
    fun setReducedWeek(from: Long, on: Boolean)
    fun setFocus(day: Long, id: String?)
    fun saveDayNote(day: Long, note: String)
    fun saveWeeklyReview(r: WeeklyReview, complete: Boolean)
    fun review(cardId: String, rating: Rating, asMcq: Boolean)
    fun addCard(topic: String, prompt: String, answer: String)
    fun updateSettings(change: (AppSettings) -> AppSettings)
    fun note(n: Note)
    /** Save a safety copy, clear every record, and count from today (Settings and the one-time card). */
    fun startFresh()
    fun setMood(day: Long, mood: Int?)
}

class LabViewModel(app: Application) : AndroidViewModel(app), Actions {
    val repo: LabRepository = (app as RoadmapLabApp).repository
    private val today = MutableStateFlow(Days.today())

    val ui: StateFlow<UiState?> = combine(repo.state, today) { s, d -> derive(s, d) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _notes = MutableSharedFlow<Note>(extraBufferCapacity = 4)
    val notes: SharedFlow<Note> = _notes

    init {
        // "Today" turns over at midnight in Kinshasa even if the app stays open.
        viewModelScope.launch {
            while (true) {
                val now = ZonedDateTime.now(Days.clock.withZone(Days.ZONE))
                val next = LocalDate.now(Days.clock.withZone(Days.ZONE)).plusDays(1).atStartOfDay(Days.ZONE)
                delay(Duration.between(now, next).toMillis().coerceAtLeast(1_000) + 1_000)
                today.value = Days.today()
            }
        }
    }

    fun refreshDay() {
        today.value = Days.today()
    }

    private fun safely(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                _notes.tryEmit(Note(e.message ?: "Couldn't save that. Please try again."))
            }
        }
    }

    override fun setTask(id: String, type: EventType, minutes: Int, independent: Boolean, note: String, day: Long?) =
        safely { repo.setTask(id, type, minutes, independent, note, day ?: Days.today()) }

    override fun addAssessment(a: Assessment) = safely { repo.addAssessment(a) }
    override fun tick(day: Long, id: String, done: Boolean, minutes: Int) = safely { repo.tick(day, id, done, minutes) }
    override fun saveEvidence(e: Evidence, onSaved: (Long) -> Unit) = safely { val id = repo.saveEvidence(e); onSaved(id) }
    override fun deleteEvidence(id: Long) = safely { repo.deleteEvidence(id) }
    override fun setDayKind(day: Long, kind: DayKind?) = safely { repo.setDayKind(day, kind) }
    override fun setReducedWeek(from: Long, on: Boolean) = safely { repo.setReducedWeek(from, on) }
    override fun setFocus(day: Long, id: String?) = safely { repo.setFocus(day, id) }
    override fun saveDayNote(day: Long, note: String) = safely { repo.saveDayNote(day, note) }
    override fun saveWeeklyReview(r: WeeklyReview, complete: Boolean) = safely { repo.saveWeeklyReview(r, complete) }
    override fun review(cardId: String, rating: Rating, asMcq: Boolean) = safely { repo.review(cardId, rating, asMcq) }
    override fun addCard(topic: String, prompt: String, answer: String) = safely { repo.addCard(topic, prompt, answer) }
    override fun note(n: Note) { _notes.tryEmit(n) }
    override fun setMood(day: Long, mood: Int?) = safely { repo.setMood(day, mood) }
    override fun startFresh() = safely {
        // In the plan's first week, day 1 is the plan's own first day (Tue 17 Nov), so the days since can still be ticked.
        val today = Days.today()
        val start = ui.value?.plan?.start
        val day = if (start != null && today <= start + 6) start else today
        repo.startFresh(com.lakshman.roadmaplab.BuildConfig.VERSION_NAME, day)
        refreshDay()
        _notes.tryEmit(Note("Fresh start: ${Days.short(day)} is day 1. A safety copy of the old records is kept in Settings."))
    }

    override fun updateSettings(change: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val s = repo.settings.update(change)
            Reminders.schedule(getApplication<Application>(), s)
        }
    }
}
