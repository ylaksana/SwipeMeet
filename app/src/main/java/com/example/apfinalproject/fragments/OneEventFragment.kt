package com.example.apfinalproject.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apfinalproject.MainActivity
import com.example.apfinalproject.MainViewModel
import com.example.apfinalproject.adapters.UserAdapter
import com.example.apfinalproject.databinding.OneEventBinding

class OneEventFragment : Fragment() {
    companion object {
        private const val TAG = "OneEventFragment"
    }

    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: OneEventBinding? = null
    private lateinit var navController: NavController

    //     This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!
    private val args: OneEventFragmentArgs by navArgs()

    private fun NavController.safeNavigate(direction: NavDirections) {
        currentDestination
            ?.getAction(direction.actionId)
            ?.run {
                navigate(direction)
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
        _binding = OneEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        viewModel.hideActionBar()

        navController = findNavController()

        binding.eventDescription.text = args.Event.description
        binding.eventLocation.text = args.Event.location
        binding.eventTime.text = args.Event.time
        binding.eventDate.text = args.Event.date
        binding.eventName.text = args.Event.title
        viewModel.fetchEventImage(args.Event.imageName, binding.eventImage)

        val user = viewModel.getActiveUser()
        viewModel.observeUserLocation().observe(viewLifecycleOwner) {
            Log.d(TAG, "userLocation: ${it?.latitude}, ${it?.longitude}")
            Log.d(TAG, "eventLocation: ${args.Event.latitude}, ${args.Event.longitude}")
            it?.let {
                val lat1 = it.latitude
                val lon1 = it.longitude
                val lat2 = args.Event.latitude
                val lon2 = args.Event.longitude
                val distance = viewModel.calculateDistanceInMiles(lat1, lon1, lat2, lon2)

                binding.eventDistance.text = String.format("%.2f", distance) + " miles away"
            }
        }

        if (user?.id == args.Event.creator) {
            setCreatorView(binding)
        } else {
            viewModel.fetchUserByUid(args.Event.creator) { creator ->
                if (creator != null) {
                    setVisitorView(binding)
                }
            }
        }
        binding.backButton.setOnClickListener {
            navController.popBackStack()
        }
    }

    private fun setCreatorView(binding: OneEventBinding) {
        binding.creatorHolder.visibility = View.GONE
        binding.editEventButton.visibility = View.VISIBLE
        binding.editEventButton.setOnClickListener {
            navController.safeNavigate(
                OneEventFragmentDirections.actionOneEventFragmentToEditEventFragment(args.Event),
            )
        }

        if (args.Event.yesSwipes.isNotEmpty()) {
            binding.interestedUsersHolder.visibility = View.VISIBLE
            initAdapter(binding)
        } else {
            binding.interestedUsersHolder.visibility = View.GONE
        }
    }

    private fun setVisitorView(binding: OneEventBinding) {
        binding.editEventButton.visibility = View.GONE
        binding.interestedUsersHolder.visibility = View.GONE
        binding.creatorHolder.visibility = View.VISIBLE

        viewModel.fetchUserByUid(args.Event.creator) { creator ->
            creator?.let {
                binding.posterName.text = creator.firstName
                viewModel.fetchUserImage(creator.profileImage, binding.profileImage)

                binding.creatorHolder.setOnClickListener {
                    navController.safeNavigate(
                        OneEventFragmentDirections.actionOneEventFragmentToProfileFragment(creator),
                    )
                }
            }
        }
    }

    private fun initAdapter(binding: OneEventBinding) {
        val adapter =
            UserAdapter(viewModel) { clickedUser ->
                // On Click Callback
                viewModel.enterChatRoom(args.Event.creator, clickedUser.id) { conv ->
                    navController.safeNavigate(
                        OneEventFragmentDirections.actionOneEventFragmentToChatFragment(conv),
                    )
                }
            }
        binding.interestedUsersRV.adapter = adapter
        viewModel.fetchUsersByIds(args.Event.yesSwipes) {
            adapter.submitList(it)
        }
        binding.interestedUsersRV.layoutManager = LinearLayoutManager(context)
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView")
        _binding = null
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        viewModel.showActionBar()
        super.onDestroyView()
    }
}
