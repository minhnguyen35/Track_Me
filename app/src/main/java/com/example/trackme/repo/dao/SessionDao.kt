package com.example.trackme.repo.dao

import androidx.annotation.Nullable
import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.trackme.repo.entity.Session
import kotlinx.coroutines.flow.Flow
import java.time.Duration

@Dao
interface SessionDao {
    @Query("SELECT * FROM session")
    fun getAll(): LiveData<List<Session>>

    @Query("SELECT * FROM session AS s WHERE s._id = :id")
    fun get(id: Int): LiveData<Session>

    @Insert
    suspend fun insert(session: Session) : Long

    @Update
    suspend fun update(session: Session)

    @Delete
    suspend fun delete(session: Session)

    @Query("UPDATE session SET map_path = :mapPath WHERE _id = :id")
    suspend fun updateMapImage(id: Int, mapPath: String)
}