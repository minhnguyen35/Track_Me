package com.example.trackme.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
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
import com.example.trackme.utils.Constants.ACTION_FOREGROUND
import com.example.trackme.utils.Constants.NOTIFICATION_CHANNEL
import com.example.trackme.utils.Constants.NOTIFICATION_CHANNEL_ID
import com.example.trackme.utils.Constants.NOTIFICATION_ID
import com.example.trackme.utils.Constants.PAUSE_SERVICE
import com.example.trackme.utils.Constants.START_SERVICE
import com.example.trackme.utils.Constants.STOP_SERVICE
import com.example.trackme.view.activity.RecordingActivity
import com.example.trackme.view.activity.SessionActivity
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng

typealias segment = MutableList<LatLng>
typealias line = MutableList<segment>

class MapService: LifecycleService() {
    private var isOpening = false
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        initParam()
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
        })
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action){
            START_SERVICE ->{
                if(!isOpening){
                    startService()
                    isOpening = true
                }
                else{
                    startService()
                }
                Log.d("TAG", "Start Service")
            }
            PAUSE_SERVICE->{
                Log.d("TAG", "Pause Service")
                isRunning.postValue(false)
            }
            STOP_SERVICE->{

            }
        }
        return super.onStartCommand(intent, flags, startId)
    }
    fun initParam(){
        isRunning.postValue(false)
        path.postValue(mutableListOf())
    }

    private fun addPoint(location: Location?){
        location?.let {
            val currentPoint = LatLng(it.latitude,it.longitude)
            Log.d("TAG","${path.value?.size}")
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
                }
            }
        }
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

    fun startService(){
        path.value?.apply {
            add(mutableListOf())
            path.postValue(this)
        }

        isRunning.postValue(true)
        val notification = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createChannel(notification)
        }
        val intent = Intent(this, RecordingActivity::class.java)
                .apply {
                    action = ACTION_FOREGROUND
                }
        val pending = TaskStackBuilder.create(this).run{
            addNextIntentWithParentStack(intent)
            getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(pending)
                .setSmallIcon(R.drawable.ic_run_24)
                .setContentText("Distance")
                .setContentTitle("You Are Running")
                .build()
        startForeground(NOTIFICATION_ID, notificationBuilder)

    }



    companion object{
        val isRunning = MutableLiveData<Boolean>()
        val path = MutableLiveData<line>()
    }

}