package com.lakshman.roadmaplab.domain

/** The topic a task's checks count toward (for weak topics and improvement), from the roadmap's own week plans. */
object Topics {
    private val ACCOUNTING = mapOf(
        0 to "Basics and the equation", 1 to "Basics and the equation", 2 to "Balance sheet, income statement, GAAP",
        3 to "Types of accounts, debit and credit", 4 to "Journal entries", 5 to "Complex entries (with GST)",
        6 to "Practice problems", 7 to "Practice problems", 8 to "Ledger and posting", 9 to "Balancing and balances",
        10 to "Ledger practice problems", 11 to "Revision: test #2",
    )

    fun of(plan: DayPlan, t: PlanTask): String = when (t.area) {
        "accounting" -> ACCOUNTING[t.week] ?: "Accounting"
        "french" -> plan.frenchWeek(t.week)?.theme ?: "Review"
        "python" -> plan.meta.weekFocus[t.week] ?: "Lead-in"
        "comms" -> when {
            t.seq == "recording" -> "Recordings"
            t.learn.contains("User training") -> "Trainings"
            else -> "Work habits"
        }
        else -> plan.area(t.area)?.name ?: t.area
    }
}
