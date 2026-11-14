package com.lakshman.roadmaplab.ui

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Numbers always carry units: "74.2 %", "38 min", "Day 9 / 18".
object Fmt {
    private val en = Locale.ENGLISH
    private val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM yyyy", en)
    private val shortFmt = DateTimeFormatter.ofPattern("d MMM", en)
    private val fullFmt = DateTimeFormatter.ofPattern("d MMM yyyy", en)
    private val monthFmt = DateTimeFormatter.ofPattern("MMM", en)
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", en)

    fun pct(x: Double): String = String.format(en, "%.1f %%", x)
    fun pct0(x: Double): String = String.format(en, "%.0f %%", x)
    fun min(m: Int): String = "$m min"
    fun hours(minutes: Int): String = if (minutes < 60) "$minutes min" else String.format(en, "%.1f h", minutes / 60.0)

    fun day(day: Long): String = LocalDate.ofEpochDay(day).format(dayFmt)
    fun short(day: Long): String = LocalDate.ofEpochDay(day).format(shortFmt)
    fun full(day: Long): String = LocalDate.ofEpochDay(day).format(fullFmt)
    fun month(day: Long): String = LocalDate.ofEpochDay(day).format(monthFmt)
    fun range(a: Long, b: Long): String = "${short(a)} – ${full(b)}"
    fun time(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime().format(timeFmt)
    fun clock(hour: Int, minute: Int): String = LocalTime.of(hour, minute).format(timeFmt)

    /** "Log #042 · 14 Nov 2026 · 21:40" */
    fun logTitle(number: Int?, day: Long, finishedAt: Long?): String {
        val n = if (number != null) String.format(en, "Log #%03d", number) else "Log (open)"
        return listOfNotNull(n, full(day), finishedAt?.let { time(it) }).joinToString(" · ")
    }

    fun greeting(): String {
        val h = LocalTime.now().hour
        return when {
            h < 12 -> "Good morning"
            h < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    fun relDays(n: Int): String = when {
        n == 0 -> "today"
        n == 1 -> "in 1 day"
        n > 1 -> "in $n days"
        n == -1 -> "1 day ago"
        else -> "${-n} days ago"
    }
}
