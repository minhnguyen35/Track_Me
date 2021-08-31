package com.example.trackme.viewmodel

import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
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

    val flow = Pager(
        config = PagingConfig(
            pageSize = 10,
            maxSize = 100,
            enablePlaceholders = true
        ),
        pagingSourceFactory = { SessionPagingSource(repository.sessionDao) }
    ).flow.cachedIn(viewModelScope)

    fun insertSession(session: Session, c: (Long) -> Unit) {
        viewModelScope.launch {
            c(repository.insertSession(session))
        }
    }

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

    fun insertPosition(position: Position){
        viewModelScope.launch {
            repository.insertPosition(position)
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

        removePref(session)
    }

    fun retrySession(preferences: SharedPreferences = appPreferences, callback: (session: Session) -> Unit) {
        viewModelScope.launch {
            if(preferences.contains(TrackMeApplication.SAVED_SESSION)){
                val id = preferences.getInt(TrackMeApplication.SAVED_SESSION, 0)
                callback(repository.getSession(id))
            }
            else{
                val session = Session.newInstance()
                val id = repository.insertSession(session)
                callback(Session(id.toInt(), 0f, 0f, 0L, byteArrayOf()))
            }
        }
    }

    fun savePref(session: Session, preferences: SharedPreferences = appPreferences) {
        preferences.edit()
            .putInt(TrackMeApplication.SAVED_SESSION, session.id)
            .apply()
    }

    fun removePref(session: Session, preferences: SharedPreferences = appPreferences){
        preferences.edit()
            .remove(TrackMeApplication.SAVED_SESSION)
            .apply()
    }

    suspend fun getLatLonBound(idSession: Int): LatLngBounds = repository.getLatLonBound(idSession)
}