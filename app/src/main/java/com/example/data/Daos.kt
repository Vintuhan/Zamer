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
