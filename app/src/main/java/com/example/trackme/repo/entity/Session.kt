package com.example.trackme.repo.entity

import android.provider.BaseColumns
import androidx.annotation.Nullable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.io.Serializable

@Entity(tableName = "session")
data class Session(
    @ColumnInfo(name = BaseColumns._ID)
    @PrimaryKey(autoGenerate = true)
    var id: Int,

    @ColumnInfo(name = "distance")
    var distance: Float,

    @ColumnInfo(name = "speed_avg")
    var speedAvg: Float,

    @ColumnInfo(name = "duration")
    var duration: Long,

    @ColumnInfo(name = "map_path")
    var mapImg: String,

) : Serializable {

    companion object{
        fun newInstance() = Session(0, 0f, 0f, 0, "")
    }
}