package com.example.trackme.repo.dao

import androidx.annotation.Nullable
import androidx.room.*
import com.example.trackme.repo.entity.Session
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM session")
    fun getAll(): Flow<List<Session>>

    @Query("SELECT s._id, s.distance, s.speed_avg, s.duration, null  FROM session AS s")
    suspend fun getRawList(): List<Session>

    @Query("SELECT s.map_img FROM session AS s WHERE s._id = :id")
    fun getImage(id: Int): Flow<ByteArray?>

    @Insert
    suspend fun insert(session: Session)

    @Update
    suspend fun update(session: Session)

    @Delete
    suspend fun delete(session: Session)
}