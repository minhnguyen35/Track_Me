package com.example.trackme.repo

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.trackme.repo.database.TrackMeDatabase
import com.example.trackme.repo.entity.Position
import com.example.trackme.repo.entity.Session
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SessionRepository @Inject constructor(private val database: TrackMeDatabase) {

    val sessionDao = database.sessionDao()
    val positionDao = database.positionDao()

    fun getSessionList(): LiveData<List<Session>> = sessionDao.getAll()

    fun getSession(id: Int): LiveData<Session> = sessionDao.get(id)

    suspend fun insertSession(session: Session): Long = sessionDao.insert(session)

    suspend fun updateSession(session: Session) {
        Log.d("PRIO", "updateSession: ")
        sessionDao.update(session)
    }

    suspend fun deleteSession(session: Session) = sessionDao.delete(session)


    suspend fun insertPosition(position: Position) = positionDao.insertPosition(position)

    suspend fun updatePosition(position: Position) = positionDao.updatePosition(position)

    suspend fun deletePositions(idSession: Int) = positionDao.deletePositions(idSession)

    fun getLatLonBound(idSession: Int): LatLngBounds {
        val c =
            database.query("SELECT * from lat_lng_range AS p WHERE p.id_session = $idSession", null)
        var minLat = 0.0
        var minLng = 0.0
        var maxLat = 0.0
        var maxLng = 0.0

        while (c.moveToNext()) {
            minLat = c.getDouble(1)
            minLng = c.getDouble(2)
            maxLat = c.getDouble(3)
            maxLng = c.getDouble(4)
        }

        return LatLngBounds(
            LatLng(minLat, minLng),
            LatLng(maxLat, maxLng)
        )
    }
}