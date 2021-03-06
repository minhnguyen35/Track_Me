package com.example.trackme.repo.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.trackme.repo.dao.PositionDao
import com.example.trackme.repo.dao.SessionDao
import com.example.trackme.repo.entity.LatLngRange
import com.example.trackme.repo.entity.Position
import com.example.trackme.repo.entity.Session

@Database(
    entities = [Session::class, Position::class],
    views = [LatLngRange::class],
    version = 1
)
abstract class TrackMeDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun positionDao(): PositionDao
}