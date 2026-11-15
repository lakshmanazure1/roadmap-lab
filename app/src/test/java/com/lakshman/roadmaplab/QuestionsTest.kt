package com.lakshman.roadmaplab

import com.lakshman.roadmaplab.TestData.plan
import com.lakshman.roadmaplab.domain.CheckSpec
import com.lakshman.roadmaplab.domain.Money
import com.lakshman.roadmaplab.domain.Privacy
import com.lakshman.roadmaplab.domain.Question
import com.lakshman.roadmaplab.domain.QuestionBank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuestionsTest {
    private fun quiz(id: String) = plan.task(id)!!.checks.filterIsInstance<CheckSpec.Quiz>().first()
    private fun build(id: String, seed: Int = 1) = QuestionBank.build(plan, plan.task(id)!!, quiz(id), TestData.cards, seed)

    @Test
    fun fridayFrenchQuizIsTheWeeksTenWordsBothDirections() {
        val qs = build("T025")
        assertEquals(10, qs.size)
        val week1 = plan.meta.french.first { it.week == 1 }.words.map { it.fr }.toSet()
        val asked = qs.map { (it as Question.Choice).let { c -> if (c.prompt.startsWith("What")) c.prompt.substringAfter("« ").substringBefore(" »") else c.answer } }
        assertTrue(asked.all { it in week1 })
        assertTrue(qs.any { it.prompt.startsWith("What does") } && qs.any { it.prompt.startsWith("In French") })
    }

    @Test
    fun whichSideQuizAsksDebitOrCredit() {
        val q = quiz("T109")
        assertEquals("side", q.bank)
        assertEquals(10, q.count)
        val qs = build("T109")
        assertEquals(10, qs.size)
        for (x in qs) {
            val c = x as Question.Choice
            assertEquals(setOf("Debit", "Credit"), c.options.toSet())
            assertTrue(c.answer == "Debit" || c.answer == "Credit")
        }
        assertEquals(10, qs.map { it.id }.toSet().size)
    }

    @Test
    fun translateBackBuildsTheModelSentences() {
        val qs = build("T015")
        assertEquals(5, qs.size)
        val o = qs.first() as Question.Order
        assertTrue(QuestionBank.isCorrect(o, o.answer.joinToString("\u0001")))
        assertFalse(QuestionBank.isCorrect(o, o.answer.reversed().joinToString("\u0001")))
    }

    @Test
    fun accountingChecksNeverAskAboutLaterTopics() {
        val qs = build("T014") // week 1
        assertEquals(5, qs.size)
        assertTrue(qs.none { it is Question.Journal }) // journal entries start in week 4
        val test1 = build("T164") // cumulative test #1, week 5
        assertEquals(10, test1.size)
        assertTrue(test1.all { it is Question.Journal || it.prompt.contains("GST") || it.prompt.contains("settlement") })
        val j = test1.filterIsInstance<Question.Journal>().first()
        assertTrue(j.credit in j.debitOptions) // the other side is always a wrong option
        assertTrue(QuestionBank.isCorrect(j, "${j.debit}|${j.credit}"))
        assertFalse(QuestionBank.isCorrect(j, "${j.credit}|${j.debit}"))
    }

    @Test
    fun sameSeedSameQuestionsNewSeedNewMix() {
        assertEquals(build("T164", 3).map { it.id }, build("T164", 3).map { it.id })
        assertTrue(build("T164", 3).map { it.id } != build("T164", 4).map { it.id })
    }

    @Test
    fun calculationsAreRight() {
        repeat(50) { s ->
            for (k in listOf("equation", "gst", "debits", "ledger")) {
                val q = QuestionBank.calcQuestion(k, Random(s)) as Question.Number
                assertTrue(q.answer > 0)
                assertTrue(QuestionBank.isCorrect(q, Money.inr(q.answer)))
                assertTrue(QuestionBank.isCorrect(q, q.answer.toString()))
            }
        }
        assertEquals("₹1,00,000", Money.inr(100_000))
        assertEquals("₹11,800", Money.inr(11_800))
    }

    @Test
    fun pythonChecksUseOnlyUnlockedTopics() {
        val qs = build("T029")
        assertTrue(qs.isNotEmpty())
        assertTrue(qs.none { it.prompt.contains("RAG") || it.prompt.contains("chunk") || it.prompt.contains("SELECT") })
    }

    @Test
    fun privacyWarnings() {
        assertNotNull(Privacy.warning("Vendor ABCDE1234F failed"))
        assertNotNull(Privacy.warning("GSTIN 27ABCDE1234F1Z5"))
        assertNotNull(Privacy.warning("acct 123456789012"))
        assertNotNull(Privacy.warning("call 9876543210"))
        assertNull(Privacy.warning("Vendor upload rejected about 40 rows; 2nd upload 0 errors"))
    }
}
