package com.example.trackme.repo.entity

import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Session")
data class Session(
    @ColumnInfo(name = BaseColumns._ID)
    @PrimaryKey(autoGenerate = true)
    val id: Int,

    @ColumnInfo(name = "distance")
    var distance: Float,

    @ColumnInfo(name = "speed_avg")
    var speedAvg: Float,

    @ColumnInfo(name = "duration")
    var duration: Long,

    @ColumnInfo(name = "map_img")
    var mapImg: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Session

        if (id != other.id) return false
        if (distance != other.distance) return false
        if (speedAvg != other.speedAvg) return false
        if (duration != other.duration) return false
        if (!mapImg.contentEquals(other.mapImg)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + distance.hashCode()
        result = 31 * result + speedAvg.hashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + mapImg.contentHashCode()
        return result
    }
}