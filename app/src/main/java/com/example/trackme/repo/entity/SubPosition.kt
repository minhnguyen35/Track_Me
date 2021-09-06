package com.example.trackme.repo.entity

import androidx.room.ColumnInfo


data class SubPosition(
        @ColumnInfo(name ="lat") val lat: Float,
        @ColumnInfo(name = "long") val lng: Float,
        @ColumnInfo(name = "segment") val segmentId: Int
)
