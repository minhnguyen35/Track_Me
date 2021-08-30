package com.example.trackme.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.trackme.R
import com.example.trackme.TrackMeApplication
import com.example.trackme.utils.Constants.ACTION_FOREGROUND
import com.example.trackme.utils.Constants.NOTIFICATION_CHANNEL
import com.example.trackme.utils.Constants.NOTIFICATION_CHANNEL_ID
import com.example.trackme.utils.Constants.NOTIFICATION_ID
import com.example.trackme.utils.Constants.PAUSE_SERVICE
import com.example.trackme.utils.Constants.START_SERVICE
import com.example.trackme.utils.Constants.STOP_SERVICE
import com.example.trackme.utils.TrackingHelper
import com.example.trackme.view.activity.RecordingActivity
import com.example.trackme.view.activity.SessionActivity
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
//import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

typealias segment = MutableList<LatLng>
typealias line = MutableList<segment>


class MapService: LifecycleService() {
    private var isOpening = false
    private var isCancelled = false
    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    @Inject
    lateinit var notificationBuilder: NotificationCompat.Builder

    private lateinit var updateNotificationBuilder: NotificationCompat.Builder
    private var prevLocation: Location? = null
    private var speedList = mutableListOf<Float>()
    //chronometer
    private val timeInMill = MutableLiveData<Long>()
    private var startTime = 0L
    private var runTime = 0L
    private var diffTime = 0L
    private var lastTimestamp = 0L
    private var isChronometerRun = false

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        Log.d("MAPSERVICE", "onCreate")
        inject()
        initParam()
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
    private fun cancellService(){
        isCancelled = true
        isOpening = false
        initParam()
        onServicePause()
        stopForeground(true)
        stopSelf()
    }
    private fun inject(){
        val appComponent = TrackMeApplication.instance.appComponent
        appComponent.serviceComponent()
                .create()
                .inject(this)
    }
    private fun onServicePause(){
        isRunning.postValue(false)
        prevLocation = null
        isChronometerRun = false
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MAPSERVICE", "on StartCommand")
        when(intent?.action){
            START_SERVICE ->{
                if(!isOpening){
                    startService()
                    isOpening = true
                    Log.d("MAPSERVICE", "Start Service")
                }
                else{
                    runChronometer()
                    Log.d("MAPSERVICE", "Resume Service")
                }

                Log.d("MAPSERVICE","${path.value?.size}")
            }
            PAUSE_SERVICE->{
                Log.d("MAPSERVICE", "Pause Service")
                onServicePause()
            }
            STOP_SERVICE->{
                Log.d("MAPSERVICE", "Stop Service")
                cancellService()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }
    fun initParam(){
        timeInSec.postValue(0L)
        timeInMill.postValue(0L)
        isRunning.postValue(false)
        path.postValue(mutableListOf())
        distance.postValue(0f)
        speed.postValue(0f)

    }

    private fun addPoint(location: Location?){
        location?.let {
            val currentPoint = LatLng(it.latitude,it.longitude)

            path.value?.apply {
                last().add(currentPoint)
                path.postValue(this)

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
                    speed.postValue(distance.value!!/ timeInSec.value!!)
                    speedList.add(speed.value!!)
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
        if(!isCancelled) {
            updateNotificationBuilder = notificationBuilder
                    .addAction(R.drawable.ic_pause_24, textButton, pending)
            notification.notify(NOTIFICATION_ID, updateNotificationBuilder.build())
        }
    }
    fun startService(){
        runChronometer()
        val notification = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createChannel(notification)
        }

        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        timeInSec.observe(this,{
            if(!isCancelled) {
                val noti = updateNotificationBuilder
                        .setContentText(TrackingHelper.formatChronometer(it))
                notification.notify(NOTIFICATION_ID, noti.build())
            }
        })
    }



    companion object{
        val isRunning = MutableLiveData<Boolean>()
        val path = MutableLiveData<line>()
        val distance = MutableLiveData<Float>()
        val speed = MutableLiveData<Float>()
        val timeInSec = MutableLiveData<Long>()
    }

}