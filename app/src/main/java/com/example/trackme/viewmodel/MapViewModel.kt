package com.example.trackme.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import android.widget.Chronometer
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.trackme.repo.entity.Session
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng

class MapViewModel: ViewModel() {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    val session: MutableLiveData<Session> = MutableLiveData()

    val listLocation =  mutableListOf<LatLng>()
    val lastLocation = MutableLiveData<Location?>()
    var prevLocation: Location? = null
    val distance = MutableLiveData<Float>()
    val speed = MutableLiveData<Float>()
    var avgSpeed : Float = 0f
    private var isStart = false


    val locationRequest = LocationRequest.create()?.apply{
        interval = 5000
        fastestInterval = 2000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }
    val locationCallback = object : LocationCallback(){
        override fun onLocationResult(p0: LocationResult) {
            for(i in p0.locations){

                val location = LatLng(i.latitude,i.longitude)

                if(i != lastLocation.value){
                    if(lastLocation.value != null)
                        distance.value = distance.value?.plus(i.distanceTo(lastLocation.value))
                    Log.d("DiStance","${distance.value}")
                    prevLocation = lastLocation.value
                    listLocation.add(location)
                    lastLocation.value = i
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun init(context: Context){
        speed.value = 0f
        distance.value = 0f
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,
        locationCallback,
        null)

        lastLocation.value = null
    }

    override fun onCleared() {
        super.onCleared()
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

}