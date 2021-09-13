package com.example.trackme.viewmodel


import android.app.NotificationManager
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Context.NOTIFICATION_SERVICE
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import com.example.trackme.R
import com.example.trackme.TrackMeApplication
import com.example.trackme.repo.SessionRepository
import com.example.trackme.repo.entity.Position
import com.example.trackme.repo.entity.Session
import com.example.trackme.service.MapService
import com.example.trackme.utils.Constants
import com.example.trackme.utils.Constants.PAUSE_SERVICE
import com.example.trackme.utils.Constants.RESUME_SERVICE
import com.example.trackme.utils.Constants.START_SERVICE
import com.example.trackme.utils.Constants.STOP_SERVICE
import com.example.trackme.utils.TrackingHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class RecordingViewModel(
        private val notificationBuilder: NotificationCompat.Builder,
        private val sessionRepo: SessionRepository,
) : ViewModel() {

    val TAG = "RECORD"
    val session = MutableLiveData<Session?>()
    val lastPosition = MutableLiveData<Position>()
    val isRecording = MutableLiveData(false)
    val distance = MutableLiveData(0f)
    val speed = MutableLiveData(0f)
    val timeInSec = MutableLiveData(0L)
    var lastTimestamp = 0L
    val listSpeed = mutableListOf<Float>()
    var isStart = false
    var missingSegment: MutableSet<Int> = mutableSetOf()
    val missingRoute: MutableMap<Int, PolylineOptions> = mutableMapOf()
    var isGpsEnable = false
    var isInBackground = false
    var timer: Timer? = null


    private val pSession
        get() = session.value


    private val pIsRecording
        get() = isRecording.value


    init {
        viewModelScope.launch(Dispatchers.IO) {
            val id = sessionRepo.insertTempSession().toInt()
            loadLiveSession(id)
            loadLiveLastPos(id)
        }
    }

    fun getGpsAtBeginning(context: Context){
        val locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
        isGpsEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun updateSessionInfo(lastPosition: Position?, newPosition: Position) {
        if (lastPosition == null || (lastPosition.segment != newPosition.segment && !isInBackground)) return
        val lastLocation = Location("lastLocation").apply {
            latitude = lastPosition.lat
            longitude = lastPosition.lon
        }

        val newLocation = Location("newLocation").apply {
            latitude = newPosition.lat
            longitude = newPosition.lon
        }

        val tmpDistance = lastLocation.distanceTo(newLocation)
        distance.postValue(distance.value?.plus(tmpDistance))
        speed.postValue(tmpDistance / (timeInSec.value!!-lastTimestamp))
        lastTimestamp = timeInSec.value!!
        listSpeed.add(speed.value!!)
    }

    private fun pauseNotificationStateChange(){
        val notificationManager =
            TrackMeApplication.instance.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        val noti = notificationBuilder
            .setContentTitle("Your tracking paused")
            .setContentText(
                "Distance: %.2f km\n".format(
                    if (distance.value == null)
                        0f
                    else
                        distance.value!! / 1000
                ) + "   Time: " +
                        TrackingHelper.formatChronometer(timeInSec.value!!)
            )
        notificationManager.notify(Constants.NOTIFICATION_ID, noti.build())
    }

    private fun updateNotification(){


        val notification =
            TrackMeApplication.instance.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
        viewModelScope.launch {
            timeInSec.asFlow().collect{

                val noti = notificationBuilder
                    .setContentTitle("You're running")
                    .setContentText(
                        "Distance: %.2f km\n".format(
                            if (distance.value == null)
                                0f
                            else
                                distance.value!! / 1000
                        ) + "   Time: " +
                                TrackingHelper.formatChronometer(it)
                    )
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
        i.putExtra(MapService.ID_SESSION, session.value?.id ?: -1)

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
            runChronometer(false)
            pauseNotificationStateChange()
        } else {
            isRecording.postValue(true)
            resumeLocationService()
            runChronometer()
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
            speedAvg =
                if (this@RecordingViewModel.listSpeed.isEmpty()) 0f else this@RecordingViewModel.listSpeed.average()
                    .toFloat()
            duration = this@RecordingViewModel.timeInSec.value!!
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                sessionRepo.updateSession(s)
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


    private fun runChronometer(isRun: Boolean = true) {
        if (isRun) {
            timer = Timer("TIMER")
            timer?.schedule(ChronometerTask(timeInSec), 0, 1000)
        } else {
            timer?.cancel()
        }
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

    private suspend fun loadLiveLastPos(id: Int) {
        viewModelScope.launch {
            sessionRepo.positionDao.getLastPosition(id).asFlow().collectLatest {
                if (it != null) {
                    if (!isInBackground)
                        lastPosition.postValue(it)
                    else {
                        missingSegment.add(it.segment)
                        getPolyValue(it.segment).add(LatLng(it.lat, it.lon))
                    }
                    updateSessionInfo(lastPosition.value, it)
                    Log.d(TAG, "loadLiveLastPos: ")
                }
            }
        }
    }

    private suspend fun loadLiveSession(idSession: Int) {
        viewModelScope.launch {
            sessionRepo.getSession(idSession).asFlow().collectLatest {
                session.postValue(it)
            }
        }
    }

    fun getPolyValue(key: Int): PolylineOptions {
        if (!missingRoute.containsKey(key))
            missingRoute[key] = PolylineOptions().color(R.color.purple_200)
        return missingRoute[key]!!
    }

    override fun onCleared() {
        timer?.purge()
        timer = null
        stopLocationService()
        super.onCleared()
    }

    class ChronometerTask(val time: MutableLiveData<Long>) : TimerTask(){
        override fun run() {
            time.postValue(time.value?.plus(1) ?: 0)
        }

    }


}