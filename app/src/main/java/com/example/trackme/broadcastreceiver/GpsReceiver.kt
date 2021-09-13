package com.example.trackme.broadcastreceiver

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.location.LocationManager
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class GpsReceiver(var isGpsEnable: MutableLiveData<Boolean>) : BroadcastReceiver() {
    private var locationManager: LocationManager? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent?.action?.equals(LocationManager.PROVIDERS_CHANGED_ACTION) == true){
            context?.let {
                locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
                val currentGpsState = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
                if(currentGpsState != isGpsEnable.value)
                {
                    if(currentGpsState == false){
                        Toast.makeText(it,"Please Turn On GPS To Use This Feature",
                                Toast.LENGTH_SHORT).show()
                    }
                    isGpsEnable.postValue(currentGpsState)
//                    Log.d("Receiver", "GPS change " +
//                            "$isGpsEnable")
                }


            }


        }
    }


}