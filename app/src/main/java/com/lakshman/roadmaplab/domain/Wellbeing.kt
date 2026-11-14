package com.lakshman.roadmaplab.domain

/** Gentle checks on mood and energy. Nothing here is a diagnosis, and nothing is shared. */
object Wellbeing {
    fun isLowEnergy(energy: Int?): Boolean = energy != null && energy <= 2

    /** True when mood was 1–2 on at least 7 of the last 10 days (today included). */
    fun lowMoodRun(dayLogs: List<DayLog>, today: Long): Boolean {
        val window = (today - 9)..today
        val low = dayLogs.count { it.day in window && it.mood != null && it.mood <= 2 }
        return low >= 7
    }
}
