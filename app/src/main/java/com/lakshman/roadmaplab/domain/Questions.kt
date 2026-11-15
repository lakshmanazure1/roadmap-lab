package com.lakshman.roadmaplab.domain

import kotlin.math.roundToLong
import kotlin.random.Random

/** One question in an in-app check. All are marked by the app. */
sealed interface Question {
    val id: String
    val prompt: String
    val explanation: String

    data class Choice(
        override val id: String,
        override val prompt: String,
        val options: List<String>,
        val answer: String,
        override val explanation: String,
    ) : Question

    /** Pick the account to debit, then the account to credit. Both must be right. */
    data class Journal(
        override val id: String,
        override val prompt: String,
        val debitOptions: List<String>,
        val debit: String,
        val creditOptions: List<String>,
        val credit: String,
        override val explanation: String,
    ) : Question

    /** Type a number (₹). */
    data class Number(
        override val id: String,
        override val prompt: String,
        val answer: Long,
        override val explanation: String,
    ) : Question

    /** Put the words in order: the French "translate back" exercise. */
    data class Order(
        override val id: String,
        override val prompt: String,
        val tokens: List<String>,
        val answer: List<String>,
        override val explanation: String,
    ) : Question
}

/**
 * Builds in-app checks from the plan's banks, using only topics already reached (a check never asks
 * about next week). All practice data is fictional (Ravi Traders, made-up amounts). The same seed gives
 * the same questions, so rotating the phone can't reshuffle them; a retake uses a new seed.
 */
object QuestionBank {
    fun build(plan: DayPlan, task: PlanTask, spec: CheckSpec.Quiz, cards: Collection<Card>, seed: Int): List<Question> {
        val rnd = Random(seed * 31 + task.id.hashCode())
        return when (spec.bank) {
            "accounting" -> accounting(plan, task, spec.count, cards, rnd, coldQuiz = task.learn.contains("Quiz") || task.learn.contains("check:"))
            "journal" -> journal(plan, task.week, spec.count, rnd)
            "side" -> side(plan, task.week, spec.count, rnd)
            "python" -> python(plan, task, spec.count, cards, rnd)
            "french" -> frenchWords(plan, spec, rnd)
            "french-sentences" -> frenchSentences(plan, spec, rnd)
            else -> emptyList()
        }
    }

    /** Is [answer] right? Journal answers are "debit|credit"; Order answers are words joined by \u0001. */
    fun isCorrect(q: Question, answer: String): Boolean = when (q) {
        is Question.Choice -> answer == q.answer
        is Question.Journal -> answer == "${q.debit}|${q.credit}"
        is Question.Number -> parseAmount(answer)?.let { kotlin.math.abs(it - q.answer) <= 1 } == true
        is Question.Order -> answer.split('\u0001') == q.answer
    }

    fun parseAmount(text: String): Long? {
        val clean = text.replace("₹", "").replace(",", "").replace(" ", "").trim()
        return clean.toDoubleOrNull()?.roundToLong()
    }

    // ------------------------------------------------------------------ accounting

    private fun cardWeek(plan: DayPlan, cardId: String): Int? = plan.meta.cardUnlocks[cardId]?.let { plan.task(it)?.week }

    private fun accounting(plan: DayPlan, task: PlanTask, count: Int, cards: Collection<Card>, rnd: Random, coldQuiz: Boolean): List<Question> {
        val week = maxOf(1, task.week)
        // A Monday cold quiz is on the previous week; other checks favour this week's topic.
        val focus = if (coldQuiz) week - 1 else week
        val tagged = ArrayList<Pair<Int, Question>>()
        for (b in plan.meta.accountingQuestions) if (b.week <= week) tagged += b.week to choice("q:${b.id}", b, rnd)
        for (t in plan.meta.journal) if (t.week <= week) tagged += t.week to journalQ(plan, t, rnd)
        for (c in cards) {
            if (c.topic != "Accounting" || c.type != CardType.MCQ || c.options.size < 2) continue
            val w = cardWeek(plan, c.id) ?: continue
            if (w <= week) tagged += w to Question.Choice("card:${c.id}", c.prompt, c.options.shuffled(rnd), c.answer, c.explanation)
        }
        val out = ArrayList<Question>()
        val calcs = plan.meta.calcs.filter { it.second <= week }
        calcs.firstOrNull { it.second == focus }?.let { out += calcQuestion(it.first, rnd) }
        out += tagged.filter { it.first == focus }.map { it.second }.shuffled(rnd).take(count - out.size)
        if (out.size < count) out += tagged.filter { it.first != focus }.map { it.second }.shuffled(rnd).take(count - out.size)
        while (out.size < count && calcs.isNotEmpty()) out += calcQuestion(calcs.random(rnd).first, rnd)
        return out.take(count)
    }

    /** "Which side?" (T109): does this change go on the debit or the credit side? Ravi Traders, made up. */
    private val MOVES = listOf(
        "Ravi Traders' cash increases" to "Debit",
        "Ravi Traders' cash decreases" to "Credit",
        "The bank balance increases" to "Debit",
        "The bank balance decreases" to "Credit",
        "Ravi Traders buys shop furniture: the Furniture account increases" to "Debit",
        "Ravi Traders takes a bank loan: the Bank loan account increases" to "Credit",
        "Part of the bank loan is repaid: the Bank loan account decreases" to "Debit",
        "Ravi brings more money into the business: Capital increases" to "Credit",
        "Ravi takes cash home for personal use: Drawings" to "Debit",
        "Shop rent is paid: the Rent expense" to "Debit",
        "Staff salaries are paid: the Salaries expense" to "Debit",
        "Goods are sold: Sales income" to "Credit",
        "A customer now owes Ravi Traders more" to "Debit",
        "A customer pays what they owe: the customer's balance decreases" to "Credit",
        "Ravi Traders now owes a supplier more" to "Credit",
        "Ravi Traders pays a supplier: the supplier's balance decreases" to "Debit",
        "The bank pays interest to Ravi Traders: Interest received" to "Credit",
        "Goods are bought for resale: Purchases" to "Debit",
    )
    private const val SIDE_RULE = "Assets, expenses and drawings go up on the debit side; liabilities, capital and income go up on the credit side. A decrease goes on the opposite side."

    fun side(plan: DayPlan, week: Int, count: Int, rnd: Random): List<Question> {
        val pool = ArrayList<Question>()
        MOVES.forEachIndexed { i, (what, side) ->
            pool += Question.Choice("side:$i", "Which side? $what", listOf("Debit", "Credit"), side, "$side. $SIDE_RULE")
        }
        // Both accounts of an entry already taught: "In this entry, is Cash debited or credited?"
        for (t in plan.meta.journal.filter { it.week <= maxOf(4, week) }) {
            val (acct, side) = if (rnd.nextBoolean()) t.dr to "Debit" else t.cr to "Credit"
            pool += Question.Choice("sidej:${t.id}:$acct", "${t.text} Which side is $acct on?", listOf("Debit", "Credit"), side,
                "Dr ${t.dr} · Cr ${t.cr}.")
        }
        val moves = pool.filter { it.id.startsWith("side:") }.shuffled(rnd)
        val entries = pool.filter { it.id.startsWith("sidej:") }.shuffled(rnd)
        // Mostly plain movements (the rules), a few from real-looking entries.
        return (moves.take(count - minOf(3, entries.size)) + entries.take(3)).shuffled(rnd).take(count)
    }

    /** A test from the plan: N made-up Ravi Traders transactions → journal (debit and credit). */
    fun journal(plan: DayPlan, week: Int, count: Int, rnd: Random): List<Question> {
        val pool: MutableList<Question> = plan.meta.journal.filter { it.week <= maxOf(4, week) }.map { journalQ(plan, it, rnd) }.toMutableList()
        // Complex entries (with GST) join once they're taught (week 5).
        if (week >= 5) pool += plan.meta.accountingQuestions.filter { it.week == 5 }.map { choice("q:${it.id}", it, rnd) }
        return pool.shuffled(rnd).take(count)
    }

    private fun choice(id: String, b: BankQuestion, rnd: Random) = Question.Choice(id, b.prompt, b.options.shuffled(rnd), b.answer, b.explanation)

    private fun journalQ(plan: DayPlan, t: JournalTx, rnd: Random): Question.Journal {
        fun opts(right: String, other: String): List<String> {
            // The other side of the entry is always one of the wrong options: it tests the direction.
            val pool = plan.meta.accounts.filter { it != right && it != other }.shuffled(rnd).take(2) + other
            return (pool + right).shuffled(rnd)
        }
        return Question.Journal("j:${t.id}", t.text, opts(t.dr, t.cr), t.dr, opts(t.cr, t.dr), t.cr, "Dr ${t.dr} · Cr ${t.cr}")
    }

    /** Fresh numbers every time, so a retake is a real retake. */
    fun calcQuestion(kind: String, rnd: Random): Question {
        fun r(lo: Int, hi: Int, step: Int) = (rnd.nextInt(lo / step, hi / step + 1) * step).toLong()
        val id = "calc:$kind:${rnd.nextInt(1_000_000)}"
        return when (kind) {
            "debits" -> {
                val p = r(5_000, 50_000, 500); val g = p * 18 / 100
                Question.Number(id, "An entry debits Purchases ${Money.inr(p)} and Input GST ${Money.inr(g)}. What must the total credit be?",
                    p + g, "Total debits = total credits = ${Money.inr(p + g)}.")
            }
            "gst" -> {
                val x = r(5_000, 60_000, 500)
                Question.Number(id, "Ravi Traders buys goods worth ${Money.inr(x)} + 18% GST. How much is the GST?", x * 18 / 100,
                    "18% of ${Money.inr(x)} = ${Money.inr(x * 18 / 100)}.")
            }
            "ledger" -> {
                val dr = r(40_000, 90_000, 1_000); val cr = r(10_000, (dr - 5_000).toInt(), 1_000)
                Question.Number(id, "Ravi Traders' Cash account: the debit side totals ${Money.inr(dr)} and the credit side ${Money.inr(cr)}. " +
                    "What is the (debit) balance?", dr - cr, "Balance = bigger side − smaller side = ${Money.inr(dr - cr)} debit.")
            }
            else -> {
                val a = r(200_000, 900_000, 10_000); val l = r(50_000, (a / 2).toInt(), 5_000)
                Question.Number(id, "Ravi Traders has assets of ${Money.inr(a)} and liabilities of ${Money.inr(l)}. What is the capital?", a - l,
                    "Capital = Assets − Liabilities = ${Money.inr(a)} − ${Money.inr(l)} = ${Money.inr(a - l)}.")
            }
        }
    }

    // ------------------------------------------------------------------ python and RAG

    private fun python(plan: DayPlan, task: PlanTask, count: Int, cards: Collection<Card>, rnd: Random): List<Question> {
        val own = ArrayList<Question>()
        val earlier = ArrayList<Question>()
        for (b in plan.meta.pythonQuestions) if (b.week <= task.week) (if (b.week == task.week) own else earlier) += choice("q:${b.id}", b, rnd)
        for (c in cards) {
            if (c.type != CardType.MCQ || c.options.size < 2) continue
            val unlock = plan.meta.cardUnlocks[c.id]?.let { plan.task(it) } ?: continue
            if (unlock.area != "python" || unlock.day > task.day) continue
            val q = Question.Choice("card:${c.id}", c.prompt, c.options.shuffled(rnd), c.answer, c.explanation)
            if (unlock.week == task.week) own += q else earlier += q
        }
        return (own.shuffled(rnd) + earlier.shuffled(rnd)).take(count)
    }

    // ------------------------------------------------------------------ french

    /** The plan's quiz: words, both directions (French → English, then English → French). */
    private fun frenchWords(plan: DayPlan, spec: CheckSpec.Quiz, rnd: Random): List<Question> {
        val all = plan.meta.french.flatMap { it.words }
        val weeks = spec.weeks.ifEmpty { listOf(1) }
        var words = plan.meta.french.filter { it.week in weeks }.flatMap { w ->
            when (spec.half) {
                0 -> w.words.take(5)
                1 -> w.words.drop(5)
                else -> w.words
            }
        }
        if (words.isEmpty()) words = all
        val picked = if (words.size >= spec.count) words.shuffled(rnd).take(spec.count) else (words + words).shuffled(rnd).take(spec.count)
        return picked.mapIndexed { i, w ->
            if (i % 2 == 0) {
                val wrong = all.map { it.en }.filter { it != w.en }.distinct().shuffled(rnd).take(3)
                Question.Choice("fw:${w.fr}:$i", "What does « ${w.fr} » mean?", (wrong + w.en).shuffled(rnd), w.en, "« ${w.fr} » = ${w.en}")
            } else {
                val wrong = all.map { it.fr }.filter { it != w.fr }.distinct().shuffled(rnd).take(3)
                Question.Choice("fe:${w.fr}:$i", "In French: “${w.en}”", (wrong + w.fr).shuffled(rnd), w.fr, "“${w.en}” = « ${w.fr} »")
            }
        }
    }

    /** "Cover the French; translate back from the English": build each model sentence from its words. */
    private fun frenchSentences(plan: DayPlan, spec: CheckSpec.Quiz, rnd: Random): List<Question> {
        val sentences = plan.meta.french.filter { it.week in spec.weeks.ifEmpty { listOf(1) } }.flatMap { it.sentences }
        return sentences.take(spec.count).mapIndexed { i, s ->
            val words = tokens(s.fr)
            var shuffled = words.shuffled(rnd)
            if (words.size > 1 && shuffled == words) shuffled = words.reversed()
            Question.Order("fo:$i:${s.fr.hashCode()}", "Translate back: “${s.en}”", shuffled, words, s.fr)
        }
    }

    fun tokens(sentence: String): List<String> = sentence.split(' ').filter { it.isNotBlank() }
}

object Money {
    /** Indian grouping: ₹1,00,000. */
    fun inr(v: Long): String {
        val neg = v < 0
        val s = kotlin.math.abs(v).toString()
        val out = if (s.length <= 3) s else {
            val last3 = s.takeLast(3)
            val rest = s.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
            "$rest,$last3"
        }
        return (if (neg) "−₹" else "₹") + out
    }
}
