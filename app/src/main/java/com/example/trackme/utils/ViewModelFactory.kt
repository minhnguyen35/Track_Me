package com.example.trackme.utils

import androidx.core.app.NotificationCompat
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
    lateinit var notificationBuilder: NotificationCompat.Builder

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T =
        when {
            modelClass.isAssignableFrom(SessionViewModel::class.java) ->
                SessionViewModel(sessionRepository) as T
            modelClass.isAssignableFrom(RecordingViewModel::class.java) ->
                RecordingViewModel(notificationBuilder, sessionRepository) as T
            else -> throw IllegalArgumentException("unknown model class")
        }
}