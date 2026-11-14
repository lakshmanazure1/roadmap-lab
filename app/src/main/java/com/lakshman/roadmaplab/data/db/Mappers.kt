package com.lakshman.roadmaplab.data.db

import com.lakshman.roadmaplab.domain.Card
import com.lakshman.roadmaplab.domain.CardState
import com.lakshman.roadmaplab.domain.CardType
import com.lakshman.roadmaplab.domain.DayLog
import com.lakshman.roadmaplab.domain.DayPlanEntry
import com.lakshman.roadmaplab.domain.LegacyMilestone
import com.lakshman.roadmaplab.domain.LegacyPhase
import com.lakshman.roadmaplab.domain.LegacyTask
import com.lakshman.roadmaplab.domain.Rating
import com.lakshman.roadmaplab.domain.ReviewLog
import com.lakshman.roadmaplab.domain.TaskLog
import com.lakshman.roadmaplab.domain.TaskStatus
import org.json.JSONArray

fun PhaseEntity.toDomain() = LegacyPhase(id, track, title, startDay, endDay, sortOrder)
fun LegacyPhase.toEntity() = PhaseEntity(id, track, title, startDay, endDay, order)

fun TaskEntity.toDomain() = LegacyTask(
    id = id, phaseId = phaseId, track = track, plannedDay = plannedDay, title = title, learn = learn,
    apply = apply, doneWhen = doneWhen, minFull = minFull, minDrc = minDrc, minMinimum = minMinimum,
    unlocks = unlocks.split(',').map { it.trim() }.filter { it.isNotEmpty() },
    core = core, assumedDone = assumedDone, order = sortOrder,
)

fun LegacyTask.toEntity() = TaskEntity(
    id = id, phaseId = phaseId, track = track, plannedDay = plannedDay, title = title, learn = learn,
    apply = apply, doneWhen = doneWhen, minFull = minFull, minDrc = minDrc, minMinimum = minMinimum,
    unlocks = unlocks.joinToString(","), core = core, assumedDone = assumedDone, sortOrder = order,
)

fun MilestoneEntity.toDomain() = LegacyMilestone(id, title, day, track)
fun LegacyMilestone.toEntity() = MilestoneEntity(id, title, day, track)

fun CardEntity.toDomain(): Card {
    val arr = try { JSONArray(optionsJson) } catch (e: Exception) { JSONArray() }
    return Card(
        id = id, topic = topic, type = if (type == "flashcard") CardType.FLASHCARD else CardType.MCQ,
        prompt = prompt, options = (0 until arr.length()).map { arr.getString(it) },
        answer = answer, explanation = explanation, unlockedBy = unlockedBy,
    )
}

fun Card.toEntity() = CardEntity(
    id = id, topic = topic, type = if (type == CardType.FLASHCARD) "flashcard" else "mcq", prompt = prompt,
    optionsJson = JSONArray(options).toString(), answer = answer, explanation = explanation, unlockedBy = unlockedBy,
)

fun CardStateEntity.toDomain() = CardState(cardId, box, mcCorrect, dueDay, lapses, reviews, lastReviewDay, unlockedDay)
fun CardState.toEntity() = CardStateEntity(cardId, box, mcCorrect, dueDay, lapses, reviews, lastReviewDay, unlockedDay)

fun TaskLogEntity.toDomain() = TaskLog(
    id, taskId, day, TaskStatus.of(status) ?: TaskStatus.SKIPPED, minutes, note, createdAt, usedMinimum,
)

fun TaskLog.toEntity() = TaskLogEntity(id, taskId, day, status.key, minutes, note, createdAt, usedMinimum)

fun DayLogEntity.toDomain() = DayLog(
    day, mood, energy, sleepHours, ifThen, note, evModule, evProblem, evAction, evResult, evLearning,
    eveningDone, logNumber, finishedAt,
)

fun DayLog.toEntity() = DayLogEntity(
    day, mood, energy, sleepHours, ifThen, note, evModule, evProblem, evAction, evResult, evLearning,
    eveningDone, logNumber, finishedAt,
)

fun ReviewLogEntity.toDomain() = ReviewLog(
    cardId, day, runCatching { Rating.valueOf(rating) }.getOrDefault(Rating.GOOD), correct, asMcq, createdAt,
)

fun ReviewLog.toEntity() = ReviewLogEntity(0, cardId, day, rating.name, correct, asMcq, createdAt)

fun DayPlanEntity.toDomain() = DayPlanEntry(day, taskId, position)
fun DayPlanEntry.toEntity() = DayPlanEntity(day, taskId, position)

// ---------------------------------------------------------------- version 2

fun TaskEventEntity.toDomain() = com.lakshman.roadmaplab.domain.ItemEvent(
    id, itemId, day, com.lakshman.roadmaplab.domain.EventType.of(type), minutes, independent, note, createdAt,
)
fun com.lakshman.roadmaplab.domain.ItemEvent.toEntity() = TaskEventEntity(id, itemId, day, type.key, minutes, independent, note, createdAt)

fun AssessmentEntity.toDomain() = com.lakshman.roadmaplab.domain.Assessment(
    id, itemId, area, topic, type, score, maxScore, passed, independent, detail, day, createdAt, seconds,
)
fun com.lakshman.roadmaplab.domain.Assessment.toEntity() = AssessmentEntity(
    id, itemId, area, topic, type, score, max, passed, independent, detail, day, createdAt, seconds,
)

fun DayTickEntity.toDomain() = com.lakshman.roadmaplab.domain.RoutineCheck(day, tickId, done, minutes, updatedAt)
fun com.lakshman.roadmaplab.domain.RoutineCheck.toEntity() = DayTickEntity(day, routineId, done, minutes, updatedAt)

fun EvidenceEntity.toDomain() = com.lakshman.roadmaplab.domain.Evidence(
    id, day, area, itemId, kind, task, problem, action, result, lesson, skill, proof, confidence, starred,
    starS, starT, starA, starR, createdAt, updatedAt,
)
fun com.lakshman.roadmaplab.domain.Evidence.toEntity() = EvidenceEntity(
    id, day, area, itemId, kind, task, problem, action, result, lesson, skill, proof, confidence, starred,
    starS, starT, starA, starR, createdAt, updatedAt,
)

fun DayMetaEntity.toDomain() = com.lakshman.roadmaplab.domain.DayMeta(
    day, com.lakshman.roadmaplab.domain.DayKind.of(kind), focusItemId, kindSetAt, updatedAt,
)
fun com.lakshman.roadmaplab.domain.DayMeta.toEntity() = DayMetaEntity(day, kind?.key, focusItemId, kindSetAt, updatedAt)

private fun mapJson(text: String): Map<String, String> = try {
    val o = org.json.JSONObject(text)
    o.keys().asSequence().associateWith { o.getString(it) }
} catch (e: Exception) {
    emptyMap()
}
private fun Map<String, String>.json(): String = org.json.JSONObject(this as Map<*, *>).toString()

fun WeeklyReviewEntity.toDomain() = com.lakshman.roadmaplab.domain.WeeklyReview(
    weekStart, mapJson(actualJson), mapJson(assessmentJson), mapJson(adjustmentJson), missedWhy,
    obstacleTags.split(',').map { it.trim() }.filter { it.isNotEmpty() }, best, weakest, priorities, nextLoad, spending,
    completedAt, updatedAt,
)
fun com.lakshman.roadmaplab.domain.WeeklyReview.toEntity() = WeeklyReviewEntity(
    weekStart, actual.json(), assessment.json(), adjustment.json(), missedWhy, obstacleTags.joinToString(","), best, weakest,
    priorities, nextLoad, spending, completedAt, updatedAt,
)
