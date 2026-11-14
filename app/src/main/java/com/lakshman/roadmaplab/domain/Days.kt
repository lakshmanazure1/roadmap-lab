package com.lakshman.roadmaplab.domain

import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * All dates in the app are "epoch days" in the Africa/Kinshasa time zone, as the plan asks.
 * A phone set to India time still sees the same Kinshasa day, so travel never splits or merges days.
 */
object Days {
    val ZONE: ZoneId = ZoneId.of("Africa/Kinshasa")

    /** Replaceable in tests. */
    @Volatile
    var clock: Clock = Clock.system(ZONE)

    fun today(): Long = LocalDate.now(clock.withZone(ZONE)).toEpochDay()

    fun nowMillis(): Long = clock.millis()

    /** The Kinshasa day a moment belongs to. */
    fun dayOf(epochMillis: Long): Long = Instant.ofEpochMilli(epochMillis).atZone(ZONE).toLocalDate().toEpochDay()

    fun of(isoDate: String): Long = LocalDate.parse(isoDate).toEpochDay()

    fun iso(day: Long): String = LocalDate.ofEpochDay(day).toString()

    fun date(day: Long): LocalDate = LocalDate.ofEpochDay(day)

    /** Monday of the week containing [day]. */
    fun weekStart(day: Long): Long = LocalDate.ofEpochDay(day).with(DayOfWeek.MONDAY).toEpochDay()

    /** 1 = Monday … 7 = Sunday. */
    fun dow(day: Long): Int = LocalDate.ofEpochDay(day).dayOfWeek.value

    fun isSunday(day: Long): Boolean = dow(day) == 7

    private val SHORT = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
    private val LONG = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH)
    private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

    /** "Mon 16 Nov" */
    fun short(day: Long): String = date(day).format(SHORT)

    /** "Monday 16 November" */
    fun long(day: Long): String = date(day).format(LONG)

    /** "16 Nov" */
    fun dayMonth(day: Long): String = date(day).format(DAY_MONTH)

    /** "today", "yesterday", "tomorrow", "in 3 days", "4 days ago" */
    fun relative(day: Long, today: Long): String = when (val d = day - today) {
        0L -> "today"
        -1L -> "yesterday"
        1L -> "tomorrow"
        else -> if (d > 0) "in $d days" else "${-d} days ago"
    }
}

/** "1 session", "2 sessions" */
fun plural(n: Int, word: String, many: String = word + "s"): String = "$n ${if (n == 1) word else many}"
