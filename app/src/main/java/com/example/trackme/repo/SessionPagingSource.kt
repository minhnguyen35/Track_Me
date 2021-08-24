package com.example.trackme.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.trackme.repo.dao.SessionDao
import com.example.trackme.repo.entity.Session

const val SESSION_STARTING_INDEX = 1

class SessionPagingSource(val sessionDao: SessionDao) : PagingSource<Int, Session>() {

    override fun getRefreshKey(state: PagingState<Int, Session>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Session> {
        return try {
            val position = params.key ?: SESSION_STARTING_INDEX
            val response = sessionDao.getRawList().sortedByDescending { s -> s.id }
            val currentPageCount = response.size - (position - 1) * params.loadSize

            LoadResult.Page(
                data = response,
                prevKey = if (position == SESSION_STARTING_INDEX) null else position - 1,
                nextKey = if (currentPageCount < params.loadSize) null else position + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}