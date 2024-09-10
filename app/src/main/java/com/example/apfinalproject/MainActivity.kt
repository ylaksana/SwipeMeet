package com.example.apfinalproject

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.apfinalproject.databinding.ActionBarBinding
import com.example.apfinalproject.databinding.ActivityMainBinding
import com.example.apfinalproject.fragments.HomeFragmentDirections
import com.example.apfinalproject.model.invalidUser
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
    }

    private var actionBarBinding: ActionBarBinding? = null

    private val viewModel: MainViewModel by viewModels()
    private lateinit var navController: NavController
    private lateinit var authUser: AuthUser
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var location: android.location.Location? = null

    private fun initActionBar(actionBar: ActionBar) {
        // Disable the default and enable the custom
        actionBar.setDisplayShowTitleEnabled(false)
        actionBar.setDisplayShowCustomEnabled(true)
        actionBarBinding = ActionBarBinding.inflate(layoutInflater)
        // Apply the custom view
        actionBar.customView = actionBarBinding?.root
        viewModel.initActionBarBinding(actionBarBinding!!)
    }

    // https://nezspencer.medium.com/navigation-components-a-fix-for-navigation-action-cannot-be-found-in-the-current-destination-95b63e16152e
    // You get a NavDirections object from a Directions object like
    // HomeFragmentDirections.
    // safeNavigate checks if you are in the source fragment for the directions
    // object and if not does nothing
    private fun NavController.safeNavigate(direction: NavDirections) {
        currentDestination
            ?.getAction(direction.actionId)
            ?.run {
                navigate(direction)
            }
    }

    private fun actionBarTitleLaunchProfile() {
        // XXX Write me actionBarBinding, safeNavigate
        actionBarBinding?.profileButton?.setOnClickListener {
            navController.safeNavigate(
                HomeFragmentDirections.actionHomeFragmentToProfileFragment(
                    viewModel.getActiveUser() ?: invalidUser,
                ),
            )
        }
    }

    private fun actionBarLaunchMap() {
        // XXX Write me actionBarBinding, safeNavigate
        actionBarBinding?.mapButton?.setOnClickListener {
            navController.safeNavigate(HomeFragmentDirections.actionHomeFragmentToMapFragment())
        }
    }

    private fun actionBarCreateEvent() {
        // XXX Write me
        actionBarBinding?.createButton?.setOnClickListener {
            navController.safeNavigate(HomeFragmentDirections.actionHomeFragmentToCreateEventFragment())
        }
    }

    private fun actionBarEventList() {
        // XXX Write me
        actionBarBinding?.eventListButton?.setOnClickListener {
            navController.safeNavigate(HomeFragmentDirections.actionHomeFragmentToEventListFragment())
        }
    }

    private fun actionBarChatList() {
        // XXX Write me
        actionBarBinding?.chatButton?.setOnClickListener {
            navController.safeNavigate(HomeFragmentDirections.actionHomeFragmentToChatListFragment())
        }
    }

    private fun initTitleObservers() {
        // Observe title changes
    }

    fun getAuthUser(): AuthUser {
        return authUser
    }

    private var locationCallback: LocationCallback? = null

    private fun requestNewLocationData() {
        Log.d(TAG, "Requesting new location data")
        val locationRequest =
            LocationRequest.Builder(10000L)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setIntervalMillis(10000L) // Desired interval for active location updates, in milliseconds.
                .setMinUpdateIntervalMillis(5000L) // Fastest rate for active location updates, in milliseconds.
                .setMaxUpdates(1) // Limits the total number of location updates.
                .build()

        val locationCallback =
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    location = locationResult.lastLocation
                    Log.d("MapFragment", "Updated Location: Latitude: ${location?.latitude}, Longitude: ${location?.longitude}")
                    // Use the location as needed
                    viewModel.updateUserLocation(location)
                }
            }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        } else {
            Log.d("MapFragment", "No location permissions")
            viewModel.updateUserLocation(location)
        }
    }

    fun requestLocationUpdates() {
        val backgroundThread = HandlerThread("BackgroundThread")
        backgroundThread.start()
        val backgroundHandler = Handler(backgroundThread.looper)

        backgroundHandler.post {
            requestNewLocationData()
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        Log.d(TAG, ">>inflating binding")
        val activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        Log.d(TAG, ">>setting content view")
        setContentView(activityMainBinding.root)
        Log.d(TAG, ">>setting support action bar")
        setSupportActionBar(activityMainBinding.toolbar)
        supportActionBar?.let {
            initActionBar(it)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (!hasLocationPermissions()) {
            requestLocationPermissions()
        }
        requestLocationUpdates()

        initTitleObservers()
        actionBarTitleLaunchProfile()
        actionBarLaunchMap()
        actionBarChatList()
        actionBarCreateEvent()
        actionBarEventList()

        // Set up our nav graph
        navController = findNavController(R.id.main_frame)
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        // If we have a toolbar (not actionbar) we don't need to override
        // onSupportNavigateUp().
        activityMainBinding.toolbar.setupWithNavController(navController, appBarConfiguration)
        // setupActionBarWithNavController(navController, appBarConfiguration)
        Log.d(TAG, "onCreate end")
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
            1,
        )
    }

    private fun hasLocationPermissions(): Boolean {
        return (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        ) &&
            (
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED
            )
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
        Log.d(TAG, ">>before authuser")
        authUser = AuthUser(activityResultRegistry)
        Log.d(TAG, ">>after authuser")
        lifecycle.addObserver(authUser)
        Log.d(TAG, ">>auth user: ${authUser.observeAuthId().value}")

        authUser.observeAuthId().observe(this) { authId ->
            Log.d(TAG, ">>observeAuthId started with $authId")
            // XXX Write me, user status has changed
            if ((authId == null) or (authId == invalidUser.id)) {
                Log.d(TAG, ">> No user logged into Firebase : $authId")
                viewModel.activeUser.postValue(invalidUser)
            } else {
                Log.d(TAG, ">>User is logged in with uid $authId : ${authUser.getName()}")
                viewModel.setActiveUser(authId) { user ->
                    if (user == invalidUser) {
                        navController.safeNavigate(
                            HomeFragmentDirections.actionHomeFragmentToCreateUserFragment(
                                authId,
                                authUser.getName(),
                                authUser.getEmail(),
                            ),
                        )
                    }
                }
            }
        }
        Log.d(TAG, "onStart end")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
        stopLocationUpdates()
    }
}
