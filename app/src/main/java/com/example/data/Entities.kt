package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String,
    val startDate: String,
    val layoutType: String,
    val layoutTitle: String,
    val wallHeightMeters: Double = 2.50,
    val wallThicknessMeters: Double = 0.15,
    val wallRateRub: Double = 0.0,
    val floorRateRub: Double = 0.0,
    val cornerRateRub: Double = 0.0,
    val useOuterFloorInEstimate: Boolean = false,
    val customDrawingJson: String = "[]",
    val estimateCategoriesJson: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "rooms",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class RoomEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val sortOrder: Int = 0,
    val wallCount: Int = 4,
    val wallsJson: String = "[]",
    val heightOverrideMeters: Double? = null,
    val hasStretchCeiling: Boolean = false,
    val heightBeforeCeilingMeters: Double? = null,
    val heightAfterCeilingMeters: Double? = null,
    val openingsJson: String = "[]",
    val innerCorners: Int = 4,
    val outerCorners: Int = 0,
    val extraCornerMeters: Double = 0.0,
    val floorAreaOverrideMeters: Double? = null
)

@Entity(
    tableName = "minor_works",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class MinorWorkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val price: Double,
    val quantity: Double = 1.0,
    val unitLabel: String = "шт",
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalCost: Double get() = price * quantity
}

@Entity(
    tableName = "saved_estimates",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class SavedEstimateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val savedAt: Long = System.currentTimeMillis(),
    val title: String,
    val summaryText: String,
    val grandTotalRub: Double
)

@Entity(
    tableName = "history_entries",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId"), Index("timestamp")]
)
data class HistoryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String, // "CALC", "MEASURE", "WORK", "ESTIMATE"
    val title: String,
    val expression: String,
    val resultValue: Double?,
    val unit: String = ""
)

@Entity(
    tableName = "calculator_states",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CalculatorStateEntity(
    @PrimaryKey val projectId: Long,
    val expressionText: String = "",
    val currentInput: String = "0",
    val resultPreview: Double = 0.0,
    val activeUnit: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
