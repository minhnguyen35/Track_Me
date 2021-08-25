package com.example.trackme.repo.dao

import androidx.annotation.Nullable
import androidx.room.*
import com.example.trackme.repo.entity.Session
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM session")
    fun getAll(): Flow<List<Session>>

    @Query("SELECT *  FROM session")
    suspend fun getList(): List<Session>

    @Insert
    suspend fun insert(session: Session)

    @Update
    suspend fun update(session: Session)

    @Delete
    suspend fun delete(session: Session)
}