package com.example.trackme.view.activity

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.example.trackme.R
import com.example.trackme.TrackMeApplication
import com.example.trackme.databinding.ActivityRecordingBinding
import com.example.trackme.databinding.DialogConfirmQuitBinding
import com.example.trackme.repo.entity.Session
import com.example.trackme.utils.Constants.PERMISSION_REQUEST_CODE
import com.example.trackme.utils.TrackingHelper
import com.example.trackme.view.fragment.MapsFragment
import com.example.trackme.viewmodel.RecordingViewModel
import com.example.trackme.viewmodel.SessionViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import javax.inject.Inject

class RecordingActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    private val TAG = "RECORD"
    private lateinit var binding: ActivityRecordingBinding
    var isBound = MutableLiveData(false)

    private var chronometer: Long = 0L
    private var locationDialog: Dialog? = null
    private var confirmDialog: Dialog? = null

    @Inject
    lateinit var sessionViewModel: SessionViewModel

    @Inject
    lateinit var recordingViewModel: RecordingViewModel


    var isRunning = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        inject()

        recordingViewModel.registerBroadcast(this)


        Log.d("MAPSFRAGMENT", "viewmodel ${recordingViewModel.hashCode()}")

        binding.activity = this
        binding.session = Session.newInstance()
        observeVar()
    }


    override fun onStart() {
        super.onStart()
        requestPermission()

    }

    private fun observeVar() {
        //listen for session object
        recordingViewModel.session.observe(this) {
            binding.session = it
        }

        recordingViewModel.isRecording.observe(this) {
            changeButton(it)
        }

        recordingViewModel.distance.observe(this, {
            binding.currentDistance.text = "%.2f km".format(it / 1000)
        })

        recordingViewModel.timeInSec.observe(this, {
            chronometer = it
            binding.chronometer.text = TrackingHelper.formatChronometer(chronometer)
        })

        recordingViewModel.speed.observe(this, {
            binding.currentSpeed.text = "%.2f km/h".format(it * 3.6)
        })

        recordingViewModel.isGpsEnable.observe(this) {
            if (it)
                locationDialog?.dismiss()
            else
                showLocationDialog()
        }
    }


    private fun showLocationDialog() {
        if (locationDialog?.isShowing == true)
            return
        locationDialog = MaterialAlertDialogBuilder(this)
            .setTitle("GPS Available")
            .setMessage("Please Turn On GPS To Use This Feature")
            .show()

        locationDialog!!.show()
    }

    fun onPauseBtnClick() {
        recordingViewModel.requestPauseResumeRecord()
    }

    fun changeButton(running: Boolean) {
        isRunning = running
        if (isRunning) {
            binding.stop.visibility = View.GONE
            binding.pause.setImageResource(R.drawable.ic_pause_24)

        } else {
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
        showConfirmDialog()
    }

    private fun showConfirmDialog() {
        confirmDialog = Dialog(this)
        val binding = DialogConfirmQuitBinding.inflate(confirmDialog!!.layoutInflater)
        confirmDialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        confirmDialog!!.setCancelable(true)
        confirmDialog!!.setContentView(binding.root)

        binding.txtCancel.setOnClickListener {
            confirmDialog!!.dismiss()
            recordingViewModel.requestStopRecord(false)
            finish()
        }

        binding.txtSave.setOnClickListener {
            confirmDialog!!.setCancelable(false)
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

            val map = (this.binding.map.tag as MapsFragment).map!!
            recordingViewModel.requestStopRecord(true, map)
            finish()
        }

        confirmDialog!!.show()
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

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
//        recordingViewModel.requestStartRecord()
//        if(!isGPSEnable)
//            showLocationDialog()
        if (recordingViewModel.isStart == false) {
            //bindService()
            recordingViewModel.isStart = true
            recordingViewModel.requestStartRecord()
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else
            requestPermission()
    }

    fun requestPermission() {
        if (TrackingHelper.checkPermission(this)) {

            if (recordingViewModel.isStart == false) {
                recordingViewModel.isStart = true
                recordingViewModel.requestStartRecord()
            }
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(
                this,
                "This Feature Need To Access Location For Tracking Route",
                PERMISSION_REQUEST_CODE,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
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
        EasyPermissions.onRequestPermissionsResult(
            requestCode,
            permissions, grantResults, this
        )
    }


    override fun onBackPressed() {
        if (recordingViewModel.isRecording.value != null) {
            if (recordingViewModel.isRecording.value!!) {
                recordingViewModel.requestPauseResumeRecord()
            }
            onStopBtnClick()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        unbindService(serviceConnection)
        recordingViewModel.unregisterReceiver(this)
        isBound.value = false

    }

}