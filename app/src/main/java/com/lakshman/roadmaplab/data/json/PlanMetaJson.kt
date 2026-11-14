package com.lakshman.roadmaplab.data.json

import com.lakshman.roadmaplab.domain.Area
import com.lakshman.roadmaplab.domain.BankQuestion
import com.lakshman.roadmaplab.domain.DayPlan
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.FrenchSentence
import com.lakshman.roadmaplab.domain.FrenchWeek
import com.lakshman.roadmaplab.domain.FrenchWord
import com.lakshman.roadmaplab.domain.JournalTx
import com.lakshman.roadmaplab.domain.PlanCsv
import com.lakshman.roadmaplab.domain.PlanMeta
import com.lakshman.roadmaplab.domain.ReducedAction
import com.lakshman.roadmaplab.domain.RubricCriterion
import org.json.JSONArray
import org.json.JSONObject

/** Reads assets/plan_meta.json (made by tools/build_plan.py from the roadmap PDF) and joins it with the CSV. */
object PlanMetaJson {
    fun plan(metaText: String, csvText: String): DayPlan {
        val meta = parse(metaText)
        return DayPlan(meta, PlanCsv.tasks(csvText, meta.titles))
    }

    fun parse(text: String): PlanMeta {
        val o = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw LabFileException("plan_meta.json can't be read: ${e.message}")
        }
        if (o.optString("kind") != "plan-meta") throw LabFileException("This isn't the plan's notes file.")
        val rubric = o.getJSONObject("rubric")
        return PlanMeta(
            version = o.optString("version"),
            title = o.optString("title"),
            subtitle = o.optString("subtitle"),
            csvName = o.optString("csvName"),
            start = Days.of(o.getString("start")),
            leadIn = Days.of(o.getString("leadIn")),
            end = Days.of(o.getString("end")),
            bottomLine = o.optString("bottomLine"),
            areas = o.getJSONArray("areas").objs().map {
                Area(it.getString("id"), it.optString("name"), it.optString("short"), it.optString("objective"), it.optString("byEnd"),
                    it.optString("budget"), it.optString("assessed"), it.optString("notEnough"), it.optString("success"),
                    it.optString("targetLevel"), it.optString("stretch"), it.optJSONArray("rules").strs())
            },
            rubric = rubric.getJSONArray("criteria").objs().map { RubricCriterion(it.getString("name"), it.getJSONArray("levels").strs()) },
            rubricNote = rubric.optString("note"),
            levels = o.getJSONArray("levels").objs().map { it.getString("name") to it.getString("meaning") },
            french = o.getJSONArray("french").objs().map { w ->
                FrenchWeek(w.getInt("week"), w.optString("theme"),
                    w.getJSONArray("words").objs().map { FrenchWord(it.getString("fr"), it.getString("en")) },
                    w.getJSONArray("sentences").objs().map { FrenchSentence(it.getString("fr"), it.getString("en")) })
            },
            checkpoints = o.getJSONObject("checkpoints").let { c ->
                c.keys().asSequence().associate { k -> k.toInt() to c.getJSONObject(k).let { a -> a.keys().asSequence().associateWith { a.getString(it) } } }
            },
            weekFocus = o.getJSONObject("weekFocus").let { f -> f.keys().asSequence().associate { it.toInt() to f.getString(it) } },
            reduced = o.getJSONArray("reduced").objs().map {
                ReducedAction(it.getString("id"), it.getString("area"), it.getString("action"), it.optString("time"), it.optInt("minutes"),
                    it.getJSONArray("days").ints().toSet())
            },
            recovery = o.optJSONArray("recovery").strs(),
            rules = o.optJSONArray("rules").strs(),
            monthly = o.getJSONObject("monthly").let { m -> m.keys().asSequence().associateWith { m.getJSONArray(it).strs() } },
            weekRoutine = o.getJSONArray("weekRoutine").let { a -> (0 until a.length()).map { a.getJSONArray(it).strs() } },
            titles = o.getJSONObject("titles").let { t -> t.keys().asSequence().associateWith { t.getString(it) } },
            accountingQuestions = o.getJSONArray("accountingQuestions").objs().map(::question),
            journal = o.getJSONArray("journal").objs().map {
                JournalTx(it.getString("id"), it.getInt("week"), it.getString("text"), it.getString("dr"), it.getString("cr"))
            },
            accounts = o.getJSONArray("accounts").strs(),
            calcs = o.getJSONArray("calcs").objs().map { it.getString("kind") to it.getInt("week") },
            pythonQuestions = o.getJSONArray("pythonQuestions").objs().map(::question),
            cardUnlocks = o.getJSONObject("cardUnlocks").let { u -> u.keys().asSequence().associateWith { u.getString(it) } },
        )
    }

    private fun question(it: JSONObject) = BankQuestion(it.getString("id"), it.getInt("week"), it.getString("prompt"),
        it.getJSONArray("options").strs(), it.getString("answer"), it.optString("explanation"))

    private fun JSONArray?.objs(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }
    private fun JSONArray?.strs(): List<String> = if (this == null) emptyList() else (0 until length()).map { getString(it) }
    private fun JSONArray?.ints(): List<Int> = if (this == null) emptyList() else (0 until length()).map { getInt(it) }
}
