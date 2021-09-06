package com.example.trackme.repo.dao

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.trackme.repo.entity.LatLngRange
import com.example.trackme.repo.entity.Position
import com.example.trackme.repo.entity.SubPosition
import com.example.trackme.viewmodel.segment
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.Flow
import java.util.*

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

    @Query("SELECT p.lat, p.long,p.segment FROM position as p WHERE p.id_session = :idSession")
    fun getCurrentPath(idSession: Int): Flow<List<SubPosition>>
//    @Query("SELECT * from lat_lng_range AS p WHERE p.id_session = :idSession")
//    suspend fun getLatLngRange(idSession: Int): Cursor
    @Query("SELECT MAX(id_session) FROM position")
    fun getLastSession(): Flow<Int>

}