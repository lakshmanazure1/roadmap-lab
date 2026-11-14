package com.lakshman.roadmaplab.domain

import java.time.DayOfWeek
import java.time.LocalDate

object Days {
    fun today(): Long = LocalDate.now().toEpochDay()

    fun of(isoDate: String): Long = LocalDate.parse(isoDate).toEpochDay()

    fun iso(day: Long): String = LocalDate.ofEpochDay(day).toString()

    /** Monday of the week containing [day]. */
    fun weekStart(day: Long): Long = LocalDate.ofEpochDay(day).with(DayOfWeek.MONDAY).toEpochDay()

    fun isSunday(day: Long): Boolean = LocalDate.ofEpochDay(day).dayOfWeek == DayOfWeek.SUNDAY
}
