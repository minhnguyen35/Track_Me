package com.example.trackme.utils

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
}