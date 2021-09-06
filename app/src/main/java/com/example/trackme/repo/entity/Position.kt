package com.example.trackme.repo.entity

import android.provider.BaseColumns
import androidx.room.*

@Entity(
    tableName = "position",
    indices = [
        Index("id_session")
    ],
    foreignKeys = [
        ForeignKey(
            entity = Session::class,
            parentColumns = [BaseColumns._ID],
            childColumns = ["id_session"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class Position(
    @ColumnInfo(name = BaseColumns._ID)
    @PrimaryKey(autoGenerate = true)
    val id: Int,

    @ColumnInfo(name = "lat")
    var lat: Double,

    @ColumnInfo(name = "long")
    var lon: Double,

    @ColumnInfo(name = "segment")
    var segment: Int,

    @ColumnInfo(name = "id_session")
    var idSession: Int
)