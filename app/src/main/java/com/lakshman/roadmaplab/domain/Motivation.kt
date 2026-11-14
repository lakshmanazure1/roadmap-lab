package com.lakshman.roadmaplab.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * One short line a day, mostly built from your own progress. Rotates through five kinds;
 * gentler on low-energy days; never guilt or "you're behind".
 */
object Motivation {
    enum class Kind { PROGRESS, MILESTONE, OWN_WORDS, SCIENCE, QUOTE }

    data class Line(val kind: Kind, val text: String, val source: String? = null)

    data class Inputs(
        val day: Long,
        val lowEnergy: Boolean,
        val phaseTitle: String?,
        val phaseDone: Int,
        val phaseTotal: Int,
        val overallPercent: Double,
        val nextMilestone: Milestone?,
        val daysToMilestone: Int?,
        /** (day, text) pairs from past notes and evidence "learning" lines. */
        val pastLines: List<Pair<Long, String>>,
    )

    private val dateFmt = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

    fun lineFor(input: Inputs): Line {
        if (input.lowEnergy) return gentle(input.day)
        val order = Kind.entries
        val start = Math.floorMod(input.day, order.size.toLong()).toInt()
        for (i in order.indices) {
            val line = build(order[(start + i) % order.size], input)
            if (line != null) return line
        }
        return science(input.day)
    }

    private fun build(kind: Kind, input: Inputs): Line? = when (kind) {
        Kind.PROGRESS -> {
            val total = input.phaseTotal
            val done = input.phaseDone
            if (input.phaseTitle != null && total > 0 && done < total) {
                val left = total - done
                Line(kind, "$done of $total tasks in “${input.phaseTitle}” done. $left more and the phase is complete.")
            } else if (input.overallPercent > 0) {
                Line(kind, String.format(Locale.ENGLISH, "%.1f %% of the whole plan is done. Every task moves the line.", input.overallPercent))
            } else null
        }
        Kind.MILESTONE -> {
            val m = input.nextMilestone
            val d = input.daysToMilestone
            when {
                m == null || d == null -> null
                d == 0 -> Line(kind, "${m.title}: today. You've prepared for this.")
                else -> Line(kind, "${m.title} in ${plural(d, "day")}. Today's task moves it forward.")
            }
        }
        Kind.OWN_WORDS -> {
            val candidates = input.pastLines.filter { it.first <= input.day - 2 && it.second.isNotBlank() }
            if (candidates.isEmpty()) null else {
                val (d, text) = candidates[Math.floorMod(input.day, candidates.size.toLong()).toInt()]
                Line(kind, "${LocalDate.ofEpochDay(d).format(dateFmt)}: ${text.trim().take(140)}", "your notes")
            }
        }
        Kind.SCIENCE -> science(input.day)
        Kind.QUOTE -> {
            val q = Quotes.ALL[Math.floorMod(input.day / 5, Quotes.ALL.size.toLong()).toInt()]
            Line(kind, "“${q.text}”", q.source)
        }
    }

    private fun science(day: Long): Line {
        val s = SCIENCE_NOTES[Math.floorMod(day / 5, SCIENCE_NOTES.size.toLong()).toInt()]
        return Line(Kind.SCIENCE, s)
    }

    private fun gentle(day: Long): Line =
        Line(Kind.PROGRESS, GENTLE[Math.floorMod(day, GENTLE.size.toLong()).toInt()])

    val GENTLE = listOf(
        "A 15-minute step still counts today.",
        "Low-energy day: do the small version. It keeps the line moving.",
        "Rest is part of the plan. One small step is enough today.",
    )

    val SCIENCE_NOTES = listOf(
        "Testing yourself beats rereading. That's why the evening starts with questions.",
        "Spacing beats cramming: each card comes back just before you'd forget it.",
        "Getting a card wrong isn't failure. Correcting it is when memory gets stronger.",
        "Mixing Python, accounting and SAP in one session feels harder, and works better.",
        "Try to recall before you look. The effort of remembering builds the memory.",
        "Deciding when and where you'll study (an if-then plan) makes you more likely to do it.",
        "Seeing small, real progress is one of the strongest everyday motivators.",
        "Explaining something in your own words shows you what you don't know yet.",
        "Sleep helps fix what you learned today into long-term memory.",
        "Short, regular sessions beat rare long ones.",
        "Rereading notes feels productive but is a weak way to learn. Turn notes into questions.",
        "Feedback right after an answer stops a mistake from sticking.",
    )
}
