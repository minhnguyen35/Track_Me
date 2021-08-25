package com.example.trackme.view

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import com.example.trackme.R
import com.example.trackme.TrackMeApplication
import com.example.trackme.databinding.ActivityRecordingBinding
import com.example.trackme.viewmodel.MapViewModel
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import javax.inject.Inject
import kotlin.math.round

class RecordingActivity : AppCompatActivity() {
    @Inject
    lateinit var viewModel: MapViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_recording)

        val binding: ActivityRecordingBinding = DataBindingUtil.setContentView(
            this,R.layout.activity_recording
        )
        inject()
        Log.d("Recording", "${viewModel.hashCode()}")

        viewModel.distance.observe(this,{
            distance ->
            binding.currentDistance.text = "${round(distance)} m"

        })
    }

    fun inject(){
        val appComponent = TrackMeApplication.instance.appComponent
        appComponent.mapComponent()
            .create(this)
            .inject(this)
    }

}