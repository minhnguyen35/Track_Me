package com.example.trackme.viewmodel

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.example.trackme.TrackMeApplication
import com.example.trackme.repo.SessionRepository
import com.example.trackme.repo.entity.Position
import com.example.trackme.repo.entity.Session
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


typealias segment = MutableList<LatLng>
typealias line = MutableMap<Int, segment>

class RecordingViewModel(
    private val sessionRepository: SessionRepository
) : ViewModel() {
    val TAG = "RECORD"

    var isRecording = MutableLiveData(true)
    val session = MutableLiveData<Session?>()
    val path = MutableLiveData<line>()
    val lastPosition = MutableLiveData<Position>()

    private val pIsRecording
        get() = isRecording.value!!

    private val pSession
        get() = session.value

    private val pPath
        get() = path.value

    private val pLastPosition
        get() = lastPosition.value

    init {
        viewModelScope.launch {
            val id = sessionRepository.insertSession(Session.newInstance()).toInt()
            loadLiveSession(id)
            loadLiveLastPos(id)
        }
    }

    private fun MutableMap<Int, segment>.getPosList(key: Int): segment {
        if(!this.containsKey(key) || this[key] == null)
            this[key] = mutableListOf()
        return this[key]!!
    }

    private fun getLatLonBound(idSession: Int): LatLngBounds =
        sessionRepository.getLatLonBound(idSession)

    private fun saveRecord(map: GoogleMap) {

        val bound = getLatLonBound(pSession!!.id)
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bound, 50))
        saveMap(map) {
            if (it != null)
                viewModelScope.launch {
                    pSession?.let { s -> sessionRepository.sessionDao.updateMapImage(s.id, it) }
                }
        }
    }

    private fun clearData(isSave: Boolean) {
        viewModelScope.launch {
            if (isSave) {
                sessionRepository.deletePositions(session.value!!.id)
            } else {
                sessionRepository.deleteSession(session.value!!)
            }
        }
    }

    //return image's file-path
    private fun saveMap(map: GoogleMap, callback: (String?) -> Unit) =
        map.snapshot {
            if (it == null) {
                callback(null)
            } else {
                val cw = ContextWrapper(TrackMeApplication.instance.applicationContext)
                val directory: File = cw.getDir("imageDir", Context.MODE_PRIVATE)
                val path = File(directory, "map_${session.value!!.id}.jpg")

                var fos: FileOutputStream? = null
                try {
                    fos = FileOutputStream(path)
                    it.compress(Bitmap.CompressFormat.PNG, 50, fos)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    try {
                        fos?.close()
                        callback(path.absolutePath)
                    } catch (e: IOException) {
                        e.printStackTrace()
                        callback(null)
                    }
                }
            }
        }

    fun requestStartRecord() {
        startLocationService()
    }

    fun requestPauseResumeRecord() {
        if (pIsRecording) {
            isRecording.postValue(false)
            pauseLocationService()
        } else {
            isRecording.postValue(true)
            resumeLocationService()
        }
    }

    fun requestStopRecord(isSave: Boolean, map: GoogleMap? = null) {
        stopLocationService()
        if (isSave && map != null) {
            saveRecord(map)
        }
        clearData(isSave)
    }

    private fun startLocationService() {}

    private fun pauseLocationService() {}

    private fun resumeLocationService() {}

    private fun stopLocationService() {}



    private suspend fun loadLiveLastPos(id: Int) {
        viewModelScope.launch {
            sessionRepository.positionDao.getLastPosition(id).asFlow().collectLatest {
                //it can be null
                if(it != null) {
                    val map = pPath ?: mutableMapOf()
                    map.getPosList(it.segment).add(
                        LatLng(it.lat, it.lon)
                    )
                    lastPosition.postValue(it)
                    path.postValue(map)
                    Log.d(TAG, "loadLiveLastPos: ")
                }
            }
        }
    }

    private suspend fun loadLiveSession(idSession: Int) {
        viewModelScope.launch {
            sessionRepository.getSession(idSession).asFlow().collectLatest {
                session.postValue(it)
                Log.d(TAG, "loadLiveSession: ")
            }
        }
    }

}