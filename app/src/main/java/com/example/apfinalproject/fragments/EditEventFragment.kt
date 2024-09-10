package com.example.apfinalproject.fragments

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.apfinalproject.MainViewModel
import com.example.apfinalproject.R
import com.example.apfinalproject.databinding.EditEventFragmentBinding
import com.example.apfinalproject.model.Event
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers

class EditEventFragment : Fragment() {
    companion object {
        private const val TAG = "EditEventFragment"
    }

    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: EditEventFragmentBinding? = null
    private var newImageUri: Uri? = null
    private val binding get() = _binding!!
    private val args: EditEventFragmentArgs by navArgs()
    private lateinit var navController: NavController

    private val photoSelectLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            uri?.let {
                Glide.with(this)
                    .load(it)
                    .into(binding.eventImage)
                newImageUri = it
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Log.d(TAG, "onCreateView")
        _binding = EditEventFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        Log.d(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        navController = findNavController()

        bindEventInfo()
        addOnClickListeners()
    }

    private fun bindEventInfo() {
        binding.eventNameET.setText(args.Event.title)
        binding.eventDescriptionET.setText(args.Event.description)
        binding.eventLocationET.setText(args.Event.location)
        binding.eventDateET.setText(args.Event.date)
        binding.eventTimeET.setText(args.Event.time)
        viewModel.fetchEventImage(args.Event.imageName, binding.eventImage)
    }

    private fun addOnClickListeners() {
        binding.saveButton.setOnClickListener {
            saveChanges { newEvent ->
                Log.d(TAG, "changes saved for image ${newEvent.imageName}")

                // Removes the editEvent fragment from the backstack
                val navOptions =
                    NavOptions.Builder()
                        .setPopUpTo(R.id.oneEventFragment, true)
                        .build()

                navController.navigate(
                    EditEventFragmentDirections.actionEditEventFragmentToOneEventFragment(newEvent),
                    navOptions,
                )
            }
        }
        binding.backButton.setOnClickListener {
            navController.popBackStack()
        }
        binding.eventImage.setOnClickListener {
            photoSelectLauncher.launch("image/*")
        }
    }

    private fun saveChanges(navigateCallback: (Event) -> Unit) {
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            // Determine if a new location needs to be fetched
            val newLocation =
                if (binding.eventLocationET.text.toString() != args.Event.location) {
                    // Fetch new location asynchronously and await the result
                    async {
                        viewModel.getEventCoords(binding.eventLocationET.text.toString())
                    }.await()
                } else {
                    null
                }

            // Create the event object and possibly update it with new location data
            val newEvent =
                createEvent().apply {
                    if (newImageUri != null) {
                        // Upload image and update event details asynchronously
                        async {
                            viewModel.uploadImage(newImageUri!!, "events") {
                                imageName = it
                            }
                        }.await()
                    }
                    newLocation?.let {
                        latitude = it.latitude
                        longitude = it.longitude
                    }
                }

            // Update the event in the ViewModel or database
            viewModel.updateEvent(newEvent)

            // Switch to the Main thread to perform UI operations such as navigation
            withContext(Dispatchers.Main) {
                // Navigate only after all updates are complete
                navigateCallback(newEvent)
            }
        }
    }

    private fun createEvent(): Event {
        val newEvent = Event()
        newEvent.id = args.Event.id
        newEvent.creator = args.Event.creator
        newEvent.type = args.Event.type
        newEvent.yesSwipes = args.Event.yesSwipes
        newEvent.noSwipes = args.Event.noSwipes
        newEvent.title = binding.eventNameET.text.toString()
        newEvent.description = binding.eventDescriptionET.text.toString()
        newEvent.location = binding.eventLocationET.text.toString()
        newEvent.date = binding.eventDateET.text.toString()
        newEvent.time = binding.eventTimeET.text.toString()
        newEvent.imageName = args.Event.imageName
        return newEvent
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
