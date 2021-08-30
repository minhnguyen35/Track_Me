package com.example.trackme.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.trackme.TrackMeApplication
import com.example.trackme.utils.RecordState

class RecordingViewModel(
    private val appPreferences: SharedPreferences
) : ViewModel() {

    var recordState: MutableLiveData<RecordState> = MutableLiveData(RecordState.RECORDING)

    fun changeRecordState(state: RecordState) {
        saveRecordState()
        recordState.postValue(state)
    }

    fun retryRecordState(preferences: SharedPreferences = appPreferences) {
        val state: RecordState = when (preferences.getInt(TrackMeApplication.RECORD_STATE, -1)) {
            RecordState.PAUSED.ordinal -> RecordState.PAUSED
            else -> RecordState.RECORDING
        }
        recordState.postValue(state)
    }

    private fun saveRecordState(preferences: SharedPreferences = appPreferences) {
        preferences.edit()
            .putInt(TrackMeApplication.RECORD_STATE, recordState.value!!.ordinal)
            .apply()
    }


}