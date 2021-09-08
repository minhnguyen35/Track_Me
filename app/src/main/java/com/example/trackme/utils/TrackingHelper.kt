package com.example.trackme.utils

import android.content.Context
import android.os.Build
import pub.devrel.easypermissions.EasyPermissions
import java.util.concurrent.TimeUnit

object TrackingHelper {
    fun formatChronometer(time: Long): String{
        var runningTime = time
        val hours = TimeUnit.SECONDS.toHours(runningTime)
        runningTime -= TimeUnit.HOURS.toSeconds(hours)
        val minutes = TimeUnit.SECONDS.toMinutes(runningTime)
        runningTime -= TimeUnit.MINUTES.toSeconds(minutes)

        var hourText: String = "$hours"
        var minuteText: String = "$minutes"
        var secondText: String = "$runningTime"
        if(hours < 10)
            hourText = "0$hours"
        if(minutes <10)
            minuteText = "0$minutes"
        if(runningTime<10)
            secondText = "0$runningTime"
        return "$hourText : $minuteText : $secondText"
    }
    fun checkPermission(context: Context): Boolean{
        var res = false
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            res = EasyPermissions.hasPermissions(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        else{
            res = EasyPermissions.hasPermissions(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
//            Log.d("TAG", "result SDK >= Q and $res")

        }
        return res
    }

}