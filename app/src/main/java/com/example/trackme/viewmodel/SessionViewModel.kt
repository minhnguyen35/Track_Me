package com.example.trackme.viewmodel

import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.liveData
import com.example.trackme.TrackMeApplication
import com.example.trackme.repo.entity.Position
import com.example.trackme.repo.entity.Session
import com.example.trackme.repository.SessionPagingSource
import com.example.trackme.repo.SessionRepository
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SessionViewModel(
    private val repository: SessionRepository,
    private val appPreferences: SharedPreferences
) : ViewModel() {

    val sessionList = repository.getSessionList()

    val listPagerData = Pager(
        config = PagingConfig(
            pageSize = 10,
            maxSize = 100,
            enablePlaceholders = true
        ),
        pagingSourceFactory = { SessionPagingSource(sessionList.value ?: listOf()) }
    ).liveData.cachedIn(viewModelScope)

    fun updateSession(session: Session) {
        viewModelScope.launch {
            Log.d("AAA", "updateSession: $session")
            repository.updateSession(session)
        }
    }

    fun deleteSession(session: Session) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }

    fun deletePositions(idSession: Int){
        viewModelScope.launch {
            repository.deletePositions(idSession)
        }
    }

    fun clearData(result: Int, session: Session) {
        when(result){
            AppCompatActivity.RESULT_OK -> {
                deletePositions(session.id)
            }
            AppCompatActivity.RESULT_CANCELED -> {
                deleteSession(session)
            }
        }
    }

    fun getLatLonBound(idSession: Int): LatLngBounds = repository.getLatLonBound(idSession)
}