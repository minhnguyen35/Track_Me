package com.example.trackme.repository

import com.example.trackme.repo.database.TrackMeDatabase
import com.example.trackme.repo.entity.Session
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SessionRepository @Inject constructor(database: TrackMeDatabase) {

    val sessionDao = database.sessionDao()
    val positionDao = database.positionDao()

    fun getSessionList(): Flow<List<Session>> = sessionDao.getAll()

    suspend fun insertSession(session: Session) : Long = sessionDao.insert(session)

    suspend fun updateSession(session: Session) = sessionDao.update(session)

    suspend fun deleteSession(session: Session) = sessionDao.delete(session)

    suspend fun deletePositions(idSession: Int) = positionDao.deletePositions(idSession)
}