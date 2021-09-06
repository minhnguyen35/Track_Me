package com.example.trackme.repo

import com.example.trackme.repo.database.TrackMeDatabase
import com.example.trackme.repo.entity.SubPosition
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PositionRepository @Inject constructor(private val database: TrackMeDatabase) {
    val positionDao = database.positionDao()
    fun getCurrentPath(idSession: Int): Flow<List<SubPosition>> = positionDao.getCurrentPath(idSession)
}