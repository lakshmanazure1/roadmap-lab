package com.lakshman.roadmaplab

import com.lakshman.roadmaplab.domain.Card
import com.lakshman.roadmaplab.domain.CardType
import com.lakshman.roadmaplab.domain.DayLog
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.Leitner
import com.lakshman.roadmaplab.domain.Motivation
import com.lakshman.roadmaplab.domain.Rating
import com.lakshman.roadmaplab.domain.Streak
import com.lakshman.roadmaplab.domain.Wellbeing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewAndStreakTest {
    private val mcq = Card("C1", "Python", CardType.MCQ, "Q?", listOf("a", "b", "c", "d"), "a", "why", "T1")

    @Test
    fun multipleChoiceBecomesFlashcardAfterTwoCorrect() {
        var s = Leitner.newState("C1", 100)
        assertTrue(Leitner.askAsMcq(mcq, s))
        s = Leitner.apply(s, Rating.GOOD, 100, asMcq = true)
        assertTrue(Leitner.askAsMcq(mcq, s))
        s = Leitner.apply(s, Rating.AGAIN, 101, asMcq = true) // wrong resets the count
        assertTrue(Leitner.askAsMcq(mcq, s))
        s = Leitner.apply(s, Rating.GOOD, 102, asMcq = true)
        s = Leitner.apply(s, Rating.GOOD, 104, asMcq = true)
        assertFalse(Leitner.askAsMcq(mcq, s))
    }

    @Test
    fun leitnerIntervals() {
        val s0 = Leitner.newState("C1", 100)
        val good = Leitner.apply(s0, Rating.GOOD, 100, false)
        assertEquals(1, good.box); assertEquals(101L, good.dueDay)
        val easy = Leitner.apply(good, Rating.EASY, 101, false)
        assertEquals(3, easy.box); assertEquals(105L, easy.dueDay)
        val hard = Leitner.apply(easy, Rating.HARD, 105, false)
        assertEquals(3, hard.box); assertEquals(107L, hard.dueDay)
        val again = Leitner.apply(hard, Rating.AGAIN, 107, false)
        assertEquals(1, again.box); assertEquals(108L, again.dueDay); assertEquals(1, again.lapses)
    }

    @Test
    fun reviewsCappedAt20AndMixed() {
        val cards = (1..30).associate { i ->
            val c = Card("C$i", if (i % 2 == 0) "Python" else "Accounting", CardType.MCQ, "", listOf("a", "b"), "a", "", null)
            c.id to c
        }
        val states = cards.keys.map { Leitner.newState(it, 100) }
        val q = Leitner.queue(cards, states, 100, reviewedToday = 0)
        assertEquals(20, q.size)
        val topics = q.map { cards.getValue(it.cardId).topic }
        assertTrue("mixed", topics.zipWithNext().all { (a, b) -> a != b })
        assertEquals(15, Leitner.queue(cards, states, 100, reviewedToday = 5).size)
    }

    @Test
    fun cardsUnlockOnlyAfterTheirTaskIsDone() {
        val cards = TestData.cards
        val tasks = TestData.roadmap.tasks.associateBy { it.id }
        val day9 = tasks.getValue("A-PY-09")
        val unlocked = Leitner.unlockOnDone(listOf(day9), cards, emptyList(), 200)
        assertTrue(unlocked.isNotEmpty())
        assertTrue(unlocked.all { s -> cards.first { it.id == s.cardId }.unlockedBy == "A-PY-09" })
        // Doing it again doesn't reset existing cards.
        assertTrue(Leitner.unlockOnDone(listOf(day9), cards, unlocked, 201).isEmpty())
    }

    @Test
    fun everyCardHasAnUnlockingTaskAndAbout150Cards() {
        val ids = TestData.roadmap.tasks.map { it.id }.toSet()
        assertTrue(TestData.cards.size in 140..180)
        assertTrue(TestData.cards.all { it.unlockedBy in ids })
        assertTrue(TestData.cards.filter { it.type == CardType.MCQ }.all { it.answer in it.options && it.options.size == 4 })
    }

    @Test
    fun streakWithOneFreeMissPerWeek() {
        val mon = Days.of("2026-11-16") // a Monday
        // Active Mon–Wed, missed Thu, active Fri. Today = Fri → 4 (Thu forgiven).
        val a = setOf(mon, mon + 1, mon + 2, mon + 4)
        assertEquals(4, Streak.current(a, mon + 4))
        // Two misses in one week: Thu is forgiven, Tue ends it → Fri + Wed = 2.
        assertEquals(2, Streak.current(setOf(mon, mon + 2, mon + 4), mon + 4))
        // Today not done yet doesn't break it.
        assertEquals(3, Streak.current(setOf(mon, mon + 1, mon + 2), mon + 3))
        // Nothing logged → 0.
        assertEquals(0, Streak.current(emptySet(), mon))
    }

    @Test
    fun lowMoodCardAfter7Of10LowDays() {
        val today = 1000L
        val logs = (0 until 10).map { DayLog(today - it, mood = if (it < 7) 2 else 4) }
        assertTrue(Wellbeing.lowMoodRun(logs, today))
        assertFalse(Wellbeing.lowMoodRun(logs.drop(1), today))
    }

    @Test
    fun motivationIsGentleOnLowEnergy() {
        val input = Motivation.Inputs(500, lowEnergy = true, phaseTitle = "X", phaseDone = 1, phaseTotal = 3,
            overallPercent = 10.0, nextMilestone = null, daysToMilestone = null, pastLines = emptyList())
        assertTrue(Motivation.lineFor(input).text in Motivation.GENTLE)
        val normal = Motivation.lineFor(input.copy(lowEnergy = false))
        assertTrue(normal.text.isNotBlank())
        assertFalse(normal.text.contains("behind"))
    }
}
