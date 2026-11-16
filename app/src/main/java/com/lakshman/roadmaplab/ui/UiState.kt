package com.lakshman.roadmaplab.ui

import com.lakshman.roadmaplab.data.LabState
import com.lakshman.roadmaplab.domain.Analytics
import com.lakshman.roadmaplab.domain.AreaProgress
import com.lakshman.roadmaplab.domain.Assessment
import com.lakshman.roadmaplab.domain.Consistency
import com.lakshman.roadmaplab.domain.ConsistencyInfo
import com.lakshman.roadmaplab.domain.DayKind
import com.lakshman.roadmaplab.domain.DayPlan
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.HabitWeek
import com.lakshman.roadmaplab.domain.ItemState
import com.lakshman.roadmaplab.domain.Leitner
import com.lakshman.roadmaplab.domain.Level
import com.lakshman.roadmaplab.domain.Mastery
import com.lakshman.roadmaplab.domain.NextAction
import com.lakshman.roadmaplab.domain.PlanTask
import com.lakshman.roadmaplab.domain.Progress
import com.lakshman.roadmaplab.domain.TodayEngine
import com.lakshman.roadmaplab.domain.TodayInput
import com.lakshman.roadmaplab.domain.TodayPlan
import com.lakshman.roadmaplab.domain.WeekBar

/** Everything the screens show, worked out from your records in one pure function (tested on the computer). */
data class UiState(
    val s: LabState,
    val plan: DayPlan,
    val today: Long,
    val states: Map<String, ItemState>,
    val levels: Map<String, Level>,
    val checksByItem: Map<String, List<Assessment>>,
    val reducedWeeks: Set<Long>,
    val minutes: Map<Long, Int>,
    val consistency: ConsistencyInfo,
    val todayPlan: TodayPlan,
    val next: NextAction,
    val areas: List<AreaProgress>,
    val weeks: List<WeekBar>,
    val months: List<Analytics.MonthTotal>,
    val dueCards: Int,
    val reviewedToday: Int,
    val weak: List<Mastery.TopicScore>,
    val needsRevision: List<PlanTask>,
    val milestones: List<PlanTask>,
    val conversations: Pair<Int, Int>,
    val logged: Pair<Int, Int>,
    val habitsThisWeek: HabitWeek,
    val sleep: Map<Long, Boolean>,
    /** Show the one-time "Start fresh from today?" card: there are records and you haven't answered yet. */
    val offerFreshStart: Boolean = false,
) {
    val totalDone: Int get() = states.values.count { it.progress == Progress.COMPLETED && plan.task(it.itemId) != null }
    val dueByToday: Int get() = plan.tasks.count {
        TodayEngine.effectiveDay(it, reducedWeeks) <= today && it.kind != com.lakshman.roadmaplab.domain.TaskKind.REST && !it.optional &&
            !(it.kind == com.lakshman.roadmaplab.domain.TaskKind.DAILY && it.day < plan.freshStart)
    }
    fun level(id: String): Level = levels[id] ?: Level.NOT_STARTED

    /** The week's focus; while your Python recap (PR1–PR7) is open, that's the focus. */
    fun weekFocus(week: Int): String? =
        if (plan.tasks.any { it.seq == DayPlan.RECAP && states[it.id]?.progress != Progress.COMPLETED }) "Python recap + accounting"
        else plan.meta.weekFocus[week]
    fun checks(id: String): List<Assessment> = checksByItem[id].orEmpty()
}

fun derive(s: LabState, today: Long): UiState? {
    // After a fresh start, days before it are "nothing scheduled", never "missed".
    val plan = (s.plan ?: return null).let { p -> if (s.settings.freshStartDay > 0) p.withFreshStart(s.settings.freshStartDay) else p }
    val states = Mastery.states(s.events)
    val checksByItem = s.assessments.filter { it.itemId != null }.groupBy { it.itemId!! }
    val levels = plan.tasks.associate { t -> t.id to Mastery.level(t.area, states[t.id], checksByItem[t.id].orEmpty()) }
    val reducedWeeks = s.dayMeta.values.filter { it.kind == DayKind.REDUCED }.groupBy { Days.weekStart(it.day) }
        .filterValues { it.size >= 3 }.keys
    val minutes = Consistency.minutesByDay(plan, states.values, s.ticks)
    val consistency = Consistency.evaluate(plan, s.dayMeta, minutes, s.settings.targets, today)
    val reviewedToday = s.reviewLogs.count { it.day == today }
    val dueCards = Leitner.queue(s.cards, s.cardStates, today, reviewedToday).size
    val todayTicks = s.ticks.filter { it.day == today }.associateBy { it.routineId }
    val todayPlan = TodayEngine.build(
        TodayInput(plan, states, todayTicks, s.dayMeta[today], reducedWeeks, dueCards, minutes[today] ?: 0, s.settings.targets), today,
    )
    val next = Analytics.nextAction(plan, todayPlan, levels, s.assessments, dueCards, consistency.daysSinceActive, today)
    val sleep = s.ticks.filter { it.routineId == Consistency.SLEEP }.associate { it.day to it.done }
    return UiState(
        s = s, plan = plan, today = today, states = states, levels = levels, checksByItem = checksByItem,
        reducedWeeks = reducedWeeks, minutes = minutes, consistency = consistency, todayPlan = todayPlan, next = next,
        areas = Analytics.areaProgress(plan, states, levels, s.assessments, reducedWeeks, today),
        weeks = Analytics.weekBars(plan, states, consistency.days, today),
        months = Analytics.months(plan, states, consistency.days),
        dueCards = dueCards, reviewedToday = reviewedToday,
        weak = Mastery.weakTopics(s.assessments),
        needsRevision = levels.filterValues { it == Level.NEEDS_REVISION }.keys.mapNotNull { plan.task(it) }.sortedBy { it.order },
        milestones = plan.tasks.filter { it.isMilestone && it.day >= today && states[it.id]?.progress != Progress.COMPLETED }.take(3),
        conversations = Analytics.conversations(plan, states, s.assessments),
        logged = Analytics.loggedWorkingDays(plan, states, today),
        habitsThisWeek = Analytics.habits(plan, Days.weekStart(today), states, sleep, today),
        sleep = sleep,
        offerFreshStart = !s.settings.freshStartAsked && s.settings.freshStartDay == 0L &&
            (s.events.isNotEmpty() || s.assessments.isNotEmpty() || s.evidence.isNotEmpty() || s.cardStates.isNotEmpty() ||
                s.ticks.isNotEmpty() || s.dayLogs.isNotEmpty() || s.legacyTaskLogs > 0),
    )
}
