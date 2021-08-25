package com.example.trackme.view.adapter

import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.trackme.databinding.RowItemSessionBinding
import com.example.trackme.repo.entity.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SessionPagingAdapter : PagingDataAdapter<Session, SessionPagingAdapter.SessionViewHolder>(
    SESSION_COMPARATOR
) {

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding =
            RowItemSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SessionViewHolder(binding)
    }

    class SessionViewHolder(val binding: RowItemSessionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(session: Session) {
            binding.session = session
        }
    }

    companion object {
        val SESSION_COMPARATOR = object : DiffUtil.ItemCallback<Session>() {
            override fun areItemsTheSame(oldItem: Session, newItem: Session): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Session, newItem: Session): Boolean {
                return oldItem == newItem
            }

        }
    }
}