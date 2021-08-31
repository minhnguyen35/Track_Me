package com.example.trackme.utils

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.trackme.repo.SessionRepository
import com.example.trackme.viewmodel.RecordingViewModel
import com.example.trackme.viewmodel.SessionViewModel
import javax.inject.Inject

class ViewModelFactory @Inject constructor() : ViewModelProvider.Factory {

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var appPreferences: SharedPreferences

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T =
        when {
            modelClass.isAssignableFrom(SessionViewModel::class.java) ->
                SessionViewModel(sessionRepository, appPreferences) as T
            modelClass.isAssignableFrom(RecordingViewModel::class.java)->
                RecordingViewModel(appPreferences) as T
            else -> throw IllegalArgumentException("unknown model class")
        }
}