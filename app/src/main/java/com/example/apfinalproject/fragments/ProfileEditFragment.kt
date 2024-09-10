package com.example.apfinalproject.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.apfinalproject.MainViewModel
import com.example.apfinalproject.R
import com.example.apfinalproject.adapters.InterestAdapter
import com.example.apfinalproject.databinding.ProfileEditFragmentBinding
import com.example.apfinalproject.model.InterestCategories
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import java.util.UUID

class ProfileEditFragment : Fragment() {
    companion object {
        private const val TAG = "ProfileEditFragment"
    }

    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: ProfileEditFragmentBinding? = null
    private lateinit var navController: NavController
    private var newImageUri: MutableLiveData<Uri> = MutableLiveData()
    private var newImageUUID: String? = null
    private val binding get() = _binding!!

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
        _binding = ProfileEditFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        Log.d(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        navController = findNavController()

        val user = viewModel.getActiveUser()

        if (user != null) {
            binding.userName.text = user.firstName
            if (user.bio != "") {
                binding.profileBio.setText(user.bio)
            } else {
                binding.profileBio.hint = "Enter Bio Here..."
            }
            viewModel.fetchUserImage(user.profileImage, binding.profileImage)
        }

        binding.backButton.setOnClickListener {
            // Exit without saving changes
            navController.popBackStack()
        }

        binding.profileImage.setOnClickListener {
            pickAndSetImage()
        }

        val interestAdapter = InterestAdapter(viewModel, true)

        binding.interestsRV.adapter = interestAdapter
        binding.interestsRV.layoutManager =
            FlexboxLayoutManager(context).apply {
                flexDirection = FlexDirection.ROW
                flexWrap = FlexWrap.WRAP
                justifyContent = JustifyContent.FLEX_START
            }
        val completeInterests =
            InterestCategories.getInterests().map {
                it.category
            }
        interestAdapter.submitList(completeInterests)

        binding.saveButton.setOnClickListener {
            // Save the new bio to the user and exit
            val newBio = binding.profileBio.text.toString()

            val newUser = user?.copy()
            if (newUser != null) {
                newUser.bio = newBio
                newUser.userInterests = interestAdapter.tempInterests
            }
            viewModel.interestsLiveData.postValue(interestAdapter.tempInterests)

            val navOptions =
                NavOptions.Builder()
                    .setPopUpTo(R.id.profileFragment, true)
                    .build()

            if (newImageUri.value != null) {
                Log.d(TAG, "Image URI: $newImageUri")
                uploadUserPhoto(newImageUri.value!!) {
                    if (newUser != null) {
                        newUser.profileImage = newImageUUID!!
                        viewModel.updateUser(newUser)
                        navController.navigate(
                            ProfileEditFragmentDirections.actionProfileEditFragmentToProfileFragment(newUser),
                            navOptions,
                        )
                    }
                }
            } else {
                if (newUser != null) {
                    viewModel.updateUser(newUser)
                    navController.navigate(
                        ProfileEditFragmentDirections.actionProfileEditFragmentToProfileFragment(newUser),
                        navOptions,
                    )
                }
            }
        }
    }

    private fun pickAndSetImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imagePickLauncher.launch(intent)
    }

    private var imagePickLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                newImageUri.value = data?.data
                binding.profileImage.setImageURI(newImageUri.value)
            }
        }

    private fun uploadUserPhoto(
        imageUri: Uri,
        onComplete: () -> Unit,
    ) {
        // Upload the image to the server and set it as the user's profile image
        val storage = viewModel.getStorage()
        val oldImageUUID = viewModel.getActiveUser()?.profileImage
        newImageUUID = UUID.randomUUID().toString()
        if (oldImageUUID != null) {
            storage.uploadUserPhoto(imageUri, newImageUUID!!, oldImageUUID) {
                onComplete()
            }
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView")
        super.onDestroyView()
        _binding = null
    }
}
