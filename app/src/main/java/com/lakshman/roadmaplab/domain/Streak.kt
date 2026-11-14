package com.lakshman.roadmaplab.domain

/**
 * Streak = days with any logged action. One free miss per week (Monday–Sunday) keeps it alive.
 * Today doesn't count as a miss until it's over.
 */
object Streak {
    fun activeDays(taskLogs: List<TaskLog>, reviewLogs: List<ReviewLog>, dayLogs: List<DayLog>): Set<Long> {
        val out = HashSet<Long>()
        for (l in taskLogs) if (l.status == TaskStatus.DONE || l.status == TaskStatus.PARTLY) out.add(l.day)
        for (r in reviewLogs) out.add(r.day)
        for (d in dayLogs) if (d.counts) out.add(d.day)
        return out
    }

    fun current(active: Set<Long>, today: Long): Int {
        var day = if (today in active) today else today - 1
        var count = 0
        val usedMissWeeks = HashSet<Long>()
        while (true) {
            if (day in active) {
                count++
                day--
                continue
            }
            val week = Days.weekStart(day)
            // A miss is forgiven once per week, and only when the streak continues just before it.
            val continuesBefore = (day - 1) in active || (day - 2) in active
            if (week in usedMissWeeks || !continuesBefore) break
            usedMissWeeks.add(week)
            day--
        }
        return count
    }
}
