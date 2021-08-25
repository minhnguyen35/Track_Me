package com.example.trackme.view.activity

import android.content.Context
import android.content.Intent
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
import com.example.trackme.view.activity.RecordingActivity
import com.example.trackme.view.adapter.SessionPagingAdapter
import com.example.trackme.viewmodel.SessionViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

class SessionActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySessionBinding

    private lateinit var recordingActivityLauncher: ActivityResultLauncher<Any?>
    private val recordingContract = object : ActivityResultContract<Any?, Int>() {
        override fun createIntent(context: Context, input: Any?): Intent {
            return Intent(this@SessionActivity, RecordingActivity::class.java)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Int {
            return resultCode
        }
    }
    private val recordingCallback = ActivityResultCallback<Int> { result ->
        when (result) {
            RESULT_OK -> (binding.recyclerSession.adapter as SessionPagingAdapter).refresh()
            else -> {
            }
        }
    }

    @Inject
    lateinit var viewModel: SessionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.handler = this

        recordingActivityLauncher = registerForActivityResult(recordingContract, recordingCallback)

        initDi()
        initRecycler()
    }

    override fun onDestroy() {
        recordingActivityLauncher.unregister()
        super.onDestroy()
    }

    private fun initRecycler() {
        val adapter = getAdapter()

        lifecycleScope.launch {
            viewModel.flow.collectLatest {
                adapter.submitData(it)
                Log.d("AAA", "onCreateView: data submitted")
            }
        }

        binding.recyclerSession.apply {
            this.adapter = adapter
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