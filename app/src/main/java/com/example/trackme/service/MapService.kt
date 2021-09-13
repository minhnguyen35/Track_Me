package com.example.trackme.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.example.trackme.TrackMeApplication
import com.example.trackme.repo.SessionRepository
import com.example.trackme.repo.entity.Position
import com.example.trackme.utils.Constants.NOTIFICATION_CHANNEL
import com.example.trackme.utils.Constants.NOTIFICATION_CHANNEL_ID
import com.example.trackme.utils.Constants.NOTIFICATION_ID
import com.example.trackme.utils.Constants.PAUSE_SERVICE
import com.example.trackme.utils.Constants.RESUME_SERVICE
import com.example.trackme.utils.Constants.INCREASE_SEGMENT
import com.example.trackme.utils.Constants.START_SERVICE
import com.example.trackme.utils.Constants.STOP_SERVICE
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


class MapService : LifecycleService() {
    var isCancelled = MutableLiveData<Boolean?>(false)
    val isGPSAvailable = MutableLiveData<Boolean>(false)
    private val isRunning = MutableLiveData<Boolean>(false)
    var segmentId = -1
    private var sessionId = -1

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    @Inject
    lateinit var notificationBuilder: NotificationCompat.Builder



    private lateinit var updateNotificationBuilder: NotificationCompat.Builder

    val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): MapService = this@MapService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
//        Log.d("Mapservice", "binding service")
        return binder
    }

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
//        Log.d("MAPSERVICE", "onCreate")
        inject()

        updateNotificationBuilder = notificationBuilder
        fusedLocationProviderClient = FusedLocationProviderClient(this)
        isRunning.observe(this, {
            if(it){
                if(sessionId != -1) {
                    fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                            locationCallback,
                            Looper.getMainLooper()
                    )
                }
            }
            else
                fusedLocationProviderClient.removeLocationUpdates(locationCallback)
//            updateNotification(it)
        })

    }

    private fun cancellService(){
        isCancelled.postValue(true)

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
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MAPSERVICE", "on StartCommand")
        sessionId = intent?.getIntExtra(ID_SESSION, -1) ?: -1
        when (intent?.action) {
            START_SERVICE -> {
                startService()
               Log.d("MAPSERVICE", "Start Service")
            }
            RESUME_SERVICE -> {
                addSegment()
               Log.d("MAPSERVICE", "Pause Service")
            }
            PAUSE_SERVICE->{
                Log.d("MAPSERVICE", "Pause Service")
                onServicePause()
            }
            STOP_SERVICE->{
                Log.d("MAPSERVICE", "Stop Service")
                cancellService()
            }
            INCREASE_SEGMENT -> {
                segmentId++
            }
            else ->{
                cancellService()
            }

        }
        return super.onStartCommand(intent, flags, startId)

    }


    private fun addPoint(location: Location?) {
        location?.let {

            lifecycleScope.launch(Dispatchers.IO) {

                if(sessionId != -1) {
                    sessionRepository.insertPosition(
                        Position(
                            0,
                            it.latitude,
                            it.longitude,
                            segmentId,
                            sessionId
                        )
                    )
                }
            }

        }
    }

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            if(isRunning.value!!){
                for(i in p0.locations){
                    addPoint(i)

                }
            }

        }

        override fun onLocationAvailability(p0: LocationAvailability) {
            super.onLocationAvailability(p0)

            if(p0.isLocationAvailable != isGPSAvailable.value!!){
                isGPSAvailable.postValue(p0.isLocationAvailable)
                if(!p0.isLocationAvailable)
                    segmentId++
            }

        }
    }


    private val locationRequest = LocationRequest.create()?.apply{
        interval = 5000
        fastestInterval = 2000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }



    private fun addSegment(){
//        Log.d("MAPSERVICE", "run chronometer")
        if(isRunning.value == null || isRunning.value == false) {
            segmentId++
//            Log.d("MAPSERVICE", "add new segment")
        }
        isRunning.postValue(true)

    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createChannel(notificationManager: NotificationManager){
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL,
                NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)
    }
    fun startService(){
        addSegment()
        val notification = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createChannel(notification)
        }
//
        startForeground(NOTIFICATION_ID, notificationBuilder.build())


    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopForeground(true)
        stopSelf()
        return super.onUnbind(intent)
    }


    companion object{
        const val ID_SESSION = "ID_SESSION"
    }

}