package com.example.trackme.repo.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.trackme.repo.entity.Position
import com.example.trackme.viewmodel.segment

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

    @Query("SELECT * FROM position AS p WHERE p.id_session = :idSession AND p.segment = :segment")
    suspend fun getPositions(idSession: Int, segment: Int) : List<Position>

}