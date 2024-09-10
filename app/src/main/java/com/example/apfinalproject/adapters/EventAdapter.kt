package com.example.apfinalproject.adapters

import android.location.Location
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.apfinalproject.MainViewModel
import com.example.apfinalproject.databinding.EventRowBinding
import com.example.apfinalproject.model.Event

class EventAdapter(
    private val viewModel: MainViewModel,
    private val navigateToOneEvent: (Event) -> Unit,
) :
    ListAdapter<Event, EventAdapter.EventViewHolder>(EventDiff()) {
    private var userLocation: Location? = null

    fun updateUserLocation(location: Location?) {
        userLocation = location
        notifyDataSetChanged()
    }

    inner class EventViewHolder(val eventRowBinding: EventRowBinding) :
        RecyclerView.ViewHolder(eventRowBinding.root) {
        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Get the RedditPost
                    val post = getItem(position)
                    // Go to OnePost
                    navigateToOneEvent(post)
                    viewModel.hideActionBar()
                }
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): EventViewHolder {
        val eventRowBinding = EventRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(eventRowBinding)
    }

    override fun onBindViewHolder(
        holder: EventViewHolder,
        position: Int,
    ) {
        val eventRowBinding = holder.eventRowBinding
        val event = getItem(position)

        eventRowBinding.eventTitle.text = event.title
        eventRowBinding.eventDate.text = event.date
        eventRowBinding.peopleInterested.text = "${event.yesSwipes.size} People Interested"

        userLocation?.let {
            eventRowBinding.miles.visibility = View.VISIBLE
            eventRowBinding.timeDot.visibility = View.VISIBLE
            Log.d("Miles", "${it.latitude}, ${it.longitude}, ${event.latitude}, ${event.longitude}")
            eventRowBinding.miles.text =
                String.format(
                    "%.2f miles",
                    viewModel.calculateDistanceInMiles(
                        it.latitude,
                        it.longitude,
                        event.latitude,
                        event.longitude,
                    ),
                )
        } ?: run {
            eventRowBinding.miles.visibility = View.GONE
            eventRowBinding.timeDot.visibility = View.GONE
        }

        Log.d("EventAdapter", "fetching image: ${event.imageName}")
        viewModel.fetchEventImage(event.imageName, eventRowBinding.image)
    }

    class EventDiff : DiffUtil.ItemCallback<Event>() {
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
                oldItem.type == newItem.type &&
                oldItem.imageName == newItem.imageName
        }
    }
}
