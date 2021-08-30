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
        recordState.postValue(state)
        saveRecordState(state)
    }

    fun retryRecordState(preferences: SharedPreferences = appPreferences) {
        val state: RecordState = when (preferences.getInt(TrackMeApplication.RECORD_STATE, -1)) {
            RecordState.PAUSED.ordinal -> RecordState.PAUSED
            else -> RecordState.RECORDING
        }
        recordState.postValue(state)
    }

    private fun saveRecordState(state: RecordState, preferences: SharedPreferences = appPreferences) {
        val iss = preferences.edit()
            .putInt(TrackMeApplication.RECORD_STATE, state.ordinal)
            .commit()
    }


}