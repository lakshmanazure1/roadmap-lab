package com.lakshman.roadmaplab.domain

/**
 * Warns when text looks like real identifiers the plan says never to store:
 * PAN, GSTIN, IFSC, bank account numbers, phone numbers and e-mail addresses.
 * It only warns (practice data in your notes may be fake on purpose); it never sends anything anywhere.
 */
object Privacy {
    private val PATTERNS = listOf(
        "a GSTIN" to Regex("""\b\d{2}[A-Z]{5}\d{4}[A-Z][1-9A-Z]Z[0-9A-Z]\b""", RegexOption.IGNORE_CASE),
        "a PAN" to Regex("""\b[A-Z]{5}\d{4}[A-Z]\b""", RegexOption.IGNORE_CASE),
        "an IFSC code" to Regex("""\b[A-Z]{4}0[A-Z0-9]{6}\b"""),
        "an e-mail address" to Regex("""[^@\s]+@[^@\s]+\.[^@\s]+"""),
        "a phone number" to Regex("""(?<!\d)(\+?\d{2,3}[ -]?)?[6-9]\d{9}(?!\d)"""),
        "a bank account number" to Regex("""(?<!\d)\d{11,18}(?!\d)"""),
    )

    data class Finding(val what: String, val text: String)

    fun scan(text: String): List<Finding> {
        val out = ArrayList<Finding>()
        val taken = ArrayList<IntRange>()
        for ((what, re) in PATTERNS) {
            for (m in re.findAll(text)) {
                if (taken.any { it.first <= m.range.last && m.range.first <= it.last }) continue
                taken += m.range
                out += Finding(what, m.value)
            }
        }
        return out
    }

    fun warning(text: String): String? {
        val f = scan(text)
        if (f.isEmpty()) return null
        val kinds = f.map { it.what }.distinct().joinToString(", ")
        return "This looks like $kinds. If it's real, replace it with “the client” or a made-up value."
    }
}

/** The plan's STAR format: S 1 line · T 1 line · A 2–3 lines · R 1 line with a number · under 150 words · no names. */
object StarCheck {
    data class Result(val criteria: List<Pair<String, Boolean>>) {
        val passed: Int get() = criteria.count { it.second }
        val total: Int get() = criteria.size
        val ok: Boolean get() = passed == total
    }

    fun words(text: String): Int = text.split(Regex("\\s+")).count { it.isNotBlank() }

    fun check(e: Evidence): Result {
        val all = listOf(e.starS, e.starT, e.starA, e.starR).joinToString(" ")
        val actionLines = e.starA.split('\n', '.').count { it.isNotBlank() }
        return Result(
            listOf(
                "Situation written" to e.starS.isNotBlank(),
                "Task written" to e.starT.isNotBlank(),
                "Action: 2–3 lines" to (actionLines in 2..3),
                "Result has a number" to e.starR.any { it.isDigit() },
                "Under 150 words (${words(all)})" to (words(all) in 1..149),
                "No names, PAN, GSTIN or bank details found" to Privacy.scan(all).isEmpty(),
            )
        )
    }
}
