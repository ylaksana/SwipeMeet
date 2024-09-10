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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apfinalproject.MainActivity
import com.example.apfinalproject.MainViewModel
import com.example.apfinalproject.adapters.InterestAdapter
import com.example.apfinalproject.adapters.PastEventAdapter
import com.example.apfinalproject.databinding.ProfileFragmentBinding
import com.google.android.flexbox.*

class ProfileFragment : Fragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: ProfileFragmentBinding? = null
    private lateinit var navController: NavController
    private val binding get() = _binding!!
    private val TAG = "ProfileFragment"
    private val args: ProfileFragmentArgs by navArgs()

    private fun initEventAdapter() {
        Log.d(TAG, "initAdapters")
        // Event RV
        viewModel.observeAllEvents().observe(viewLifecycleOwner) { allEvents ->
            Log.d(TAG, "allEvents size = ${allEvents?.size}")
            val userEvents = allEvents?.filter { it.creator == args.User.id }
            Log.d(TAG, "userEvents size = ${userEvents?.size}")
            if (userEvents?.isEmpty() == true) {
                binding.userEventsRV.visibility = View.GONE
                binding.noUserEvents.visibility = View.VISIBLE
            } else {
                binding.noUserEvents.visibility = View.GONE
                binding.userEventsRVHolder.visibility = View.VISIBLE

                val adapter =
                    PastEventAdapter(viewModel) {
                        navController.navigate(
                            ProfileFragmentDirections.actionProfileFragmentToOneEventFragment(it),
                        )
                    }
                binding.userEventsRV.adapter = adapter
                adapter.submitList(userEvents)
                binding.userEventsRV.layoutManager = LinearLayoutManager(context)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Log.d(TAG, "onCreateView")
        _binding = ProfileFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        Log.d(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        viewModel.hideActionBar()
        navController = findNavController()

        val user = args.User
        binding.userName.text = user.firstName
        binding.profileBio.text = user.bio
        viewModel.fetchUserImage(user.profileImage, binding.profileImage)
        if (user.id == viewModel.getActiveUser()?.id) {
            setActiveUserView()
        } else {
            setOtherUserView()
        }
        initInterestAdapter(binding)

        binding.backButton.setOnClickListener {
            navController.popBackStack()
        }
    }

    private fun setActiveUserView() {
        binding.userEventsRVHolder.visibility = View.GONE

        binding.editProfileButton.visibility = View.VISIBLE
        binding.editProfileButton.setOnClickListener {
            navController.navigate(
                ProfileFragmentDirections.actionProfileFragmentToProfileEditFragment(),
            )
        }

        binding.logoutButton.visibility = View.VISIBLE
        binding.logoutButton.setOnClickListener {
            (activity as MainActivity).getAuthUser().logout()
            navController.navigate(
                ProfileFragmentDirections.actionProfileFragmentToHomeFragment(),
            )
        }
    }

    private fun setOtherUserView() {
        binding.editProfileButton.visibility = View.GONE
        binding.logoutButton.visibility = View.GONE
        initEventAdapter()
    }

    private fun initInterestAdapter(binding: ProfileFragmentBinding) {
        val interestAdapter = InterestAdapter(viewModel)
        binding.interestsRV.adapter = interestAdapter

        interestAdapter.submitList(args.User.userInterests)

        binding.interestsRV.layoutManager =
            FlexboxLayoutManager(context).apply {
                flexDirection = FlexDirection.ROW
                flexWrap = FlexWrap.WRAP
                justifyContent = JustifyContent.FLEX_START
            }
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView")
        super.onDestroyView()
        _binding = null
    }
}
