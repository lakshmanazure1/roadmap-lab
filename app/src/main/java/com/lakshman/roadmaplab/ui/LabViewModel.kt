package com.lakshman.roadmaplab.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lakshman.roadmaplab.RoadmapLabApp
import com.lakshman.roadmaplab.data.LabRepository
import com.lakshman.roadmaplab.data.LabState
import com.lakshman.roadmaplab.domain.AppSettings
import com.lakshman.roadmaplab.domain.CardState
import com.lakshman.roadmaplab.domain.DayLog
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.Leitner
import com.lakshman.roadmaplab.domain.Motivation
import com.lakshman.roadmaplab.domain.Phase
import com.lakshman.roadmaplab.domain.PlanTask
import com.lakshman.roadmaplab.domain.Planner
import com.lakshman.roadmaplab.domain.Progress
import com.lakshman.roadmaplab.domain.ProgressInfo
import com.lakshman.roadmaplab.domain.Streak
import com.lakshman.roadmaplab.domain.TaskLog
import com.lakshman.roadmaplab.domain.TaskStatus
import com.lakshman.roadmaplab.domain.Wellbeing
import com.lakshman.roadmaplab.reminder.Reminders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LabViewModel(app: Application) : AndroidViewModel(app) {
    val repo: LabRepository = (app as RoadmapLabApp).repository
    private val today = MutableStateFlow(Days.today())

    val ui: StateFlow<UiState?> = combine(repo.state, today) { s, d -> derive(s, d) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            ui.filterNotNull().collect { u ->
                if (u.s.settings.onboarded && u.s.roadmap != null && u.s.dayPlans[u.today] == null) repo.ensurePlan(u.today)
            }
        }
    }

    fun refreshDay() {
        today.value = Days.today()
    }

    fun launch(block: suspend LabRepository.() -> Unit) {
        viewModelScope.launch { repo.block() }
    }

    fun updateSettings(change: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val s = repo.settings.update(change)
            Reminders.schedule(getApplication<Application>(), s)
        }
    }
}
