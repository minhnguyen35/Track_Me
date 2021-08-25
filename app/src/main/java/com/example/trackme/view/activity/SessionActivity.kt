package com.example.trackme.view.activity

import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trackme.TrackMeApplication
import com.example.trackme.databinding.ActivitySessionBinding
import com.example.trackme.repo.entity.Session
import com.example.trackme.view.adapter.SessionPagingAdapter
import com.example.trackme.viewmodel.SessionViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.graphics.Bitmap
import com.example.trackme.R
import java.io.ByteArrayOutputStream


class SessionActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySessionBinding
    var i = 0f

    @Inject
    lateinit var viewModel: SessionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.handler = this

        initDi()
        initRecycler()
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

    fun recordClick(){
    }
}