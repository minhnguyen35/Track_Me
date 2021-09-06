package com.example.trackme.viewmodel

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.Location.distanceBetween
import android.util.Log
import androidx.lifecycle.*
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
import com.example.trackme.repo.PositionRepository
import com.example.trackme.repo.SessionRepository
import com.example.trackme.repo.entity.Session
import com.example.trackme.repo.entity.SubPosition
import com.example.trackme.utils.Constants.START_SERVICE
import com.example.trackme.utils.RecordState
import com.example.trackme.utils.TrackingHelper
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.switchMap
import kotlinx.coroutines.launch

class RecordingViewModel(
        private val sessionRepo: SessionRepository,
        private val positionRepo: PositionRepository,
        private val appPreferences: SharedPreferences
) : ViewModel() {

    fun requestStartRecord() {
        startLocationService()
    }
    init {
        Log.d("recordviewmodel", "construct recording viewmodel ${this.hashCode()}")

    }
    fun getLastSessionId() {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("recordviewmodel", "session id is : ${sessionId.value}")

//            sessionRepo.updateSession(Session.newInstance())
            sessionId.value = sessionRepo.getLastSessionID()


        }
    }

    private val sessionId = MutableStateFlow(-1)

    var path: LiveData<List<SubPosition>> = sessionId.flatMapLatest {
        if(it == -1)
        {

            positionRepo.getCurrentPath(-1)
        }
        else
            positionRepo.getCurrentPath(it)

    }.asLiveData()

        val isRunning = MutableLiveData<Boolean>()
//    val path = MutableLiveData<line>()
        val distance = MutableLiveData<Float>()
        val speed = MutableLiveData<Float>()
        val timeInSec = MutableLiveData<Long>()
        val session = MutableLiveData<Session>()
        val isGPSAvailable = MutableLiveData<Boolean>()
        val listSpeed = mutableListOf<Float>()

        fun calculateDistance(){
            path?.let {
                if (it.value != null) {
                    if (it.value!!.size > 1) {
                        val lastPos = it.value!!.last()
                        val prevPos = it.value!![it.value!!.size - 2]
                        if (lastPos.segmentId == prevPos.segmentId) {
                            val startLat = prevPos.lat.toDouble()
                            val startLong = prevPos.lng.toDouble()
                            val endLat = lastPos.lat.toDouble()
                            val endLong = lastPos.lng.toDouble()
                            val prevLocation = Location("prevLocation")
                            prevLocation.latitude = startLat
                            prevLocation.longitude = startLong

                            val lastLocation = Location("lastLocation")
                            lastLocation.longitude = endLong
                            lastLocation.latitude = endLat
                            val tmpDistance = prevLocation.distanceTo(lastLocation)
                            distance.postValue(distance.value?.plus(tmpDistance))
                        }
                    }
                }
            }

        }

        fun initParam(){
            //initSession()
//            viewModelScope.launch(Dispatchers.IO) {
//                sessionId = sessionRepo.getLastSessionID()
//                Log.d("recordviewmodel", "session id is : $sessionId")
////                Log.d("recordviewmodel", "path $sessionId size: ${path?.value?.size}")
//
//            }
            Log.d("recordviewmodel", "session id is : $sessionId")

//            path = positionRepo.getCurrentPath(sessionId).asLiveData()

            timeInSec.postValue(0L)
            isRunning.postValue(false)
//        path.postValue(mutableListOf())
            distance.postValue(0f)
            speed.postValue(0f)
            isGPSAvailable.postValue(false)
            isRunning.postValue(false)
//        path.postValue(mutableListOf())
        }
        fun initSession(){
            viewModelScope.launch(Dispatchers.IO) {
                sessionRepo.updateSession(Session.newInstance())
                Log.d("initSession", "session id is ${sessionId}")
                Log.d("initSession", "path is ${path?.value?.size}")
            }

        }

        fun triggerService(context: Context ,action: String){
            if(!TrackingHelper.checkPermission(context))
                return
            val i = Intent(context, MapService::class.java)
            i.action = action
            context.startService(i)
        }
        var recordState: MutableLiveData<RecordState> = MutableLiveData(RecordState.RECORDING)

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


}