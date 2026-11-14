package com.lakshman.roadmaplab.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LabDao {
    // ---- live streams for the screens
    @Query("SELECT * FROM phases ORDER BY sortOrder")
    fun phasesFlow(): Flow<List<PhaseEntity>>

    @Query("SELECT * FROM tasks ORDER BY plannedDay, sortOrder")
    fun tasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM milestones ORDER BY day")
    fun milestonesFlow(): Flow<List<MilestoneEntity>>

    @Query("SELECT * FROM cards")
    fun cardsFlow(): Flow<List<CardEntity>>

    @Query("SELECT * FROM card_states")
    fun cardStatesFlow(): Flow<List<CardStateEntity>>

    @Query("SELECT * FROM task_logs ORDER BY createdAt, id")
    fun taskLogsFlow(): Flow<List<TaskLogEntity>>

    @Query("SELECT * FROM day_logs ORDER BY day")
    fun dayLogsFlow(): Flow<List<DayLogEntity>>

    @Query("SELECT * FROM review_logs ORDER BY createdAt, id")
    fun reviewLogsFlow(): Flow<List<ReviewLogEntity>>

    @Query("SELECT * FROM day_plans ORDER BY day, position")
    fun dayPlansFlow(): Flow<List<DayPlanEntity>>

    @Query("SELECT * FROM meta")
    fun metaFlow(): Flow<List<MetaEntity>>

    // ---- one-off reads
    @Query("SELECT * FROM phases ORDER BY sortOrder")
    suspend fun phases(): List<PhaseEntity>

    @Query("SELECT * FROM tasks ORDER BY plannedDay, sortOrder")
    suspend fun tasks(): List<TaskEntity>

    @Query("SELECT * FROM milestones ORDER BY day")
    suspend fun milestones(): List<MilestoneEntity>

    @Query("SELECT * FROM cards")
    suspend fun cards(): List<CardEntity>

    @Query("SELECT * FROM card_states")
    suspend fun cardStates(): List<CardStateEntity>

    @Query("SELECT * FROM card_states WHERE cardId = :cardId")
    suspend fun cardState(cardId: String): CardStateEntity?

    @Query("SELECT * FROM task_logs ORDER BY createdAt, id")
    suspend fun taskLogs(): List<TaskLogEntity>

    @Query("SELECT * FROM day_logs ORDER BY day")
    suspend fun dayLogs(): List<DayLogEntity>

    @Query("SELECT * FROM day_logs WHERE day = :day")
    suspend fun dayLog(day: Long): DayLogEntity?

    @Query("SELECT MAX(logNumber) FROM day_logs")
    suspend fun maxLogNumber(): Int?

    @Query("SELECT * FROM review_logs ORDER BY createdAt, id")
    suspend fun reviewLogs(): List<ReviewLogEntity>

    @Query("SELECT * FROM day_plans ORDER BY day, position")
    suspend fun dayPlans(): List<DayPlanEntity>

    @Query("SELECT COUNT(*) FROM day_plans WHERE day = :day")
    suspend fun planCount(day: Long): Int

    @Query("SELECT COUNT(*) FROM task_logs WHERE day = :day")
    suspend fun logCount(day: Long): Int

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun taskCount(): Int

    @Query("SELECT COUNT(*) FROM cards")
    suspend fun cardCount(): Int

    @Query("SELECT * FROM meta")
    suspend fun meta(): List<MetaEntity>

    // ---- writes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPhases(items: List<PhaseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(items: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMilestones(items: List<MilestoneEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCards(items: List<CardEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCardStates(items: List<CardStateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCardState(item: CardStateEntity)

    @Insert
    suspend fun insertTaskLog(item: TaskLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTaskLogs(items: List<TaskLogEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDayLog(item: DayLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDayLogs(items: List<DayLogEntity>)

    @Insert
    suspend fun insertReviewLog(item: ReviewLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReviewLogs(items: List<ReviewLogEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDayPlans(items: List<DayPlanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(items: List<MetaEntity>)

    @Query("DELETE FROM day_plans WHERE day = :day")
    suspend fun clearDayPlan(day: Long)

    @Query("DELETE FROM phases")
    suspend fun clearPhases()

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    @Query("DELETE FROM milestones")
    suspend fun clearMilestones()

    @Query("DELETE FROM cards")
    suspend fun clearCards()

    @Query("DELETE FROM card_states")
    suspend fun clearCardStates()

    @Query("DELETE FROM task_logs")
    suspend fun clearTaskLogs()

    @Query("DELETE FROM day_logs")
    suspend fun clearDayLogs()

    @Query("DELETE FROM review_logs")
    suspend fun clearReviewLogs()

    @Query("DELETE FROM day_plans")
    suspend fun clearDayPlans()

    @Query("DELETE FROM meta")
    suspend fun clearMeta()
}
