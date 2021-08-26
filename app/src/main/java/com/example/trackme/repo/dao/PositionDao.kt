package com.example.trackme.repo.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface PositionDao {
    @Query("DELETE FROM position WHERE id_session = :idSession")
    suspend fun deletePositions(idSession: Int)
}