package com.example.trackme.view

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import com.example.trackme.R
import com.example.trackme.databinding.ActivityRecordingBinding
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

class RecordingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_recording)

        val binding: ActivityRecordingBinding =
                DataBindingUtil.setContentView(this, R.layout.activity_recording)


    }


}