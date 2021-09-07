package com.example.trackme.utils

import android.os.Build
import android.util.Log
import pub.devrel.easypermissions.EasyPermissions

enum class RecordState {
    RECORDING,
    PAUSED,
    NONE //saved state
}

object Constants{
    const val START_SERVICE = "START_SERVICE"
    const val STOP_SERVICE = "STOP_SERVICE"
    const val PAUSE_SERVICE = "PAUSE_SERVICE"
    const val RESUME_SERVICE = "RESUME_SERVICE"
    const val PERMISSION_REQUEST_CODE = 1

    const val NOTIFICATION_CHANNEL = "TrackLocation"
    const val NOTIFICATION_ID = 1
    const val NOTIFICATION_CHANNEL_ID = "1"
    const val ACTION_FOREGROUND = "ACTION FOREGROUND"


}
