package com.example.apfinalproject.fragments

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apfinalproject.MainActivity
import com.example.apfinalproject.MainViewModel
import com.example.apfinalproject.R
import com.example.apfinalproject.adapters.MapEventAdapter
import com.example.apfinalproject.adapters.PastEventAdapter
import com.example.apfinalproject.databinding.MapFragmentBinding
import com.example.apfinalproject.model.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapFragment : Fragment() {
    private var _binding: MapFragmentBinding? = null
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var navController: NavController
    private val binding get() = _binding!!
    private lateinit var mapView: MapView
    private lateinit var adapter: MapEventAdapter
    private lateinit var boundingBox: BoundingBox
    private lateinit var userLocation: GeoPoint
    private var allEvents: List<Event> = mutableListOf()
    private var filteredEvents: List<Event> = mutableListOf()
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0
    private var distance: String = ""

    private fun initTouchHelper(): ItemTouchHelper {
        val simpleItemTouchCallback =
            object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder,
                ): Boolean {
                    return true
                }

                override fun onSwiped(
                    viewHolder: RecyclerView.ViewHolder,
                    direction: Int,
                ) {
                    val position = viewHolder.bindingAdapterPosition
                    val event = adapter.currentList[position]
                    Log.d(HomeFragment.TAG, "Swipe delete $direction")
                    Log.d(HomeFragment.TAG, "adapter: $adapter")
                    Log.d(HomeFragment.TAG, "position: $position")
                    Log.d(HomeFragment.TAG, "event: ${adapter.currentList}")
                    Log.d(HomeFragment.TAG, "event: ${event?.title}")

                    event?.let {
                        val eventId = it.id
                        viewModel.addEventSwipe(eventId, direction)
                        viewModel.removeEventFromView(event)
                        viewModel.fetchEventList()

                        Log.d(HomeFragment.TAG, "event removed: ${event.title}")

                        // find new way to update, itemRemovedAt duplicated bound objects
                        adapter.notifyItemRemoved(position)
                    }
                }
            }
        return ItemTouchHelper(simpleItemTouchCallback)
    }

    private fun initAdapter(binding: MapFragmentBinding) {
        val rv = binding.eventRV
        rv.layoutManager = LinearLayoutManager(context)
        adapter = MapEventAdapter(viewModel) { event ->
            val action = MapFragmentDirections.actionMapFragmentToOneEventFragment(event)
            navController.navigate(action)
        }
        rv.adapter = adapter
        initTouchHelper().attachToRecyclerView(rv)
    }

    private fun updateEventsBasedOnBoundingBox() {
        filteredEvents = filterEvents(allEvents, boundingBox)
        adapter.submitList(filteredEvents)
        setEventMarkers(filteredEvents)
    }

    // Function for filtering events based on the bounding box
    private fun filterEvents(
        events: List<Event>,
        boundingBox: BoundingBox,
    ): List<Event> {
        val minX = boundingBox.lonWest
        val maxX = boundingBox.lonEast
        val minY = boundingBox.latSouth
        val maxY = boundingBox.latNorth
        Log.d("MapFragment", "Min X: $minX, Max X: $maxX, Min Y: $minY, Max Y: $maxY")
        return events.filter { event ->
            event.latitude in minY..maxY && event.longitude in minX..maxX
        }
    }

    private fun setEventMarkers(events: List<Event>) {
        mapView.overlays.clear()

        // Only set markers if the fragment is added and the view is not null
        // Avoids issue where you navigate away from the fragment quickly
        if (isAdded && view != null) {
            viewModel.observeUserLocation().observe(viewLifecycleOwner) { location ->
                if (location != null) {
                    Marker(mapView).apply {
                        position = GeoPoint(userLatitude, userLongitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = ResourcesCompat.getDrawable(resources, R.drawable.home, null)
                        title = "Current Location"
                        mapView.overlays.add(this)
                    }
                }
            }
            if (events.isEmpty()) {
                binding.noEvents.visibility = View.VISIBLE
            } else {
                binding.noEvents.visibility = View.GONE
            }
            events.forEach { nonUserEvent ->
                Log.d(
                    "MapFragment",
                    "Event: ${nonUserEvent.title}, ${nonUserEvent.latitude}, ${nonUserEvent.longitude}",
                )
                Marker(mapView).apply {
                    position = GeoPoint(nonUserEvent.latitude, nonUserEvent.longitude)
                    distance =
                        String.format(
                            "%.2f",
                            viewModel.calculateDistanceInMiles(
                                userLatitude,
                                userLongitude,
                                nonUserEvent.latitude,
                                nonUserEvent.longitude,
                            ),
                        )
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "${nonUserEvent.title}\n$distance miles away"
                    mapView.overlays.add(this)
                    Log.d(
                        "MapFragment",
                        "Added marker at: ${position.latitude}, ${position.longitude}",
                    )
                }
            }
            // Redraw the map
            mapView.invalidate()
        }
    }

    // Function for setting new user location
    private fun setNewLocation(location: Location?) {
        Log.d("MapFragment", "Setting new location: $location")

        val mapController = mapView.controller
        if (location != null) {
            userLatitude = location.latitude
            userLongitude = location.longitude
            Log.d("MapFragment", "Last Location: Latitude: $userLatitude, Longitude: $userLongitude")
            Log.d("OSM", "Last Location: $location")
            // Initialize the map controller here
            mapController.setZoom(12.0)
            mapController.setCenter(GeoPoint(userLatitude, userLongitude))
            mapView.overlays.clear()

            userLocation = GeoPoint(location.latitude, location.longitude)

            // Create a new Marker at the user's current location
            val marker = Marker(mapView)
            marker.position = userLocation
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = "Current Location"
            marker.icon = ResourcesCompat.getDrawable(resources, R.drawable.home, null)
            // Add the marker to the map's overlays
            mapView.overlays.add(marker)

            // Get the current bounding box of the visible area
            boundingBox = mapView.boundingBox

            // Log the values
            viewModel.setEvents(false)
            updateEventsBasedOnBoundingBox()
        } else {
            Log.d("MapFragment", "Last location is null")
            viewModel.viewModelScope.launch {
                val nullUserLocation =
                    async(Dispatchers.IO) {
                        viewModel.getEventCoords("United States")
                    }
                withContext(Dispatchers.Main) {
                    val coords = nullUserLocation.await()

                    mapController.setZoom(4.5)
                    mapController.setCenter(GeoPoint(coords.latitude, coords.longitude))

                    // Get the current bounding box of the visible area
                    boundingBox = mapView.boundingBox

                    // Log the values
                    viewModel.setEvents(false)
                    updateEventsBasedOnBoundingBox()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        (activity as MainActivity).requestLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        (activity as MainActivity).stopLocationUpdates()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = MapFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.observeNetTypeEvents().observe(viewLifecycleOwner) { events ->
            // Save the events
            allEvents = events
        }

        initAdapter(binding)

        mapView = binding.mapFrag
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)

        viewModel.observeUserLocation().observe(viewLifecycleOwner) { location ->
            setNewLocation(location)
        }

        // Find the navController
        navController = findNavController()

        // Back Button
        _binding?.backButton?.setOnClickListener {
            navController.popBackStack()
        }

        // Add a map listener for when the user moves and zooms the map
        mapView.addMapListener(
            object : MapListener {
                override fun onScroll(event: ScrollEvent?): Boolean {
                    // Code to execute when the map is scrolled
                    Log.d("OSM", "Map scrolled")
                    // Get the current bounding box of the visible area
                    boundingBox = mapView.boundingBox
                    updateEventsBasedOnBoundingBox()
                    return true
                }

                override fun onZoom(event: ZoomEvent?): Boolean {
                    // Code to execute when the map is zoomed
                    boundingBox = mapView.boundingBox
                    updateEventsBasedOnBoundingBox()
                    return true
                }
            },
        )

        // Set user agent to prevent getting banned from the OSM servers
        Configuration.getInstance().userAgentValue = "SwipeMeet"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        viewModel.showActionBar()
        viewModel.observeUserLocation().removeObservers(viewLifecycleOwner)
        viewModel.observeNetTypeEvents().removeObservers(viewLifecycleOwner)
    }
}
