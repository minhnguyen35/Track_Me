package com.example.trackme.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MapViewModel: ViewModel() {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient


    fun init(context: Context){
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    }
}