package com.example.trackme.repo.dao

import androidx.annotation.Nullable
import androidx.room.*
import com.example.trackme.repo.entity.Session
import kotlinx.coroutines.flow.Flow
import java.time.Duration

@Dao
interface SessionDao {
    @Query("SELECT * FROM session")
    fun getAll(): Flow<List<Session>>

    @Query("SELECT * FROM session AS s WHERE s._id = :id")
    suspend fun get(id: Int): Session

    @Query("SELECT *  FROM session")
    suspend fun getList(): List<Session>

    @Insert
    suspend fun insert(session: Session) : Long

    @Update
    suspend fun update(session: Session)

    @Delete
    suspend fun delete(session: Session)

    @Query("UPDATE session SET map_img = :mapImg WHERE _id = :id")
    suspend fun updateMapImage(id: Int, mapImg: ByteArray)
}