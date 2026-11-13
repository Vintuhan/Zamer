package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun observeProject(projectId: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("UPDATE projects SET hasStretchCeiling = :hasStretchCeiling, updatedAt = :updatedAt WHERE id = :projectId")
    suspend fun updateProjectStretchCeiling(projectId: Long, hasStretchCeiling: Boolean, updatedAt: Long)

    @Query("UPDATE projects SET wallHeightMeters = :wallHeightMeters, updatedAt = :updatedAt WHERE id = :projectId")
    suspend fun updateProjectWallHeight(projectId: Long, wallHeightMeters: Double, updatedAt: Long)

    @Query("UPDATE projects SET projectOpeningsJson = :json, updatedAt = :updatedAt WHERE id = :projectId")
    suspend fun updateProjectOpeningsJson(projectId: Long, json: String, updatedAt: Long)

    @Query("UPDATE projects SET notesText = :notes, updatedAt = :updatedAt WHERE id = :projectId")
    suspend fun updateProjectNotes(projectId: Long, notes: String, updatedAt: Long)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: Long)
}

@Dao
interface RoomDao {
    @Query("SELECT * FROM rooms WHERE projectId = :projectId ORDER BY sortOrder ASC, id ASC")
    fun observeRoomsForProject(projectId: Long): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms ORDER BY sortOrder ASC, id ASC")
    fun observeAllRooms(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE id = :roomId")
    suspend fun getRoomById(roomId: Long): RoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: RoomEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRooms(rooms: List<RoomEntity>)

    @Update
    suspend fun updateRoom(room: RoomEntity)

    @Query("UPDATE rooms SET wallsJson = :wallsJson WHERE id = :roomId")
    suspend fun updateWallsJson(roomId: Long, wallsJson: String)

    @Query("UPDATE rooms SET openingsJson = :openingsJson WHERE id = :roomId")
    suspend fun updateOpeningsJson(roomId: Long, openingsJson: String)

    @Query(
        """
        UPDATE rooms SET
            heightOverrideMeters = :heightOverride,
            hasStretchCeiling = :hasStretchCeiling,
            heightBeforeCeilingMeters = :heightBefore,
            heightAfterCeilingMeters = :heightAfter
        WHERE id = :roomId
        """
    )
    suspend fun updateCeilingAndHeight(
        roomId: Long,
        heightOverride: Double,
        hasStretchCeiling: Boolean,
        heightBefore: Double?,
        heightAfter: Double?
    )

    @Query("UPDATE rooms SET hasStretchCeiling = :hasStretchCeiling WHERE projectId = :projectId")
    suspend fun updateRoomsStretchCeilingForProject(projectId: Long, hasStretchCeiling: Boolean)

    @Query(
        """
        UPDATE rooms SET
            heightOverrideMeters = :heightOverride,
            hasStretchCeiling = :hasStretchCeiling,
            heightBeforeCeilingMeters = :heightBefore,
            heightAfterCeilingMeters = :heightAfter
        WHERE projectId = :projectId
        """
    )
    suspend fun applyCeilingAndHeightToAllRooms(
        projectId: Long,
        heightOverride: Double,
        hasStretchCeiling: Boolean,
        heightBefore: Double?,
        heightAfter: Double?
    )

    @Query("UPDATE rooms SET heightOverrideMeters = :heightMeters, heightBeforeCeilingMeters = :heightMeters, heightAfterCeilingMeters = :heightMeters WHERE projectId = :projectId")
    suspend fun updateAllRoomHeights(projectId: Long, heightMeters: Double)

    @Query("UPDATE rooms SET floorAreaOverrideMeters = :floorOverride WHERE id = :roomId")
    suspend fun updateFloorOverride(roomId: Long, floorOverride: Double?)

    @Query("UPDATE rooms SET name = :name WHERE id = :roomId")
    suspend fun updateRoomName(roomId: Long, name: String)

    @Query("SELECT * FROM rooms WHERE projectId = :projectId ORDER BY sortOrder ASC, id ASC")
    suspend fun getRoomsForProjectSync(projectId: Long): List<RoomEntity>

    @Query("DELETE FROM rooms WHERE projectId = :projectId")
    suspend fun deleteRoomsForProject(projectId: Long)

    @Query("DELETE FROM rooms WHERE id = :roomId")
    suspend fun deleteRoomById(roomId: Long)
}

@Dao
interface MinorWorkDao {
    @Query("SELECT * FROM minor_works WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeWorksForProject(projectId: Long): Flow<List<MinorWorkEntity>>

    @Query("SELECT * FROM minor_works ORDER BY createdAt DESC")
    fun observeAllWorks(): Flow<List<MinorWorkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWork(work: MinorWorkEntity): Long

    @Update
    suspend fun updateWork(work: MinorWorkEntity)

    @Query("DELETE FROM minor_works WHERE id = :workId")
    suspend fun deleteWorkById(workId: Long)
}

@Dao
interface SavedEstimateDao {
    @Query("SELECT * FROM saved_estimates WHERE projectId = :projectId ORDER BY savedAt DESC")
    fun observeEstimatesForProject(projectId: Long): Flow<List<SavedEstimateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEstimate(estimate: SavedEstimateEntity): Long

    @Query("DELETE FROM saved_estimates WHERE id = :estimateId")
    suspend fun deleteEstimateById(estimateId: Long)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_entries WHERE projectId = :projectId ORDER BY timestamp DESC, id DESC")
    fun observeHistoryForProject(projectId: Long): Flow<List<HistoryEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: HistoryEntryEntity): Long

    @Query("DELETE FROM history_entries WHERE projectId = :projectId")
    suspend fun clearHistoryForProject(projectId: Long)
}

@Dao
interface CalculatorStateDao {
    @Query("SELECT * FROM calculator_states WHERE projectId = :projectId")
    fun observeState(projectId: Long): Flow<CalculatorStateEntity?>

    @Query("SELECT * FROM calculator_states WHERE projectId = :projectId")
    suspend fun getState(projectId: Long): CalculatorStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertState(state: CalculatorStateEntity)
}

@Dao
interface MaterialExpenseDao {
    @Query("SELECT * FROM material_expenses WHERE projectId = :projectId ORDER BY createdAt DESC, id DESC")
    fun observeForProject(projectId: Long): Flow<List<MaterialExpenseEntity>>

    @Query("SELECT * FROM material_expenses ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MaterialExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MaterialExpenseEntity): Long

    @Query("DELETE FROM material_expenses WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface MoneyEntryDao {
    @Query("SELECT * FROM money_entries WHERE projectId = :projectId ORDER BY createdAt DESC, id DESC")
    fun observeForProject(projectId: Long): Flow<List<MoneyEntryEntity>>

    @Query("SELECT * FROM money_entries ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<MoneyEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MoneyEntryEntity): Long

    @Query("DELETE FROM money_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}
