package com.example.trackme.view.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trackme.TrackMeApplication
import com.example.trackme.databinding.ActivitySessionBinding
import com.example.trackme.utils.Constants.START_SERVICE
import com.example.trackme.utils.RecordState
import com.example.trackme.utils.TrackingHelper
import com.example.trackme.view.adapter.SessionPagingAdapter
import com.example.trackme.viewmodel.SessionViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

const val TAG = "SESSION"

class SessionActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySessionBinding
    private lateinit var sessionAdapter: SessionPagingAdapter

    private lateinit var recordingActivityLauncher: ActivityResultLauncher<Any?>
    private val recordingContract = object : ActivityResultContract<Any?, Any>() {
        override fun createIntent(context: Context, input: Any?): Intent {
            return Intent(this@SessionActivity, RecordingActivity::class.java)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Int {
            return resultCode
        }
    }
    private val activityResultCallback = ActivityResultCallback<Any> { }

    @Inject
    lateinit var viewModel: SessionViewModel

    @Inject
    lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initDi()

        binding.handler = this
        recordingActivityLauncher = registerForActivityResult(recordingContract, activityResultCallback)

        initRecycler()
        observeData()
    }

    private fun observeData() {
        //listen for session list changes
        viewModel.sessionList.observe(this) {
            Log.d(TAG, "sessionAdapter.refresh()")
            sessionAdapter.refresh()
        }

        //listen for session paging ready
        viewModel.listPagerData.observe(this) {
            lifecycleScope.launch {
                Log.d(TAG, "sessionAdapter.submitData(it)")
                sessionAdapter.submitData(it)
            }
        }
    }

    override fun onDestroy() {
        recordingActivityLauncher.unregister()
        super.onDestroy()
    }

    private fun initRecycler() {
        sessionAdapter = getAdapter()

        binding.recyclerSession.apply {
            this.adapter = sessionAdapter
            this.layoutManager =
                LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
            this.addItemDecoration(
                DividerItemDecoration(
                    this.context,
                    DividerItemDecoration.VERTICAL
                )
            )
        }

    }

    private fun initDi() {
        val appComponent = TrackMeApplication.instance.appComponent
        appComponent.sessionComponent()
            .create(this)
            .inject(this)
    }

    private fun getAdapter(): SessionPagingAdapter {
        return SessionPagingAdapter()
    }

    fun recordClick() {
        recordingActivityLauncher.launch(null)
    }
}