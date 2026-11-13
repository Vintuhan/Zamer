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
        CalculatorStateEntity::class,
        MaterialExpenseEntity::class,
        MoneyEntryEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun roomDao(): RoomDao
    abstract fun minorWorkDao(): MinorWorkDao
    abstract fun savedEstimateDao(): SavedEstimateDao
    abstract fun historyDao(): HistoryDao
    abstract fun calculatorStateDao(): CalculatorStateDao
    abstract fun materialExpenseDao(): MaterialExpenseDao
    abstract fun moneyEntryDao(): MoneyEntryDao

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN hasStretchCeiling INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE rooms ADD COLUMN shapeKey TEXT NOT NULL DEFAULT 'AUTO'")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN projectOpeningsJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE projects ADD COLUMN notesText TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS material_expenses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        projectId INTEGER NOT NULL,
                        itemName TEXT NOT NULL,
                        priceRub REAL NOT NULL,
                        purchaseDate TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_material_expenses_projectId ON material_expenses(projectId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_material_expenses_createdAt ON material_expenses(createdAt)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS money_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        projectId INTEGER NOT NULL,
                        amountRub REAL NOT NULL,
                        paymentMethod TEXT NOT NULL,
                        dateStr TEXT NOT NULL,
                        reasonText TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_money_entries_projectId ON money_entries(projectId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_money_entries_createdAt ON money_entries(createdAt)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zamershchik_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
