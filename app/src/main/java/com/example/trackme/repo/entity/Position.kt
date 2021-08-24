package com.example.trackme.repo.entity

import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "position",
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
    var lat: Float,

    @ColumnInfo(name = "long")
    var lon: Float,

    @ColumnInfo(name = "id_session")
    var idSession: Int
)