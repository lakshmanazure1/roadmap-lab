package com.lakshman.roadmaplab

import com.lakshman.roadmaplab.domain.Card
import com.lakshman.roadmaplab.domain.CardType
import com.lakshman.roadmaplab.domain.Leitner
import com.lakshman.roadmaplab.domain.Rating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeitnerTest {
    private val mcq = Card("C1", "Python", CardType.MCQ, "Q?", listOf("a", "b", "c", "d"), "a", "why", null)

    @Test
    fun multipleChoiceBecomesFlashcardAfterTwoCorrect() {
        var s = Leitner.newState("C1", 100)
        assertTrue(Leitner.askAsMcq(mcq, s))
        s = Leitner.apply(s, Rating.GOOD, 100, asMcq = true)
        s = Leitner.apply(s, Rating.AGAIN, 101, asMcq = true)
        assertTrue(Leitner.askAsMcq(mcq, s))
        s = Leitner.apply(s, Rating.GOOD, 102, asMcq = true)
        s = Leitner.apply(s, Rating.GOOD, 104, asMcq = true)
        assertFalse(Leitner.askAsMcq(mcq, s))
    }

    @Test
    fun leitnerIntervals() {
        val good = Leitner.apply(Leitner.newState("C1", 100), Rating.GOOD, 100, false)
        assertEquals(1, good.box); assertEquals(101L, good.dueDay)
        val easy = Leitner.apply(good, Rating.EASY, 101, false)
        assertEquals(3, easy.box); assertEquals(105L, easy.dueDay)
        val again = Leitner.apply(easy, Rating.AGAIN, 105, false)
        assertEquals(1, again.box); assertEquals(1, again.lapses)
    }

    @Test
    fun cardsUnlockWithTheirPlanTaskOnly() {
        val unlocks = TestData.plan.meta.cardUnlocks
        val fresh = Leitner.unlockForItems(setOf("T1023"), TestData.cards, unlocks, emptyList(), 200)
        assertTrue(fresh.isNotEmpty())
        assertTrue(fresh.all { unlocks[it.cardId] == "T1023" })
        assertTrue(Leitner.unlockForItems(setOf("T1023"), TestData.cards, unlocks, fresh, 201).isEmpty())
        // SAP cards unlock with their SAP Friday; asset accounting and the exam come after this plan, so those stay locked.
        assertEquals("T1058", unlocks["SAP-01"])
        assertTrue(listOf("SAP-16", "SAP-17", "SAP-18").none { unlocks.containsKey(it) })
    }
}
