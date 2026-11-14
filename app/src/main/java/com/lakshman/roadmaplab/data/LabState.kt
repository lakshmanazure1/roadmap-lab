package com.lakshman.roadmaplab.data

import com.lakshman.roadmaplab.domain.AppSettings
import com.lakshman.roadmaplab.domain.Assessment
import com.lakshman.roadmaplab.domain.Card
import com.lakshman.roadmaplab.domain.CardState
import com.lakshman.roadmaplab.domain.DayLog
import com.lakshman.roadmaplab.domain.DayMeta
import com.lakshman.roadmaplab.domain.DayPlan
import com.lakshman.roadmaplab.domain.Evidence
import com.lakshman.roadmaplab.domain.ItemEvent
import com.lakshman.roadmaplab.domain.ReviewLog
import com.lakshman.roadmaplab.domain.RoutineCheck
import com.lakshman.roadmaplab.domain.WeeklyReview

/** Everything stored, as plain models, plus the plan the app is following. */
data class LabState(
    val settings: AppSettings,
    val plan: DayPlan?,
    val planError: String?,
    val cards: Map<String, Card>,
    val cardStates: List<CardState>,
    val reviewLogs: List<ReviewLog>,
    val dayLogs: Map<Long, DayLog>,
    val events: List<ItemEvent>,
    val assessments: List<Assessment>,
    val ticks: List<RoutineCheck>,
    val evidence: List<Evidence>,
    val dayMeta: Map<Long, DayMeta>,
    val weeklyReviews: Map<Long, WeeklyReview>,
    /** Records kept from version 1 (the October roadmap), shown in Settings → Data. */
    val legacyTaskLogs: Int,
)
