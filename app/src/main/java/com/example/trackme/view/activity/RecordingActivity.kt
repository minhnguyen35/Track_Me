package com.example.trackme.view.activity

import android.app.Dialog
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.example.trackme.R
import com.example.trackme.TrackMeApplication
import com.example.trackme.databinding.ActivityRecordingBinding
import com.example.trackme.databinding.DialogConfirmQuitBinding
import com.example.trackme.repo.entity.Session
import com.example.trackme.utils.Constants.PAUSE_SERVICE
import com.example.trackme.utils.Constants.PERMISSION_REQUEST_CODE
import com.example.trackme.utils.Constants.START_SERVICE
import com.example.trackme.utils.Constants.STOP_SERVICE
import com.example.trackme.utils.RecordState
import com.example.trackme.utils.TrackingHelper
import com.example.trackme.view.fragment.MapsFragment
import com.example.trackme.viewmodel.MapService
import com.example.trackme.viewmodel.RecordingViewModel
import com.example.trackme.viewmodel.SessionViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class RecordingActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks{
    private lateinit var binding: ActivityRecordingBinding
    private lateinit var recordState: RecordState
    private var isGPSEnable = false
    private var chronometer: Long = 0L
    private var locationDialog: Dialog? = null
    private var confirmDialog: Dialog? = null
    @Inject
    lateinit var sessionViewModel: SessionViewModel

    @Inject
    lateinit var recordingViewModel: RecordingViewModel

    @Inject
    lateinit var preferences: SharedPreferences


    var isRunning = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestPermission()
        inject()
        binding.activity = this
        binding.session = Session.newInstance()

        preferences = getSharedPreferences(TrackMeApplication.SHARED_NAME, MODE_PRIVATE)
        observeVar()
    }

    private fun observeVar() {
        //create new session if not exist
        MapService.session.observe(this) {
            binding.session = it
        }

        MapService.isRunning.observe(this, {
            changeButton(it)
        })
        MapService.distance.observe(this, {
            binding.currentDistance.text = "%.2f km".format(it / 1000)
        })
        MapService.timeInSec.observe(this,{
            chronometer = it
            binding.chronometer.text = TrackingHelper.formatChronometer(chronometer)
        })
        MapService.speed.observe(this,{
            binding.currentSpeed.text = "%.2f km/h".format(it*3.6)
        })
        MapService.isGPSAvailable.observe(this, {
            Log.d("RECORDING", "gps: $it")
            isGPSEnable = it
            if(!isGPSEnable)
            {
                showLocationDialog()
//                binding.pause.isClickable = false
            }
            else {
                if(locationDialog?.isShowing == true)
                    locationDialog?.dismiss()
//                binding.pause.isClickable = true
            }

        })

    }

    private fun showLocationDialog() {
        if(locationDialog?.isShowing == true)
            return
        locationDialog = MaterialAlertDialogBuilder(this)
                .setTitle("GPS Available")
                .setMessage("Please Turn On GPS To Use This Feature")
                .show()

        locationDialog!!.show()
    }

    fun onPauseBtnClick(){
        if(isRunning){
            TrackingHelper.triggerService(this,PAUSE_SERVICE)
            //upload current data
        }
        else
            TrackingHelper.triggerService(this,START_SERVICE)
    }
    fun changeButton(running: Boolean){
        isRunning = running
        if(isRunning){
            binding.stop.visibility = View.GONE
            binding.pause.setImageResource(R.drawable.ic_pause_24)

        }
        else {
            binding.pause.setImageResource(R.drawable.ic_replay_24)
            binding.stop.visibility = View.VISIBLE
        }
    }


    fun inject() {
        val appComponent = TrackMeApplication.instance.appComponent
        appComponent.mapComponent()
            .create(this)
            .inject(this)
    }


    fun onStopBtnClick() {
        recordingViewModel.changeRecordState(RecordState.PAUSED)
        showConfirmDialog()
    }

    private fun showConfirmDialog() {
        confirmDialog = Dialog(this)
        val binding = DialogConfirmQuitBinding.inflate(confirmDialog!!.layoutInflater)
        confirmDialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        confirmDialog!!.setCancelable(true)
        confirmDialog!!.setContentView(binding.root)

        binding.txtCancel.setOnClickListener {
            confirmDialog?.dismiss()
            quitRecord(RESULT_CANCELED)

        }

        binding.txtSave.setOnClickListener {
            confirmDialog?.setCancelable(false)
            binding.textView.text = resources.getString(R.string.saving_label)
            binding.txtCancel.apply {
                isEnabled = false
                visibility = View.GONE
            }
            binding.txtSave.apply {
                isEnabled = false
                visibility = View.GONE
            }
            binding.progressBar.visibility = View.VISIBLE

            saveAndQuit()
        }

        confirmDialog?.setOnCancelListener {
            recordingViewModel.changeRecordState(RecordState.RECORDING)
        }

        confirmDialog!!.show()
    }

    private fun saveAndQuit() {
        val map = (binding.map.tag as MapsFragment).map!!
        lifecycleScope.launch {
            val bound: LatLngBounds = sessionViewModel.getLatLonBound(binding.session!!.id)

            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bound, 50))
            map.snapshot {
                val byteOs = ByteArrayOutputStream()
                if (it != null) {
                    it.compress(Bitmap.CompressFormat.JPEG, 50, byteOs)
                    binding.session!!.mapImg = byteOs.toByteArray()

                    Log.d("AAA", "prepare update session: ${binding.session}")
                    sessionViewModel.updateSession(binding.session!!)
                    quitRecord(RESULT_OK)
                }
            }
        }

    }

    private fun quitRecord(result: Int) {
        recordingViewModel.changeRecordState(RecordState.NONE)
        sessionViewModel.viewModelScope.launch {
            //sessionViewModel.updateSession(binding.session!!)
            TrackingHelper.triggerService(this@RecordingActivity, STOP_SERVICE)
            setResult(result)
            clearDb(result)
            finish()
        }
    }

    override fun onStop() {
        super.onStop()
        locationDialog?.let {
            it.dismiss()
            locationDialog = null
        }
        confirmDialog?.let {
            it.dismiss()
            confirmDialog = null
        }
    }
    private fun saveState(state: RecordState) {
        recordState = state
        preferences.edit()
            .putInt(TrackMeApplication.RECORD_STATE, recordState.ordinal)
            .apply()
    }
    private fun clearDb(result: Int) {
        sessionViewModel.clearData(result, binding.session!!)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
//        triggerService(START_SERVICE)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else
            requestPermission()
    }

    fun requestPermission() {
        if (TrackingHelper.checkPermission(this)) {
            return
        }
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            EasyPermissions.requestPermissions(
                    this,
                    "This Feature Need To Access Location For Tracking Route",
                    PERMISSION_REQUEST_CODE,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        else{
            EasyPermissions.requestPermissions(
                    this,
                    "This Feature Need To Access Location For Tracking Route",
                    PERMISSION_REQUEST_CODE,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode,
        permissions, grantResults, this)
    }


    override fun onBackPressed() {
        onStopBtnClick()
        if (recordingViewModel.recordState.value == RecordState.NONE)
            super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("AAA", "onDestroy: ")
    }

}