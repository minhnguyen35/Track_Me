package com.example.trackme.view

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.example.trackme.R
import com.example.trackme.databinding.ActivityRecordingBinding
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

class RecordingActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    private lateinit var binding: ActivityRecordingBinding
    private lateinit var recordState: RecordState
    private lateinit var preferences: SharedPreferences
    @Inject
    lateinit var viewModel: MapViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.activity = this
        preferences = getSharedPreferences(TrackMeApplication.SHARED_NAME, MODE_PRIVATE)

        enableLocation()


    }

    fun checkPermission(): Boolean{
        return if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            EasyPermissions.hasPermissions(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        else{
            EasyPermissions.hasPermissions(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }
        val binding: ActivityRecordingBinding = DataBindingUtil.setContentView(
            this,R.layout.activity_recording
        )
        inject()
        Log.d("Recording", "${viewModel.hashCode()}")

        viewModel.distance.observe(this,{
            distance ->
            binding.currentDistance.text = "${round(distance)} m"



    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
    }

    fun inject(){
        val appComponent = TrackMeApplication.instance.appComponent
        appComponent.mapComponent()
            .create(this)
            .inject(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,
                grantResults,this)

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

    companion object{
        private val REQUEST_CODE = 1
    }
}