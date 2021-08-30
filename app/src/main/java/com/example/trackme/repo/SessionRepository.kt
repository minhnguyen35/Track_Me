package com.example.trackme.repository

import com.example.trackme.repo.database.TrackMeDatabase
import com.example.trackme.repo.entity.Position
import com.example.trackme.repo.entity.Session
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SessionRepository @Inject constructor(database: TrackMeDatabase) {

    val sessionDao = database.sessionDao()
    val positionDao = database.positionDao()

    fun getSessionList(): Flow<List<Session>> = sessionDao.getAll()

    suspend fun getSession(id: Int): Session = sessionDao.get(id)

    suspend fun insertSession(session: Session) : Long = sessionDao.insert(session)

    suspend fun updateSession(session: Session) =
        sessionDao.update(session)

    suspend fun deleteSession(session: Session) = sessionDao.delete(session)


    suspend fun insertPosition(position: Position) = positionDao.insertPosition(position)

    suspend fun updatePosition(position: Position) = positionDao.updatePosition(position)

    suspend fun deletePositions(idSession: Int) = positionDao.deletePositions(idSession)
}