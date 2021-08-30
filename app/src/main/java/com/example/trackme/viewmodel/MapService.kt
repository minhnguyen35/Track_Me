package com.example.trackme.viewmodel

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.example.trackme.R
import com.example.trackme.TrackMeApplication
import com.example.trackme.repo.entity.Position
import com.example.trackme.repository.SessionRepository
import com.example.trackme.utils.Constants.NOTIFICATION_CHANNEL
import com.example.trackme.utils.Constants.NOTIFICATION_CHANNEL_ID
import com.example.trackme.utils.Constants.NOTIFICATION_ID
import com.example.trackme.utils.Constants.PAUSE_SERVICE
import com.example.trackme.utils.Constants.START_SERVICE
import com.example.trackme.utils.Constants.STOP_SERVICE
import com.example.trackme.utils.RecordState
import com.example.trackme.utils.TrackingHelper
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
//import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

typealias segment = MutableList<LatLng>
typealias line = MutableList<segment>


class MapService: LifecycleService() {
    private var isOpening = false
    @Inject
    lateinit var sessionRepository: SessionRepository
    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    @Inject
    lateinit var notificationBuilder: NotificationCompat.Builder
    @Inject
    lateinit var preferences: SharedPreferences

    private lateinit var updateNotificationBuilder: NotificationCompat.Builder
    private var prevLocation: Location? = null

    private val timeInMill = MutableLiveData<Long>()
    private var startTime = 0L
    private var runTime = 0L
    private var diffTime = 0L
    private var lastTimestamp = 0L
    private var isChronometerRun = false

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        inject()
        updateNotificationBuilder = notificationBuilder
        fusedLocationProviderClient = FusedLocationProviderClient(this)
        isRunning.observe(this, {
            if(it){
                fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                )
            }
            else
                fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            updateNotification(it)
        })

    }
    private fun inject(){
        val appComponent = TrackMeApplication.instance.appComponent
        appComponent.serviceComponent()
                .create()
                .inject(this)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        initParam(intent?.getBundleExtra("SESSION"))

        when(intent?.action){
            START_SERVICE ->{
                if(!isOpening){
                    startService()
                    isOpening = true
                    Log.d("TAG", "Start Service")
                }
                else{
                    runChronometer()
                    Log.d("TAG", "Resume Service")
                }

                Log.d("TAG","${path.value?.size}")
            }
            PAUSE_SERVICE->{
                Log.d("TAG", "Pause Service")
                isRunning.postValue(false)
                prevLocation = null
                isChronometerRun = false
            }
            STOP_SERVICE->{

            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun initParam(bundleExtra: Bundle?) {
        val _id = bundleExtra?.getInt("id") ?: -1
        val _distance = bundleExtra?.getFloat("distance") ?: 0f
        val _speed = bundleExtra?.getFloat("speed") ?: 0f
        val _duration = bundleExtra?.getLong("duration") ?: 0L

        lifecycleScope.launch {
            val pathList: line = mutableListOf()
            val pathCount = sessionRepository.positionDao.segmentCount(_id)
            for (i in 0..pathCount){
                val segList = sessionRepository.positionDao.getPositions(_id, i).map {
                    LatLng(it.lat.toDouble(), it.lon.toDouble())
                }.toMutableList()

                pathList.add(segList)
            }
            path.postValue(pathList)
        }

        lifecycleScope.launch {
            val isPause = preferences.getInt(TrackMeApplication.RECORD_STATE, -1) == RecordState.PAUSED.ordinal
            isRunning.postValue(!isPause)
        }

        idSession = _id
        timeInSec.postValue(_duration)
        timeInMill.postValue(_duration * 1000)
        distance.postValue(_distance)
        speed.postValue(_speed)

    }

    private fun addPoint(location: Location?){
        location?.let {
            val currentPoint = LatLng(it.latitude,it.longitude)

            path.value?.apply {
                last().add(currentPoint)
                path.postValue(this)

            }

            lifecycleScope.launch {
                sessionRepository.insertPosition(
                    Position(0, it.latitude.toFloat(), it.longitude.toFloat(), path.value!!.size - 1, idSession)
                )
            }
        }
    }
    val locationCallback = object : LocationCallback(){
        override fun onLocationResult(p0: LocationResult) {
            if(isRunning.value!!){
                for(i in p0.locations){
                    addPoint(i)
                    distance.value = distance.value?.plus(calculateDistance(i))
                    prevLocation = i
//                    Log.d("TAG", "${distance.value}")
                }
            }

        }
    }
    fun calculateDistance(location: Location): Float{

        if(prevLocation == null) {
            prevLocation = location
            return 0f
        }

        return location.distanceTo(prevLocation)

    }
    private val locationRequest = LocationRequest.create()?.apply{
        interval = 5000
        fastestInterval = 2000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createChannel(notificationManager: NotificationManager){
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL,
        IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)
    }

    fun addNewSegment() = path.value?.apply {
        add(mutableListOf())
        path.postValue(this)
    }?: path.postValue(mutableListOf(mutableListOf()))

    private fun runChronometer(){
        if(!isChronometerRun)
            addNewSegment()
        isRunning.postValue(true)
        startTime = System.currentTimeMillis()
        isChronometerRun = true
        CoroutineScope(Dispatchers.Main).launch {
            while(isRunning.value!!){
                diffTime = System.currentTimeMillis() - startTime
                timeInMill.postValue(diffTime+runTime)
                if(timeInMill.value!! >= lastTimestamp + 1000L){
                    timeInSec.postValue(timeInSec.value!!+1)
                    lastTimestamp += 1000L
                    Log.d("RECording", "${timeInSec.value!!}")

                }
                //frequency of updated time
                delay(200)
            }
            runTime += diffTime
        }
    }
    private fun updateNotification(running: Boolean){
        val textButton = if(running) "Pause" else "Run"
        var pending: PendingIntent? = null
        if(running)
        {
            val i = Intent(this, MapService::class.java)
            i.action = PAUSE_SERVICE
            pending = PendingIntent.getService(this, 1, i,
            FLAG_UPDATE_CURRENT)
        }
        else{
            val i = Intent(this, MapService::class.java)
            i.action = START_SERVICE
            pending = PendingIntent.getService(this, 2, i,
                FLAG_UPDATE_CURRENT)
        }
        val notification = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        updateNotificationBuilder = notificationBuilder
            .addAction(R.drawable.ic_pause_24,textButton,pending)
        notification.notify(NOTIFICATION_ID,updateNotificationBuilder.build())
    }
    fun startService(){
        runChronometer()
        val notification = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createChannel(notification)
        }
//        val intent = Intent(this, RecordingActivity::class.java)
//                .apply {
//                    action = ACTION_FOREGROUND
//                }
////        val pending = TaskStackBuilder.create(this).run{
////            addNextIntentWithParentStack(intent)
////            getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT)
////        }
//        val pending = PendingIntent.getActivity(this,
//                0, intent, FLAG_UPDATE_CURRENT
//        )

        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        timeInSec.observe(this,{
            val noti = updateNotificationBuilder
                .setContentText(TrackingHelper.formatChronometer(it))
            notification.notify(NOTIFICATION_ID,noti.build())
        })
    }



    companion object{
        var idSession: Int = 0
        val isRunning = MutableLiveData<Boolean>()
        val path = MutableLiveData<line>()
        val distance = MutableLiveData<Float>()
        val speed = MutableLiveData<Float>()
        val timeInSec = MutableLiveData<Long>()
    }

}