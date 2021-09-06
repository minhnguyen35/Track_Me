package com.example.trackme.view.activity

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import com.example.trackme.R
import com.example.trackme.TrackMeApplication
import com.example.trackme.databinding.ActivityRecordingBinding
import com.example.trackme.databinding.DialogConfirmQuitBinding
import com.example.trackme.repo.entity.Session
import com.example.trackme.utils.Constants.PERMISSION_REQUEST_CODE
import com.example.trackme.utils.RecordState
import com.example.trackme.utils.TrackingHelper
import com.example.trackme.view.fragment.MapsFragment
import com.example.trackme.viewmodel.MapService
import com.example.trackme.viewmodel.RecordingViewModel
import com.example.trackme.viewmodel.SessionViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import javax.inject.Inject

class RecordingActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    private val TAG = "RECORD"
    private lateinit var binding: ActivityRecordingBinding
    private lateinit var recordState: RecordState
    private var isGPSEnable = false
    private var chronometer: Long = 0L
    private var locationDialog: Dialog? = null

    @Inject
    lateinit var sessionViewModel: SessionViewModel

    @Inject
    lateinit var recordingViewModel: RecordingViewModel


    var isRunning = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestPermission()
        inject()
        binding.activity = this
        binding.session = Session.newInstance()

        observeVar()
    }

    private fun observeVar() {
        //listen for session object
        recordingViewModel.session.observe(this) {
            binding.session = it
            if (it != null) {
                Log.d(
                    TAG,
                    "observe session: ${it.id} - ${it.distance} - ${it.speedAvg} - ${it.duration}"
                )
            }
        }

        //listen for service state
        recordingViewModel.isRecording.observe(this) {
            changeButton(it)
        }

        MapService.isGPSAvailable.observe(this, {
            Log.d("RECORDING", "gps: $it")
            isGPSEnable = it
            if (!isGPSEnable) {
                showLocationDialog()
//                binding.pause.isClickable = false
            } else {
                if (locationDialog?.isShowing == true)
                    locationDialog?.dismiss()
//                binding.pause.isClickable = true
            }

        })

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

//    fun onPauseBtnClick(){
//        if(isRunning){
//            TrackingHelper.triggerService(this,PAUSE_SERVICE)
//            //upload current data
//        }
//        else
//            TrackingHelper.triggerService(this,START_SERVICE)
//    }

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
        val dialog = Dialog(this)
        val binding = DialogConfirmQuitBinding.inflate(dialog.layoutInflater)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(binding.root)

        binding.txtCancel.setOnClickListener {
            dialog.dismiss()
            recordingViewModel.requestStopRecord(false)
            finish()
        }

        binding.txtSave.setOnClickListener {
            dialog.setCancelable(false)
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

        dialog.setOnCancelListener {
            recordingViewModel.requestPauseResumeRecord()
        }

        dialog.show()
    }

    override fun onStop() {
        super.onStop()
        locationDialog?.let {
            it.dismiss()
            locationDialog = null
        }
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
            if(recordingViewModel.isRecording.value!!){
                recordingViewModel.requestPauseResumeRecord()
            }
            onStopBtnClick()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("AAA", "onDestroy: ")
    }

}