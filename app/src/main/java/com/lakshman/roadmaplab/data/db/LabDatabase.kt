package com.lakshman.roadmaplab.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        PhaseEntity::class, TaskEntity::class, MilestoneEntity::class, CardEntity::class,
        CardStateEntity::class, TaskLogEntity::class, DayLogEntity::class, ReviewLogEntity::class,
        DayPlanEntity::class, MetaEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class LabDatabase : RoomDatabase() {
    abstract fun dao(): LabDao

    companion object {
        /**
         * One Migration per schema change, e.g. MIGRATION_1_2 adding a column with a default value.
         * The app never uses destructive migration, so an update can't wipe your history:
         * if a migration were ever missing, the app would stop with an error instead of deleting data.
         */
        val MIGRATIONS: Array<Migration> = arrayOf()

        fun build(context: Context): LabDatabase =
            Room.databaseBuilder(context.applicationContext, LabDatabase::class.java, "roadmaplab.db")
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
