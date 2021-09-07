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
import com.example.trackme.utils.Constants.RESUME_SERVICE
import com.example.trackme.utils.Constants.START_SERVICE
import com.example.trackme.utils.Constants.STOP_SERVICE
import com.example.trackme.utils.TrackingHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class RecordingViewModel(
        private val notificationBuilder: NotificationCompat.Builder,
        private val sessionRepo: SessionRepository,
        private val positionRepo: PositionRepository,
) : ViewModel() {

    val TAG = "RECORD"
    val session = MutableLiveData<Session?>()
    val route = MutableLiveData<List<SubPosition>>(mutableListOf())
    val isRecording = MutableLiveData(false)
    val distance = MutableLiveData(0f)
    val speed = MutableLiveData(0f)
    val timeInSec = MutableLiveData(0L)
    val listSpeed = mutableListOf<Float>()
    var id = MutableStateFlow(-1)

    private var timer: Timer? = Timer("TIMER")

    private val chronometerTask = object : TimerTask(){
        override fun run() {
            if(pIsRecording == true)
                timeInSec.postValue(timeInSec.value!! + 1)
        }
    }


    private val pSession
        get() = session.value

    private val pRoute
        get() = route.value

    private val pIsRecording
        get() = isRecording.value


    init {
        viewModelScope.launch(Dispatchers.IO) {
            val id = sessionRepo.insertSession(Session.newInstance()).toInt()
            loadLiveSession(id)
            loadLiveRoute(id)
        }
    }


//        val isGPSAvailable = MutableLiveData<Boolean>()
//        fun calculateDistance(){
//            route.let {
//                if (it.value != null) {
//                    if (it.value!!.size > 1) {
//                        val lastPos = it.value!!.last()
//                        val prevPos = it.value!![it.value!!.size - 2]
//                        if (lastPos.segmentId == prevPos.segmentId) {
//                            val startLat = prevPos.lat.toDouble()
//                            val startLong = prevPos.lng.toDouble()
//                            val endLat = lastPos.lat.toDouble()
//                            val endLong = lastPos.lng.toDouble()
//                            val prevLocation = Location("prevLocation")
//                            prevLocation.latitude = startLat
//                            prevLocation.longitude = startLong
//
//                            val lastLocation = Location("lastLocation")
//                            lastLocation.longitude = endLong
//                            lastLocation.latitude = endLat
//                            val tmpDistance = prevLocation.distanceTo(lastLocation)
//                            distance.postValue(distance.value?.plus(tmpDistance))
//                            speed.postValue(distance.value!!/ timeInSec.value!!)
//                            listSpeed.add(speed.value!!)
//                        }
//                    }
//                }
//            }
//
//        }


    private fun updateNotification(){


        val notification =
            TrackMeApplication.instance.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
        viewModelScope.launch {
            timeInSec.asFlow().collect{

                    val noti = notificationBuilder
                            .setContentText(
                                    "Distance: %.2f km\n".format(
                                            if(distance.value == null)
                                                0f
                                            else
                                                distance.value!!/1000
                                    )+
                                            TrackingHelper.formatChronometer(it))
                    notification.notify(Constants.NOTIFICATION_ID, noti.build())

            }
        }
    }

    private fun triggerService(action: String) {
        val context = TrackMeApplication.instance.applicationContext
        if (!TrackingHelper.checkPermission(context))
            return
        val i = Intent(context, MapService::class.java)
        i.action = action
        context.startService(i)
    }

    fun requestStartRecord() {
        if (pIsRecording == false) {
            startLocationService()
            updateNotification()
            runChronometer()
            isRecording.postValue(true)
        }
    }
    fun requestPauseResumeRecord() {
        if (isRecording.value != null && isRecording.value == true) {
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
        val s = session.value!!
        s.apply {
            distance = this@RecordingViewModel.distance.value!!
            speedAvg = this@RecordingViewModel.listSpeed.average().toFloat()
            duration = this@RecordingViewModel.timeInSec.value!!
        }
        viewModelScope.launch {
            sessionRepo.updateSession(s)
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


    //call one-time only
    private fun runChronometer() {
        timer?.schedule(chronometerTask, 0, 1000)
    }

    private fun startLocationService() {
        triggerService(START_SERVICE)
    }

    private fun pauseLocationService() {
        triggerService(PAUSE_SERVICE)
    }

    private fun resumeLocationService() {
        triggerService(RESUME_SERVICE)
    }

    private fun stopLocationService() {
        triggerService(STOP_SERVICE)
    }


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
            }
        }
    }


    private fun loadLiveRoute(id: Int) {
        viewModelScope.launch {
            positionRepo.getCurrentPath(id).collectLatest {
                route.postValue(it)
            }
        }
    }

    override fun onCleared() {
        timer?.purge()
        timer = null

        super.onCleared()
    }

}