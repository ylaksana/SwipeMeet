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
import com.example.apfinalproject.databinding.MapEventRowBinding
import com.example.apfinalproject.model.Event

class MapEventAdapter(
    private val viewModel: MainViewModel,
    private val navigateToOneEvent: (Event) -> Unit,
    ) :
ListAdapter<Event, MapEventAdapter.MapEventViewHolder>(MapEventDiff()) {
    private var userLocation: Location? = null

    inner class MapEventViewHolder(val mapEventRowBinding: MapEventRowBinding) :
        RecyclerView.ViewHolder(mapEventRowBinding.root) {
        init {

        }
    }

    fun updateUserLocation(location: Location?) {
        userLocation = location
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): MapEventViewHolder {
        val mapEventRowBinding = MapEventRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MapEventViewHolder(mapEventRowBinding)
    }

    override fun onBindViewHolder(
        holder: MapEventViewHolder,
        position: Int,
    ) {
        val eventRowBinding = holder.mapEventRowBinding
        val event = getItem(position)

        eventRowBinding.infoButton.setOnClickListener{
            navigateToOneEvent(event)
        }
        eventRowBinding.eventName.text = event.title
        eventRowBinding.eventDate.text = event.date
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
        viewModel.fetchEventImage(event.imageName, eventRowBinding.eventImage)
    }

    class MapEventDiff : DiffUtil.ItemCallback<Event>() {
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