package com.example.trackme.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.example.trackme.repo.entity.Session
import com.example.trackme.repository.SessionPagingSource
import com.example.trackme.repository.SessionRepository
import kotlinx.coroutines.launch

class SessionViewModel(private val repository: SessionRepository) : ViewModel() {

    val flow = Pager(
        config = PagingConfig(
            pageSize = 10,
            maxSize = 100,
            enablePlaceholders = true
        ),
        pagingSourceFactory = { SessionPagingSource(repository.sessionDao) }
    ).flow.cachedIn(viewModelScope)

    fun insertSession(session: Session) {
        viewModelScope.launch {
            repository.insertSession(session)
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
}