package com.lakshman.roadmaplab.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PhaseEntity::class, TaskEntity::class, MilestoneEntity::class, CardEntity::class,
        CardStateEntity::class, TaskLogEntity::class, DayLogEntity::class, ReviewLogEntity::class,
        DayPlanEntity::class, MetaEntity::class,
        // version 2
        TaskEventEntity::class, AssessmentEntity::class, DayTickEntity::class, EvidenceEntity::class,
        DayMetaEntity::class, WeeklyReviewEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2, spec = LabDatabase.Migration1To2::class)],
)
abstract class LabDatabase : RoomDatabase() {
    abstract fun dao(): LabDao

    /**
     * 1 → 2 adds the new tables (Room writes that SQL from the saved schemas in app/schemas) and then
     * copies any evidence lines you wrote in version 1 into the new evidence log. Old tables stay as they were.
     */
    class Migration1To2 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            db.execSQL(COPY_V1_EVIDENCE)
        }
    }

    companion object {
        const val NAME = "roadmaplab.db"

        const val COPY_V1_EVIDENCE = """
            INSERT INTO evidence (day, area, itemId, kind, task, problem, action, result, lesson, skill, proof, confidence,
                                  starred, starS, starT, starA, starR, createdAt, updatedAt)
            SELECT day, 'evidence', NULL, 'line', evModule, evProblem, evAction, evResult, evLearning, '', '', NULL,
                   0, '', '', '', '', COALESCE(finishedAt, day * 86400000), COALESCE(finishedAt, day * 86400000)
            FROM day_logs
            WHERE TRIM(evModule) <> '' OR TRIM(evProblem) <> '' OR TRIM(evAction) <> '' OR TRIM(evResult) <> '' OR TRIM(evLearning) <> ''
        """

        /**
         * Hand-written migrations, if a change ever needs one (AutoMigration covers 1 → 2).
         * The app never uses destructive migration, so an update can't wipe your history:
         * if a migration were ever missing, the app would stop with an error instead of deleting data.
         */
        val MIGRATIONS: Array<Migration> = arrayOf()

        fun build(context: Context): LabDatabase =
            Room.databaseBuilder(context.applicationContext, LabDatabase::class.java, NAME)
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
