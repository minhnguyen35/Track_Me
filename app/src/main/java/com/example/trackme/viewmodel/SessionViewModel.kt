package com.example.trackme.viewmodel

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.example.trackme.repo.entity.Position
import com.example.trackme.repo.entity.Session
import com.example.trackme.repository.SessionPagingSource
import com.example.trackme.repository.SessionRepository
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor

class SessionViewModel(private val repository: SessionRepository) : ViewModel() {

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
    }
}