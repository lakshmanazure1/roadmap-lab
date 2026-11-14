package com.lakshman.roadmaplab.data

import com.lakshman.roadmaplab.domain.AppSettings
import com.lakshman.roadmaplab.domain.Card
import com.lakshman.roadmaplab.domain.CardState
import com.lakshman.roadmaplab.domain.DayLog
import com.lakshman.roadmaplab.domain.ReviewLog
import com.lakshman.roadmaplab.domain.Roadmap
import com.lakshman.roadmaplab.domain.TaskLog

/** Everything stored, as plain models. */
data class LabState(
    val settings: AppSettings,
    val roadmap: Roadmap?,
    val cardsVersion: String,
    val cards: Map<String, Card>,
    val cardStates: List<CardState>,
    val taskLogs: List<TaskLog>,
    val dayLogs: Map<Long, DayLog>,
    val reviewLogs: List<ReviewLog>,
    val dayPlans: Map<Long, List<String>>,
)
