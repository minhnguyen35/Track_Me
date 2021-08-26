package com.example.trackme.view.activity

import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.Window
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.example.trackme.R
import com.example.trackme.TrackMeApplication
import com.example.trackme.databinding.ActivityRecordingBinding
import com.example.trackme.databinding.DialogConfirmQuitBinding
import com.example.trackme.utils.Constants.PAUSE_SERVICE
import com.example.trackme.utils.Constants.PERMISSION_REQUEST_CODE
import com.example.trackme.utils.Constants.START_SERVICE
import com.example.trackme.utils.RecordState
import com.example.trackme.view.fragment.MapsFragment
import com.example.trackme.viewmodel.MapService
import com.example.trackme.viewmodel.MapViewModel
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import javax.inject.Inject
import kotlin.math.round

class RecordingActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks{
    private lateinit var binding: ActivityRecordingBinding
    private lateinit var recordState: RecordState
    private lateinit var preferences: SharedPreferences
//    @Inject
//    lateinit var viewModel: MapViewModel
    var isRunning = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestPermission()

        binding.activity = this
        preferences = getSharedPreferences(TrackMeApplication.SHARED_NAME, MODE_PRIVATE)
        MapService.isRunning.observe(this,{
            changeButton(it)
        })
    }
    fun onPauseBtnClick(){
        if(isRunning){
            triggerService(PAUSE_SERVICE)
        }
        else
            triggerService(START_SERVICE)
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
    fun checkPermission(): Boolean{
        var res = false
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            res = EasyPermissions.hasPermissions(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
            Log.d("TAG", "result SDK < Q and $res")
        }
        else{
            res = EasyPermissions.hasPermissions(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
            Log.d("TAG", "result SDK >= Q and $res")

        }
        return res
    }

    fun inject(){
        val appComponent = TrackMeApplication.instance.appComponent
        appComponent.mapComponent()
            .create(this)
            .inject(this)
    }


    fun onStopBtnClick(){
        saveState(RecordState.PAUSED)
        showConfirmDialog()
    }

    private fun showConfirmDialog(){
        val dialog = Dialog(this)
        val binding = DialogConfirmQuitBinding.inflate(dialog.layoutInflater)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(binding.root)

        binding.btnCancelDialog.setOnClickListener {
            dialog.dismiss()
            quitRecord(RESULT_CANCELED)
        }

        binding.btnSaveDialog.setOnClickListener {
            dialog.setCancelable(false)
            binding.textView.text = resources.getString(R.string.saving_label)
            binding.btnCancelDialog.apply {
                isEnabled = false
                visibility = View.GONE
            }
            binding.btnSaveDialog.apply {
                isEnabled = false
                visibility = View.GONE
            }
            binding.progressBar.visibility = View.VISIBLE
            saveRecord()
        }

        dialog.setOnCancelListener {
            saveState(RecordState.RECORDING)
        }

        dialog.show()
    }

    private fun saveRecord() {
        Thread{
            Thread.sleep(2000)
            Handler(Looper.getMainLooper()).post {
                quitRecord(RESULT_OK)
            }
        }.start()
    }

    private fun quitRecord(result: Int) {
        saveState(RecordState.STOPPED)
        setResult(result)
        finish()
    }

    private fun saveState(state: RecordState){
        recordState = state
        preferences.edit()
            .putInt(TrackMeApplication.RECORD_STATE, recordState.ordinal)
            .apply()
    }
    private fun triggerService(action: String){
        val i = Intent(this, MapService::class.java)
        i.action = action
        startService(i)
    }


    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
//        triggerService(START_SERVICE)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this,perms)){
            AppSettingsDialog.Builder(this).build().show()
        }
        else
            requestPermission()
    }
    fun requestPermission(){
        if(checkPermission()){
            triggerService(START_SERVICE)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode,
        permissions, grantResults, this)
    }



}