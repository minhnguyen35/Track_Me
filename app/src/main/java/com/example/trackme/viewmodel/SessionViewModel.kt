package com.example.trackme.viewmodel

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.liveData
import com.example.trackme.repo.entity.Session
import com.example.trackme.repo.SessionPagingSource
import com.example.trackme.repo.SessionRepository
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.launch

class SessionViewModel(
    private val repository: SessionRepository
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
    fun deleteErrorSession(){
        viewModelScope.launch {
            repository.deleteError()
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


}