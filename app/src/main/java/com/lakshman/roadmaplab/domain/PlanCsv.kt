package com.lakshman.roadmaplab.domain

/**
 * Reads the roadmap's CSV (one task per row) and works out, from each row's own words, how the task is
 * scheduled and how it can be checked. Nothing is added: every check comes from the "done when" column
 * or from the roadmap's section 12 ("what counts as assessed").
 */
object PlanCsv {
    val COLUMNS = listOf(
        "task_id", "date", "weekday", "week", "area", "learn_practise", "build_deliver", "minutes",
        "done_when", "evidence", "milestone", "status", "score", "notes",
    )

    val AREA_IDS = mapOf(
        "Python/AI" to "python", "Accounting" to "accounting", "French" to "french", "Communication" to "comms",
        "LinkedIn" to "linkedin", "Evidence log" to "evidence", "App" to "app", "Health/family/money" to "life",
        "Weekly review" to "review",
    )

    class CsvException(message: String) : Exception(message)

    /** RFC 4180 CSV: quoted fields may hold commas, quotes ("") and line breaks. */
    fun parse(text: String): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        val s = text.removePrefix("﻿")
        fun endField() { row.add(field.toString()); field.setLength(0) }
        fun endRow() {
            endField()
            if (!(row.size == 1 && row[0].isEmpty())) rows.add(row)
            row = ArrayList()
        }
        while (i < s.length) {
            val c = s[i]
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < s.length && s[i + 1] == '"') { field.append('"'); i++ } else quoted = false
                } else field.append(c)
            } else when (c) {
                '"' -> quoted = true
                ',' -> endField()
                '\r' -> {}
                '\n' -> endRow()
                else -> field.append(c)
            }
            i++
        }
        if (quoted) throw CsvException("A quoted field is never closed.")
        if (field.isNotEmpty() || row.isNotEmpty()) endRow()
        return rows
    }

    /** Builds the plan's tasks from CSV text. [titles] gives short titles for known task ids. */
    fun tasks(text: String, titles: Map<String, String> = emptyMap()): List<PlanTask> {
        val rows = parse(text)
        if (rows.isEmpty()) throw CsvException("The file is empty.")
        val header = rows.first().map { it.trim() }
        val idx = COLUMNS.associateWith { header.indexOf(it) }
        val missing = COLUMNS.take(11).filter { idx[it]!! < 0 }
        if (missing.isNotEmpty()) throw CsvException("This isn't the plan's CSV: missing column ${missing.joinToString()}.")
        val seen = HashSet<String>()
        return rows.drop(1).mapIndexed { n, r ->
            fun col(name: String): String = idx[name]!!.let { if (it in r.indices) r[it].trim() else "" }
            val id = col("task_id")
            if (id.isBlank()) throw CsvException("Row ${n + 2} has no task_id.")
            if (!seen.add(id)) throw CsvException("Task $id appears twice.")
            val areaLabel = col("area")
            val area = AREA_IDS[areaLabel] ?: throw CsvException("Task $id: unknown area “$areaLabel”.")
            val day = try { Days.of(col("date")) } catch (e: Exception) { throw CsvException("Task $id: date “${col("date")}” isn't YYYY-MM-DD.") }
            val learn = col("learn_practise").let { if (it == "—") "" else it }
            val build = col("build_deliver").let { if (it == "—") "" else it }
            val doneWhen = col("done_when")
            val weekday = col("weekday")
            val kind = kindOf(area, weekday, learn, build, col("milestone"))
            val base = PlanTask(
                id = id, day = day, weekday = weekday, week = col("week").toIntOrNull() ?: 0, area = area, areaLabel = areaLabel,
                learn = learn, build = build, minutes = col("minutes").toIntOrNull() ?: 0, doneWhen = doneWhen,
                evidence = col("evidence").let { if (it == "—") "" else it }, milestone = col("milestone"),
                csvLevel = col("status").takeWhile { it.isDigit() }.toIntOrNull() ?: 1, csvScore = col("score"), csvNotes = col("notes"),
                title = titles[id] ?: fallbackTitle(learn, build),
                kind = kind, seq = seqOf(area, weekday), checks = emptyList(), order = n,
            )
            base.copy(checks = checksOf(base))
        }
    }

    fun kindOf(area: String, weekday: String, learn: String, build: String, milestone: String): TaskKind = when (area) {
        "accounting", "french", "python", "app", "linkedin" -> TaskKind.SEQUENCE
        "comms" -> when {
            weekday == "Sun" -> TaskKind.SEQUENCE
            learn.contains("User training") || build.contains("prepare training") -> TaskKind.CARRY
            else -> TaskKind.DAILY
        }
        "evidence" -> if (build.contains("STAR #")) TaskKind.CARRY else TaskKind.DAILY
        "life" -> when {
            learn.startsWith("Rest day") -> TaskKind.REST
            build.contains("MONEY:") || build.contains("FAMILY:") || milestone.isNotBlank() -> TaskKind.CARRY
            else -> TaskKind.DAILY
        }
        "review" -> TaskKind.CARRY
        else -> TaskKind.DAILY
    }

    fun seqOf(area: String, weekday: String): String = if (area == "comms" && weekday == "Sun") "recording" else area

    private fun fallbackTitle(learn: String, build: String): String =
        (learn.ifBlank { build }).split(" · ").first().replace(Regex("\\s*\\([^)]*\\)"), "").take(72)

    private val OUT_OF = Regex("""(\d+)\s*/\s*(\d+)""")
    private val OF = Regex("""(\d+) of (\d+)""")
    private val PCT = Regex("""(\d+)% or better""")
    private val RUBRIC = Regex("""(?:Rubric score |^)(\d+)/10 or better""")

    /** Checks worked out from the task's own words. */
    fun checksOf(t: PlanTask): List<CheckSpec> {
        val out = ArrayList<CheckSpec>()
        val all = "${t.learn} ${t.build}"
        val dw = t.doneWhen
        when (t.area) {
            "french" -> french(t, out)
            "comms" -> {
                if (dw.contains("Rubric") || t.seq == "recording") {
                    out += CheckSpec.Rubric("Score it with the rubric", RUBRIC.find(dw)?.groupValues?.get(1)?.toInt() ?: monthTarget(t.day))
                } else if (t.learn.contains("User training")) {
                    out += CheckSpec.Rubric("Score the training with the rubric", monthTarget(t.day))
                    out += CheckSpec.Demonstrate("You trained others: that's a demonstration")
                }
                out += CheckSpec.Checklist("Done when", listOf(dw).filter { it.isNotBlank() })
            }
            "accounting" -> {
                val quizCount = Regex("""(\d+)[- ](?:card|'which side\?') quiz|Quiz: (\d+)""").find(all)?.let { m ->
                    m.groupValues.drop(1).firstOrNull { it.isNotBlank() }?.toInt()
                }
                val testCount = Regex("""(\d+) (?:transactions|new Ravi Traders transactions|mixed transactions|Ravi Traders transactions|transactions) → journal|(\d+)-entry self-test|Journalise (\d+) transactions""")
                    .find(all)?.groupValues?.drop(1)?.firstOrNull { it.isNotBlank() }?.toInt()
                val minutes = Regex("""in (\d+) min|\((\d+) min\)""").find(all)?.groupValues?.drop(1)?.firstOrNull { it.isNotBlank() }?.toInt() ?: 0
                val score = scoreFrom(dw)
                when {
                    testCount != null -> out += CheckSpec.Quiz("In-app test: $testCount transactions", "journal", testCount,
                        target = score?.target ?: 0, timedMinutes = minutes)
                    quizCount != null -> out += CheckSpec.Quiz("Cold quiz in the app", "accounting", quizCount,
                        target = score?.takeIf { it.max == quizCount }?.target ?: 0, timedMinutes = minutes)
                }
                if (score != null) out += score else if (dw.isNotBlank() && dw != "Done" && !dw.startsWith("Score written")) {
                    out += CheckSpec.Checklist("Done when", listOf(dw))
                }
                if (dw.startsWith("Score written")) out += CheckSpec.Score("Your score", "marks", 0, 0)
                if (testCount == null && quizCount == null && t.learn != "Light day") {
                    out += CheckSpec.Quiz("Quick check, cold (no notes)", "accounting", 5)
                }
                if (t.learn.contains("Explain", ignoreCase = true) || t.build.startsWith("Explain")) {
                    out += CheckSpec.Explain("Explain it aloud without notes", t.build.ifBlank { t.learn })
                }
            }
            "python" -> {
                scoreFrom(dw)?.let { out += it }
                val crit = dw.split(";").map { it.trim() }.filter { it.isNotBlank() && scoreFrom(it) == null }
                if (crit.isNotEmpty()) out += CheckSpec.Checklist("Done when", crit)
                out += CheckSpec.Explain("Explain it aloud without AI or notes",
                    if (t.learn.contains("Monthly check")) t.learn else "Explain what today's code does, line by line, in your own words.")
                out += CheckSpec.Quiz("Quick check (Python and RAG)", "python", 4)
                if (all.contains("Demo", ignoreCase = true) || all.contains("manager")) out += CheckSpec.Demonstrate("Shown to your manager")
            }
            "linkedin" -> {
                out += CheckSpec.Checklist("Done when", listOf(dw) + if (dw.startsWith("Message")) listOf(
                    "Asked about their work, never for a job", "Personal note under 300 characters") else emptyList())
                if (dw.startsWith("Message") || dw.contains("message")) out += CheckSpec.Outcome("Did they reply? (useful = 2+ messages)")
            }
            "evidence" -> {
                out += CheckSpec.Checklist("Done when", listOf(dw.split(";").first().trim()))
                if (t.build.contains("STAR #")) out += CheckSpec.Star("STAR story check")
            }
            "app" -> {
                val test = t.build.removePrefix("Test:").trim().takeIf { it.isNotBlank() && it != "—" }
                out += CheckSpec.Checklist("Tested on the phone", listOfNotNull(test, dw.takeIf { it.isNotBlank() }))
            }
            "life", "review" -> {}
        }
        return out
    }

    private fun monthTarget(day: Long): Int = if (Days.date(day).monthValue == 1) 7 else 6

    /** "10 of 12 correct", "Quiz 8/10 or better", "16/20 or better", "80% or better", "target 16/20". */
    fun scoreFrom(text: String): CheckSpec.Score? {
        if (text.contains("Rubric")) return null
        PCT.find(text)?.let { return CheckSpec.Score("Your score", "%", 100, it.groupValues[1].toInt()) }
        OF.find(text)?.let { m ->
            val (a, b) = m.destructured
            if (text.contains("for $a of $b")) return CheckSpec.Score("Your score", "test questions", b.toInt(), a.toInt())
            return CheckSpec.Score("Your score", "correct", b.toInt(), a.toInt())
        }
        OUT_OF.find(text)?.let { m ->
            val (a, b) = m.destructured
            if (b.toInt() <= 1 || a.toInt() > b.toInt()) return null
            return CheckSpec.Score("Your score", "correct", b.toInt(), a.toInt())
        }
        return null
    }

    private fun french(t: PlanTask, out: ArrayList<CheckSpec>) {
        val all = "${t.learn} ${t.build}"
        val dw = t.doneWhen
        val wk = Regex("""weeks? (\d+)(?:–(\d+))?""").find(all)
        val weeks = when {
            wk != null -> (wk.groupValues[1].toInt()..(wk.groupValues[2].toIntOrNull() ?: wk.groupValues[1].toInt())).toList()
            else -> listOf(t.week)
        }
        val target = scoreFrom(dw)
        when {
            Regex("""Quiz: (\d+) words|Review quiz: (\d+) words""").containsMatchIn(all) -> {
                val n = Regex("""(\d+) words""").find(all)!!.groupValues[1].toInt()
                out += CheckSpec.Quiz("$n-word quiz, both directions", "french", n, target = target?.target ?: 0, weeks = weeks)
                if (target != null) out += target
            }
            all.contains("words 1–5") -> {
                out += CheckSpec.Quiz("Check the 5 new words", "french", 5, weeks = listOf(t.week), half = 0)
                out += CheckSpec.Checklist("Done when", listOf(dw))
            }
            all.contains("Words 6–10") -> {
                out += CheckSpec.Quiz("Check the 5 new words", "french", 5, weeks = listOf(t.week), half = 1)
                out += CheckSpec.Checklist("Done when", listOf(dw))
            }
            all.contains("model sentences") && (all.contains("translate back") || all.contains("Translate back")) -> {
                out += CheckSpec.Quiz("Translate back: the 5 model sentences", "french-sentences", 5, target = target?.target ?: 4, weeks = listOf(t.week))
                if (all.contains("record")) out += CheckSpec.Checklist("Done when", listOf("Recording saved"))
            }
            all.contains("Rubric") || all.contains("rubric") -> {
                out += CheckSpec.Rubric("Score your introduction with the rubric", RUBRIC.find(dw)?.groupValues?.get(1)?.toInt() ?: 6)
                out += CheckSpec.Demonstrate("Said to a colleague, unaided")
            }
            all.contains("introduction") -> {
                out += CheckSpec.Quiz("Build your introduction", "french-sentences", 5, weeks = listOf(11))
                out += CheckSpec.Checklist("Done when", listOf(dw))
            }
            else -> out += CheckSpec.Checklist("Done when", listOf(dw))
        }
    }
}
