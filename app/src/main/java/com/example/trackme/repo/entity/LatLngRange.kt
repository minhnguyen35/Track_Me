package com.example.trackme.repo.entity

import androidx.room.ColumnInfo
import androidx.room.DatabaseView

@DatabaseView(
    "select id_session, MIN(lat), MIN(long), MAX(lat), MAX(long) from position group by id_session",
    viewName = "lat_lng_range"
)
data class LatLngRange(
    @ColumnInfo(name = "id_session")
    val idSession: Int,

    @ColumnInfo(name = "min_lat")
    val minLat: Double,

    @ColumnInfo(name = "min_lng")
    val minLng: Double,

    @ColumnInfo(name = "max_lat")
    val maxLat: Double,

    @ColumnInfo(name = "max_ng")
    val maxLng: Double,
)