package com.example.trackme.utils

import android.os.Build
import android.util.Log
import pub.devrel.easypermissions.EasyPermissions

enum class RecordState { RECORDING, PAUSED, STOPPED }

object Constants{
    const val START_SERVICE = "START_SERVICE"
    const val STOP_SERVICE = "STOP_SERVICE"
    const val PAUSE_SERVICE = "PAUSE_SERVICE"
    const val PERMISSION_REQUEST_CODE = 1

    const val NOTIFICATION_CHANNEL = "TrackLocation"
    const val NOTIFICATION_ID = 1
    const val NOTIFICATION_CHANNEL_ID = "1"
    const val ACTION_FOREGROUND = "ACTION FOREGROUND"

//    fun checkPermission(): Boolean{
//        var res = false
//        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
//            res = EasyPermissions.hasPermissions(
//                    this,
//                    android.Manifest.permission.ACCESS_FINE_LOCATION,
//                    android.Manifest.permission.ACCESS_COARSE_LOCATION
//            )
//            Log.d("TAG", "result SDK < Q and $res")
//        }
//        else{
//            res = EasyPermissions.hasPermissions(
//                    this,
//                    android.Manifest.permission.ACCESS_FINE_LOCATION,
//                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
//                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
//            )
//            Log.d("TAG", "result SDK >= Q and $res")
//
//        }
//        return res
//    }
}
