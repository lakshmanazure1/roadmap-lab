package com.lakshman.roadmaplab.domain

enum class DayType(val label: String) {
    WEEKDAY("Weekday"),
    SATURDAY("Saturday"),
    SUNDAY("Sunday"),
    REDUCED("Reduced routine"),
    CATCHUP("Catch-up day"),
    REST("Planned rest day"),
    /** Outside the plan (the optional lead-in, or after 31 Jan): nothing required. */
    NONE("Nothing scheduled"),
}

enum class DayStatus(val label: String) {
    QUALIFIED("Minimum met"),
    MISSED("Minimum not met"),
    /** One missed day a week is forgiven: the streak pauses instead of resetting. */
    GRACE("Missed (forgiven: 1 a week)"),
    REST("Planned rest day"),
    NONE("Nothing scheduled"),
    TODAY("Today, still open"),
    FUTURE("Coming up"),
}

data class DayResult(val day: Long, val type: DayType, val minutes: Int, val target: Int, val status: DayStatus) {
    val qualified: Boolean get() = status == DayStatus.QUALIFIED
    /** A day that had work scheduled (counts in consistency %). */
    val scheduled: Boolean get() = type != DayType.REST && type != DayType.NONE
}

data class ConsistencyInfo(
    val days: List<DayResult>,
    val current: Int,
    val longest: Int,
    val activeThisWeek: Int,
    val scheduledThisWeek: Int,
    /** Percent of scheduled days that met the minimum in the last 7 / 28 days, or null with no scheduled days yet. */
    val rolling7: Double?,
    val rolling28: Double?,
    val graceUsedThisWeek: Boolean,
    /** Days since the last day that met the minimum (for a calm "welcome back"). */
    val daysSinceActive: Int?,
)

/**
 * The streak engine. Transparent rules:
 *  1. A day counts when the minutes of planned study you completed reach that day's minimum
 *     (you set the minimum per kind of day). Opening the app, or ticking a health habit, adds nothing.
 *  2. Planned rest days (25 Dec, 1 Jan and any you plan ahead) and days outside the plan are neutral:
 *     they neither count nor break the streak. Reduced-routine days have their own, smaller minimum.
 *  3. The first missed day in a Monday–Sunday week is forgiven: the streak pauses, it isn't reset.
 *  4. Today never breaks the streak while it is still open.
 *  5. Nothing is ever deleted: the longest streak and every past day stay in the history.
 */
object Consistency {
    fun dayType(day: Long, plan: DayPlan, meta: DayMeta?): DayType {
        if (!plan.inPlan(day)) return DayType.NONE
        if (plan.restDays.containsKey(day)) return DayType.REST
        when (meta?.kind) {
            DayKind.REST -> return DayType.REST
            DayKind.REDUCED -> return DayType.REDUCED
            DayKind.CATCHUP -> return DayType.CATCHUP
            null -> {}
        }
        return when (Days.dow(day)) {
            6 -> DayType.SATURDAY
            7 -> DayType.SUNDAY
            else -> DayType.WEEKDAY
        }
    }

    /**
     * Minutes of planned study done on each day: completed tasks (on the day they were completed),
     * partial work ("did part of it"), reduced-routine actions and flashcard practice.
     * Health, family and money tasks are tracked as habits and never count here.
     */
    fun minutesByDay(plan: DayPlan, states: Collection<ItemState>, checks: List<RoutineCheck>): Map<Long, Int> {
        val out = HashMap<Long, Int>()
        fun add(day: Long, m: Int) { if (m > 0) out[day] = (out[day] ?: 0) + m }
        for (s in states) {
            val t = plan.task(s.itemId) ?: continue
            if (!t.study) continue
            if (s.progress == Progress.COMPLETED && s.doneDay != null) add(s.doneDay, s.doneMinutes)
            for ((d, m) in s.partMinutes) add(d, m)
        }
        for (c in checks) if (c.done && c.routineId != SLEEP && !c.routineId.startsWith("red-life")) add(c.day, c.minutes)
        return out
    }

    /** The routine-check id for "slept 7 hours". */
    const val SLEEP = "sleep7"

    fun evaluate(
        plan: DayPlan,
        meta: Map<Long, DayMeta>,
        minutes: Map<Long, Int>,
        targets: Targets,
        today: Long,
    ): ConsistencyInfo {
        val first = minOf(plan.leadIn, minutes.keys.minOrNull() ?: plan.leadIn)
        val last = maxOf(plan.end, today)
        val days = ArrayList<DayResult>()
        val graceWeeks = HashSet<Long>()
        var d = first
        while (d <= last) {
            val type = dayType(d, plan, meta[d])
            val m = minutes[d] ?: 0
            val target = targets.of(type)
            val status = when {
                d > today -> DayStatus.FUTURE
                type == DayType.REST -> DayStatus.REST
                // Outside the plan nothing is required; a day there only counts if you choose to work.
                type == DayType.NONE -> if (m >= targets.weekday && targets.weekday > 0) DayStatus.QUALIFIED else DayStatus.NONE
                m >= target && target > 0 -> DayStatus.QUALIFIED
                d == today -> DayStatus.TODAY
                graceWeeks.add(Days.weekStart(d)) -> DayStatus.GRACE
                else -> DayStatus.MISSED
            }
            days.add(DayResult(d, type, m, target, status))
            d++
        }

        // Current streak: walk back from today through neutral days, stop at a miss.
        var current = 0
        for (r in days.asReversed()) {
            if (r.day > today) continue
            when (r.status) {
                DayStatus.QUALIFIED -> current++
                DayStatus.MISSED -> break
                else -> {}
            }
        }
        var longest = 0
        var run = 0
        for (r in days) {
            if (r.day > today) break
            when (r.status) {
                DayStatus.QUALIFIED -> { run++; longest = maxOf(longest, run) }
                DayStatus.MISSED -> run = 0
                else -> {}
            }
        }
        fun rolling(n: Int): Double? {
            // The window ends today if today already counts, otherwise yesterday (an open day isn't a miss).
            val todayDone = days.firstOrNull { it.day == today }?.qualified == true
            val end = if (todayDone) today else today - 1
            val window = days.filter { it.day in (end - n + 1)..end && it.scheduled }
            if (window.isEmpty()) return null
            return 100.0 * window.count { it.qualified } / window.size
        }
        val week = Days.weekStart(today)
        val thisWeek = days.filter { it.day in week..today }
        val lastActive = days.lastOrNull { it.qualified && it.day <= today }?.day
        return ConsistencyInfo(
            days = days,
            current = current,
            longest = longest,
            activeThisWeek = thisWeek.count { it.qualified },
            scheduledThisWeek = days.count { it.day in week..(week + 6) && it.scheduled },
            rolling7 = rolling(7),
            rolling28 = rolling(28),
            graceUsedThisWeek = thisWeek.any { it.status == DayStatus.GRACE },
            daysSinceActive = lastActive?.let { (today - it).toInt() },
        )
    }
}
