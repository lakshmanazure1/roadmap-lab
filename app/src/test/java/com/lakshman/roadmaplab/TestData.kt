package com.lakshman.roadmaplab

import com.lakshman.roadmaplab.data.json.LabJson
import com.lakshman.roadmaplab.data.json.PlanMetaJson
import com.lakshman.roadmaplab.domain.Assessment
import com.lakshman.roadmaplab.domain.Card
import com.lakshman.roadmaplab.domain.DayPlan
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.EventType
import com.lakshman.roadmaplab.domain.ItemEvent
import com.lakshman.roadmaplab.domain.RoutineCheck
import java.io.File

object TestData {
    fun asset(name: String): File {
        // Gradle runs unit tests from the module folder; the local runner from the project folder.
        val candidates = listOf(File("src/main/assets/$name"), File("app/src/main/assets/$name"))
        return candidates.first { it.exists() }
    }

    val csv: String by lazy { asset("schedule.csv").readText() }
    val plan: DayPlan by lazy { PlanMetaJson.plan(asset("plan_meta.json").readText(), csv) }
    val cards: List<Card> by lazy { LabJson.parseCards(asset("cards.json").readText()).second }

    fun d(iso: String): Long = Days.of(iso)

    private var nextId = 1L
    fun ev(item: String, day: Long, type: EventType = EventType.DONE, minutes: Int? = null, independent: Boolean = false): ItemEvent {
        val m = minutes ?: plan.task(item)?.minutes ?: 30
        val id = nextId++
        return ItemEvent(id, item, day, type, if (type == EventType.DONE || type == EventType.START) m else 0, independent, "", day * 100_000 + id)
    }

    fun check(item: String?, day: Long, pct: Double, independent: Boolean = false, type: String = "quiz", area: String = "accounting",
              topic: String = "T", passed: Boolean = pct >= 70.0): Assessment {
        val id = nextId++
        return Assessment(id, item, area, topic, type, pct, 100.0, passed, independent, "{}", day, day * 100_000 + id, 60)
    }

    fun tick(id: String, day: Long, minutes: Int = 5, done: Boolean = true) = RoutineCheck(day, id, done, minutes, day * 100_000)
}
