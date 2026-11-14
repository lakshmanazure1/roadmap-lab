package com.lakshman.roadmaplab.domain

/**
 * Version 1 spaced repetition: Leitner boxes.
 *  - New cards (box 0) start as multiple choice; after 2 correct answers in a row they are asked
 *    as flashcards you answer from memory.
 *  - Again → box 1, back tomorrow. Hard → same box, half the wait. Good → next box. Easy → skip a box.
 *  - At most 20 reviews a day; the rest stay due and spread over the next days.
 *  - Each session mixes topics (interleaving).
 */
object Leitner {
    /** Days to wait after landing in box i (index = box). */
    val INTERVAL_DAYS = intArrayOf(0, 1, 2, 4, 8, 16, 32)
    const val MAX_BOX = 6
    const val PROMOTE_AFTER_CORRECT = 2
    const val DAILY_CAP = 20

    fun newState(cardId: String, day: Long) = CardState(
        cardId = cardId, box = 0, mcCorrect = 0, dueDay = day, lapses = 0, reviews = 0,
        lastReviewDay = null, unlockedDay = day,
    )

    fun askAsMcq(card: Card, state: CardState): Boolean =
        card.type == CardType.MCQ && card.options.size >= 2 && state.mcCorrect < PROMOTE_AFTER_CORRECT

    fun apply(state: CardState, rating: Rating, day: Long, asMcq: Boolean): CardState {
        val correct = rating != Rating.AGAIN
        val box = when (rating) {
            Rating.AGAIN -> 1
            Rating.HARD -> maxOf(1, state.box)
            Rating.GOOD -> minOf(MAX_BOX, state.box + 1)
            Rating.EASY -> minOf(MAX_BOX, state.box + 2)
        }
        val wait = when (rating) {
            Rating.AGAIN -> 1
            Rating.HARD -> maxOf(1, INTERVAL_DAYS[box] / 2)
            else -> INTERVAL_DAYS[box]
        }
        val mcCorrect = when {
            !asMcq -> state.mcCorrect
            correct -> state.mcCorrect + 1
            else -> 0
        }
        return state.copy(
            box = box,
            mcCorrect = mcCorrect,
            dueDay = day + wait,
            lapses = state.lapses + if (!correct && state.box > 0) 1 else 0,
            reviews = state.reviews + 1,
            lastReviewDay = day,
        )
    }

    /** Today's review queue: due cards, oldest first, capped, then interleaved by topic. */
    fun queue(cards: Map<String, Card>, states: List<CardState>, today: Long, reviewedToday: Int): List<CardState> {
        val limit = maxOf(0, DAILY_CAP - reviewedToday)
        val due = states
            .filter { it.dueDay <= today && cards.containsKey(it.cardId) }
            .sortedWith(compareBy<CardState>({ it.dueDay }, { it.box }, { it.cardId }))
            .take(limit)
        return interleave(due) { cards.getValue(it.cardId).topic }
    }

    fun dueCount(states: List<CardState>, today: Long): Int = states.count { it.dueDay <= today }

    /** Round-robin by topic, keeping each topic's order. */
    fun <T> interleave(items: List<T>, topicOf: (T) -> String): List<T> {
        val groups = LinkedHashMap<String, ArrayDeque<T>>()
        for (item in items) groups.getOrPut(topicOf(item)) { ArrayDeque() }.addLast(item)
        val out = ArrayList<T>(items.size)
        while (out.size < items.size) {
            for (q in groups.values) if (q.isNotEmpty()) out.add(q.removeFirst())
        }
        return out
    }

    /** Recall rate over the last [days] days, or null with no reviews. */
    fun recallRate(logs: List<ReviewLog>, today: Long, days: Int = 30): Double? {
        val recent = logs.filter { it.day > today - days && it.day <= today }
        if (recent.isEmpty()) return null
        return 100.0 * recent.count { it.correct } / recent.size
    }

    /** Cards that have reached box 3 or higher. */
    fun learnedCount(states: List<CardState>): Int = states.count { it.box >= 3 }

    /**
     * New card states for cards whose unlocking plan item is now completed (the plan's cardUnlocks map).
     * Cards already unlocked keep their history; cards for later months (SAP, SQL…) stay locked.
     */
    fun unlockForItems(doneItemIds: Set<String>, cards: Collection<Card>, unlocks: Map<String, String>, existing: Collection<CardState>, day: Long): List<CardState> {
        val have = existing.mapTo(HashSet()) { it.cardId }
        return cards.filter { c -> unlocks[c.id]?.let { it in doneItemIds } == true && c.id !in have }.map { newState(it.id, day) }
    }

    /** Your own cards (A5: "Write 10 flashcards") have ids starting with this. */
    const val USER_PREFIX = "U-"
}
