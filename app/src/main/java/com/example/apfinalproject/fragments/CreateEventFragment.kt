package com.example.apfinalproject.fragments

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.apfinalproject.MainViewModel
import com.example.apfinalproject.R
import com.example.apfinalproject.databinding.CreateEventBinding
import com.example.apfinalproject.model.Event
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class CreateEventFragment : Fragment() {
    companion object {
        const val TAG = "CreateEventFragment"
        private val collection = "events"
    }

    //     XXX initialize viewModel
    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: CreateEventBinding? = null
    private lateinit var navController: NavController

    //     This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!
    private var imageUri: Uri? = null

    private val photoSelectLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            uri?.let {
                Glide.with(this)
                    .load(it)
                    .into(binding.eventImage)
                imageUri = it
            }
        }

    // Function for populating spinners with their respective arrays
    private fun populateSpinner(
        spinner: Spinner?,
        array: Int,
    ) {
        ArrayAdapter.createFromResource(
            requireContext(),
            array,
            android.R.layout.simple_spinner_item,
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner?.adapter = adapter
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Log.d(TAG, "onCreateView")
        _binding = CreateEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun setOnClickListeners() {
        binding.backButton.setOnClickListener {
            navController = findNavController()
            navController.popBackStack()
        }
        binding.eventImage.setOnClickListener {
            // Selects image, sets to imageView, and sets imageUri string
            photoSelectLauncher.launch("image/*")
        }
        binding.finishButton.setOnClickListener {
            if (binding.editTextTitle.text.isNullOrEmpty()) {
                Snackbar.make(binding.root, "Please enter a title", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (binding.editTextDescription.text.isNullOrEmpty()) {
                Snackbar.make(binding.root, "Please enter a description", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (binding.editTextAddress.text.isNullOrEmpty()) {
                Snackbar.make(binding.root, "Please enter an address", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val event = createEvent()
            viewModel.viewModelScope.launch {
                val locationCoords =
                    async(Dispatchers.IO) {
                        val coords = viewModel.getEventCoords(event.location)
                        event.latitude = coords.latitude
                        event.longitude = coords.longitude
                        Log.d(TAG, "newEvent locationCoords complete: ${event.location} ${event.latitude} ${event.longitude}")
                    }

                val imageUpload =
                    async(Dispatchers.IO) {
                        imageUri?.let {
                            viewModel.uploadImage(it, collection) { uuid ->
                                Log.d(TAG, "newEvent image upload complete")
                                event.imageName = uuid
                            }
                        } ?: run {
                            Log.d(TAG, "creating event without image")
                            Log.d(TAG, "newEvent finished: ${event.title}")
                        }
                    }
                locationCoords.await()
                imageUpload.await()

                viewModel.addEvent(event)
                Log.d(TAG, "newEvent added to db")
            }
            Log.d(TAG, "newEvent exiting")
            navController = findNavController()
            navController.popBackStack()
        }
    }

    private fun populateSpinners() {
        val spinnerMinutes = binding.spinnerMinutes
        val spinnerAMPM = binding.spinnerAMPM
        val spinnerHours = binding.spinnerHours
        val spinnerEvent = binding.spinnerEvent
        val spinnerMonth = binding.spinnerMonth
        val spinnerDay = binding.spinnerDay
        val spinnerYear = binding.spinnerYear

        populateSpinner(spinnerMinutes, R.array.minutes_array)
        populateSpinner(spinnerAMPM, R.array.ampm_array)
        populateSpinner(spinnerHours, R.array.hours_array)
        populateSpinner(spinnerEvent, R.array.genre_array)
        populateSpinner(spinnerMonth, R.array.month_array)
        populateSpinner(spinnerDay, R.array.day_array)
        populateSpinner(spinnerYear, R.array.year_array)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(javaClass.simpleName, "onViewCreated")
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        viewModel.hideActionBar()

        populateSpinners()
        setOnClickListeners()
    }

    private fun createEvent(): Event {
        Log.d(TAG, "createEvent started")
        val event = Event()
        Log.d(TAG, "createEvent new event: $event")
        Log.d(TAG, "createEvent binding: $binding")
        event.title = binding.editTextTitle.text.toString()
        event.description = binding.editTextDescription.text.toString()
        event.location = binding.editTextAddress.text.toString()
        event.date = "${binding.spinnerYear.selectedItem}-${binding.spinnerMonth.selectedItem}-${binding.spinnerDay.selectedItem}"
        event.time = "${binding.spinnerHours.selectedItem}:${binding.spinnerMinutes.selectedItem} ${binding.spinnerAMPM.selectedItem}"
        event.type = binding.spinnerEvent.selectedItem.toString()
        event.creator = viewModel.getActiveUser()?.id ?: "-1"
        Log.d(TAG, "newEvent finished: ${event.title}")
        return event
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView")
        super.onDestroyView()
        _binding = null
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        viewModel.showActionBar()
    }
}
