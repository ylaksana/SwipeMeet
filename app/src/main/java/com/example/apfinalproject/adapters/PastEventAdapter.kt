// Adapter to display past events in the RV on the user profile page

package com.example.apfinalproject.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.apfinalproject.MainViewModel
import com.example.apfinalproject.databinding.PastEventRowBinding
import com.example.apfinalproject.model.Event

class PastEventAdapter(
    private val viewModel: MainViewModel,
    private val navigateToOneEvent: (Event) -> Unit,
) :
    ListAdapter<Event, PastEventAdapter.PastEventViewHolder>(pastEventDiff()) {
    inner class PastEventViewHolder(val pastEventRowBinding: PastEventRowBinding) :
        RecyclerView.ViewHolder(pastEventRowBinding.root) {
        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Get the RedditPost
                    val post = getItem(position)
                    // Go to OnePost
                    Log.d("MapToOnePost", "Navigating to OnePost: ${post.title}")
                    navigateToOneEvent(post)
                    viewModel.hideActionBar()
                }
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): PastEventViewHolder {
        val pastEventRowBinding = PastEventRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PastEventViewHolder(pastEventRowBinding)
    }

    override fun onBindViewHolder(
        holder: PastEventViewHolder,
        position: Int,
    ) {
        val pastEventRowBinding = holder.pastEventRowBinding
        val event = getItem(position)
        pastEventRowBinding.pastEventTitle.text = event.title
        pastEventRowBinding.root.setOnClickListener {
            navigateToOneEvent(event)
        }
    }

    class pastEventDiff : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(
            oldItem: Event,
            newItem: Event,
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: Event,
            newItem: Event,
        ): Boolean {
            return oldItem.location == newItem.location &&
                oldItem.title == newItem.title &&
                oldItem.description == newItem.description &&
                oldItem.date == newItem.date &&
                oldItem.time == newItem.time &&
                oldItem.creator == newItem.creator &&
                oldItem.imageName == newItem.imageName
        }
    }
}
