package com.example.trackme.viewmodel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.Location.distanceBetween
import androidx.lifecycle.*
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startForegroundService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.example.trackme.R
import com.example.trackme.TrackMeApplication
import com.example.trackme.repo.entity.Position
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException



import com.example.trackme.repo.PositionRepository
import com.example.trackme.repo.SessionRepository
import com.example.trackme.repo.entity.Session
import com.example.trackme.repo.entity.SubPosition
import com.example.trackme.utils.Constants
import com.example.trackme.utils.Constants.PAUSE_SERVICE
import com.example.trackme.utils.Constants.START_SERVICE
import com.example.trackme.utils.RecordState
import com.example.trackme.utils.TrackingHelper
import com.example.trackme.view.activity.RecordingActivity
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class RecordingViewModel(
        private val notificationBuilder: NotificationCompat.Builder,
        private val sessionRepo: SessionRepository,
        private val positionRepo: PositionRepository,
) : ViewModel() {

    val TAG = "RECORD"
    val session = MutableLiveData<Session?>()
    private val pSession
        get() = session.value
    var id = MutableStateFlow(-1)



    var route: LiveData<List<SubPosition>> = id.flatMapLatest {
        if(it == -1)
        {
            positionRepo.getCurrentPath(-1)
        }
        else
            positionRepo.getCurrentPath(it)

    }.asLiveData()

    var isOpening = false
    val isRecording = MutableLiveData(false)
    val distance = MutableLiveData(0f)
    val speed = MutableLiveData(0f)
    val timeInSec = MutableLiveData(0L)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            id.value = sessionRepo.insertSession(Session.newInstance()).toInt()
            Log.d(TAG, "id: ${id.value}")
            loadLiveSession(id.value)
//            loadLiveLastPos(id)

        }
    }

    private var startTime: Long = 0
    private var diffTime: Long = 0
    private val timeInMill = MutableLiveData(0L)
    private var runTime = 0L
    private var lastTimestamp = 0L
    private fun runChronometer(){
//        Log.d("MAPSERVICE", "run chronometer")
//        if(MapService.isRunning.value == null || MapService.isRunning.value == false) {
////            addNewSegment()
//            segmentId++
//            Log.d("MAPSERVICE", "add new segment")
//        }
        isRecording.postValue(true)

        startTime = System.currentTimeMillis()
        Log.d("Timer viewmodel","start time $startTime")
        viewModelScope.launch {
            delay(10)
            while(isRecording.value!!){
                diffTime = System.currentTimeMillis() - startTime
                timeInMill.postValue(diffTime+runTime)
                if(timeInMill.value!! >= lastTimestamp + 1000L){
                    timeInSec.postValue(timeInSec.value!!+1)
                    lastTimestamp += 1000L
                    Log.d("Timer viewmodel", "${timeInSec.value!!}")
//

                }
                //frequency of updated time
                delay(200)
            }
            runTime += diffTime
            Log.d("Timer","run time $runTime")

        }
    }




//        val isGPSAvailable = MutableLiveData<Boolean>()
        val listSpeed = mutableListOf<Float>()
        fun calculateDistance(){
            route?.let {
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
                            speed.postValue(distance.value!!/ session.value!!.duration)
                            listSpeed.add(speed.value!!)
                        }
                    }
                }
            }

        }


    private fun updateNotification(){


        val notification = TrackMeApplication.instance.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                            as NotificationManager
        viewModelScope.launch {
            timeInSec.asFlow().collect{

                    val noti = notificationBuilder
                            .setContentText(
                                    "Distance: %.2f km\n".format(
                                            if(MapService.distance.value == null)
                                                0f
                                            else
                                                MapService.distance.value!!/1000
                                    )+
                                            TrackingHelper.formatChronometer(it))
                    notification.notify(Constants.NOTIFICATION_ID, noti.build())

            }
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

    fun requestPauseResumeRecord(context: Context) {
        if (isRecording.value!= null && isRecording.value==true) {
            isRecording.postValue(false)
            triggerService(context, PAUSE_SERVICE)
        } else {
            isRecording.postValue(true)
            runChronometer()
            triggerService(context, START_SERVICE)
        }
    }

    fun requestStopRecord(isSave: Boolean, map: GoogleMap? = null) {
//        stopLocationService()

//        if (isSave && map != null) {
//            saveRecord(map)
//        }
        clearData(isSave)
    }
    private fun getLatLonBound(idSession: Int): LatLngBounds =
            sessionRepo.getLatLonBound(idSession)
    private fun saveRecord(map: GoogleMap) {

        val bound = getLatLonBound(pSession!!.id)
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bound, 50))
        saveMap(map) {
            if (it != null)
                viewModelScope.launch {
                    pSession?.let { s -> sessionRepo.sessionDao.updateMapImage(s.id, it) }
                }
        }
    }
    private fun clearData(isSave: Boolean) {
        viewModelScope.launch {
            if (isSave) {
                sessionRepo.deletePositions(session.value!!.id)
            } else {
                sessionRepo.deleteSession(session.value!!)
            }
        }
    }
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

//    private fun startLocationService() {}
//
//    private fun pauseLocationService() {}
//
//    private fun resumeLocationService() {}
//
//    private fun stopLocationService() {}



//    private suspend fun loadLiveLastPos(id: Int) {
//        viewModelScope.launch {
//            sessionRepository.positionDao.getLastPosition(id).asFlow().collectLatest {
//                //it can be null
//                if(it != null) {
//                    val map = pPath ?: mutableMapOf()
//                    map.getPosList(it.segment).add(
//                        LatLng(it.lat, it.lon)
//                    )
//                    lastPosition.postValue(it)
//                    path.postValue(map)
//                    Log.d(TAG, "loadLiveLastPos: ")
//                }
//            }
//        }
//    }
    private suspend fun loadLiveSession(idSession: Int) {
        viewModelScope.launch {
            sessionRepo.getSession(idSession).asFlow().collectLatest {
                session.postValue(it)
//                Log.d(TAG, "loadLiveSession: ")
            }
        }
    }

    fun startService(context: Context) {
        if(isOpening == false) {
            triggerService(context, START_SERVICE)
            updateNotification()
            runChronometer()
            isOpening = true
        }
    }

}