package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProjectEntity::class,
        RoomEntity::class,
        MinorWorkEntity::class,
        SavedEstimateEntity::class,
        HistoryEntryEntity::class,
        CalculatorStateEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun roomDao(): RoomDao
    abstract fun minorWorkDao(): MinorWorkDao
    abstract fun savedEstimateDao(): SavedEstimateDao
    abstract fun historyDao(): HistoryDao
    abstract fun calculatorStateDao(): CalculatorStateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN estimateCategoriesJson TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE rooms ADD COLUMN hasStretchCeiling INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE rooms ADD COLUMN heightBeforeCeilingMeters REAL")
                db.execSQL("ALTER TABLE rooms ADD COLUMN heightAfterCeilingMeters REAL")
                db.execSQL("ALTER TABLE calculator_states ADD COLUMN activeUnit TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS saved_estimates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        projectId INTEGER NOT NULL,
                        savedAt INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        summaryText TEXT NOT NULL,
                        grandTotalRub REAL NOT NULL,
                        FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_saved_estimates_projectId ON saved_estimates(projectId)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zamershchik_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
