package com.lakshman.roadmaplab

import com.lakshman.roadmaplab.data.json.LabJson
import com.lakshman.roadmaplab.domain.Card
import com.lakshman.roadmaplab.domain.PlanTask
import com.lakshman.roadmaplab.domain.Roadmap
import com.lakshman.roadmaplab.domain.TaskLog
import com.lakshman.roadmaplab.domain.TaskStatus
import java.io.File

object TestData {
    private fun asset(name: String): File {
        // Gradle runs unit tests from the module folder; the local runner from the project folder.
        val candidates = listOf(File("src/main/assets/$name"), File("app/src/main/assets/$name"))
        return candidates.first { it.exists() }
    }

    val roadmap: Roadmap by lazy { LabJson.parseRoadmap(asset("roadmap.json").readText()) }
    val cards: List<Card> by lazy { LabJson.parseCards(asset("cards.json").readText()).second }

    fun task(id: String, day: Long, minutes: Int, core: Boolean = true, order: Int = 0, track: String = "T") = PlanTask(
        id = id, phaseId = "P", track = track, plannedDay = day, title = id, learn = "", apply = "", doneWhen = "",
        minFull = minutes, minDrc = minutes, minMinimum = 15, unlocks = emptyList(), core = core,
        assumedDone = false, order = order,
    )

    private var nextId = 1L
    fun log(taskId: String, day: Long, status: TaskStatus, at: Long = nextId) =
        TaskLog(nextId++, taskId, day, status, 30, "", at, false)
}
