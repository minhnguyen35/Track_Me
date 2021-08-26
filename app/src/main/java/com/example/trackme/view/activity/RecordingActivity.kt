package com.example.trackme.view.activity

import android.app.Dialog
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import com.example.trackme.R
import com.example.trackme.TrackMeApplication
import com.example.trackme.databinding.ActivityRecordingBinding
import com.example.trackme.databinding.DialogConfirmQuitBinding
import com.example.trackme.repo.entity.Session
import com.example.trackme.utils.RecordState
import com.example.trackme.viewmodel.MapViewModel
import com.example.trackme.viewmodel.SessionViewModel
import kotlinx.coroutines.Job
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import javax.inject.Inject
import kotlin.math.round

class RecordingActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    private lateinit var binding: ActivityRecordingBinding
    private lateinit var recordState: RecordState
    private lateinit var preferences: SharedPreferences

    @Inject
    lateinit var viewModel: MapViewModel

    @Inject
    lateinit var sessionViewModel: SessionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        inject()


        binding.activity = this
        binding.session = viewModel.session.value

        preferences = getSharedPreferences(TrackMeApplication.SHARED_NAME, MODE_PRIVATE)

        enableLocation()
        viewModel.session.observe(this){
            binding.session = it
        }


    }

    fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.hasPermissions(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            EasyPermissions.hasPermissions(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }

    fun inject() {
        val appComponent = TrackMeApplication.instance.appComponent
        appComponent.mapComponent()
            .create(this)
            .inject(this)
    }

    fun enableLocation() {
        if (checkPermission()) {
//            map?.isMyLocationEnabled = true
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(
                this,
                "This Feature Need To Access Location For Tracking Route",
                REQUEST_CODE,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            EasyPermissions.requestPermissions(
                this,
                "This Feature Need To Access Location For Tracking Route",
                REQUEST_CODE,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }


    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            enableLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(
            requestCode, permissions,
            grantResults, this
        )

    }

    fun onStopBtnClick() {
        saveState(RecordState.PAUSED)
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
            quitRecord(RESULT_CANCELED)
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
            saveRecord()
        }

        dialog.setOnCancelListener {
            saveState(RecordState.RECORDING)
        }

        dialog.show()
    }

    private fun saveRecord() {
        sessionViewModel.insertSession(
            Session(0, 0f, 0f, 0, null),
        ) { id ->
            sessionViewModel.deletePositions(id.toInt())
        }
        quitRecord(RESULT_OK)
    }

    private fun quitRecord(result: Int) {
        saveState(RecordState.STOPPED)
        setResult(result)
        finish()
    }

    private fun saveState(state: RecordState) {
        recordState = state
        preferences.edit()
            .putInt(TrackMeApplication.RECORD_STATE, recordState.ordinal)
            .apply()
    }

    companion object {
        private val REQUEST_CODE = 1
    }
}