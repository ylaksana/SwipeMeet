package com.example.apfinalproject.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apfinalproject.MainActivity
import com.example.apfinalproject.MainViewModel
import com.example.apfinalproject.R
import com.example.apfinalproject.adapters.EventAdapter
import com.example.apfinalproject.databinding.FragmentRvBinding

class EventListFragment : Fragment() {
    companion object {
        private const val TAG = "EventListFragment"
    }

    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentRvBinding? = null
    private lateinit var navController: NavController
    private var eventAdapter: EventAdapter? = null

    //     This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRvBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun initAdapters(binding: FragmentRvBinding) {
        eventAdapter =
            EventAdapter(viewModel) { event ->
                navController.navigate(
                    EventListFragmentDirections.actionEventListFragmentToOneEventFragment(event),
                )
            }
        binding.rv.adapter = eventAdapter
        viewModel.fetchMyEvents { events ->
            Log.d("MainActivity", "eventList length: ${events.size}")
            eventAdapter?.submitList(events)
        }
        binding.rv.layoutManager = LinearLayoutManager(context)

        viewModel.observeUserLocation().observe(viewLifecycleOwner) {
            Log.d(TAG, "userLocation: ${it?.latitude}, ${it?.longitude}")
            eventAdapter?.updateUserLocation(it)
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(javaClass.simpleName, "onViewCreated")
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        viewModel.hideActionBar()
        navController = findNavController()

        initAdapters(binding)

        // Insert items into spinner
        val spinner = _binding?.titleTextView
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.list_type_array,
            android.R.layout.simple_spinner_item,
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner?.adapter = adapter
        }

        // add init adapter and init spinners to separate function
        // Set the spinner item selection listener
        spinner?.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    // probably don't need this null check, can view ever be null?
                    if (view != null) {
                        val selectedItem = parent.getItemAtPosition(position).toString()
                        when (selectedItem) {
                            "My Events" -> {
                                // Change the filter to "My Events"
                                viewModel.setEvents(true)
                            }

                            "My Requests" -> {
                                // Change the filter to "My Requests"
                                viewModel.setEvents(false)
                            }
                        }
                        viewModel.observeUserEvents().observe(viewLifecycleOwner) {
                            Log.d("filterSpinner", "filterList length: $it")
                            eventAdapter?.submitList(it)
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    viewModel.setEvents(true)
                    viewModel.observeNetTypeEvents().observe(viewLifecycleOwner) {
                        Log.d("filterSpinner", "filterList length: $it")
                        eventAdapter?.submitList(it)
                    }
                }
            }

        binding.backButton.setOnClickListener {
            Log.d(TAG, "back pressed $navController")
            navController.popBackStack()
            Log.d(TAG, "leaving backButton")
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity).requestLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        (activity as MainActivity).stopLocationUpdates()
    }

    override fun onDestroyView() {
        _binding = null
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        viewModel.showActionBar()
        super.onDestroyView()
    }
}
