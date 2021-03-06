package com.example.trackme.repo.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.trackme.repo.entity.Position
import com.example.trackme.repo.entity.SubPosition
import kotlinx.coroutines.flow.Flow

@Dao
interface PositionDao {
    @Query("DELETE FROM position WHERE id_session = :idSession")
    suspend fun deletePositions(idSession: Int)

    @Insert
    suspend fun insertPosition(position: Position)

    @Update
    suspend fun updatePosition(position: Position)

    @Query("SELECT MAX(p.segment) FROM position AS p WHERE p.id_session = :idSession")
    suspend fun segmentCount(idSession: Int) : Int

    @Query("SELECT * FROM position AS p WHERE p.id_session = :idSession")
    fun getPositions(idSession: Int) : LiveData<List<Position>>

    @Query("SELECT * FROM position AS p WHERE p.id_session = :idSession ORDER BY P._id DESC LIMIT 1")
    fun getLastPosition(idSession: Int) : LiveData<Position>

    @Query("SELECT p.lat, p.long,p.segment FROM position as p WHERE p.id_session = :idSession")
    fun getCurrentPath(idSession: Int): Flow<List<SubPosition>>

    //    @Query("SELECT * from lat_lng_range AS p WHERE p.id_session = :idSession")
//    suspend fun getLatLngRange(idSession: Int): Cursor
    @Query("SELECT MAX(id_session) FROM position")
    fun getLastSession(): Flow<Int>




}