package com.example.trackme.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.trackme.repository.SessionRepository
import com.example.trackme.viewmodel.MapViewModel
import com.example.trackme.viewmodel.SessionViewModel
import javax.inject.Inject

class ViewModelFactory @Inject constructor() : ViewModelProvider.Factory {

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T =
        when {
            modelClass.isAssignableFrom(SessionViewModel::class.java) ->
                SessionViewModel(sessionRepository) as T
            modelClass.isAssignableFrom(MapViewModel::class.java)->
                MapViewModel() as T
            else -> throw IllegalArgumentException("unknown model class")
        }
}