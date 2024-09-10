package com.example.apfinalproject

import android.location.Location
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import androidx.core.view.isGone
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apfinalproject.api.OSMApi
import com.example.apfinalproject.api.OSMRepository
import com.example.apfinalproject.databinding.ActionBarBinding
import com.example.apfinalproject.glide.Glide
import com.example.apfinalproject.model.Conversation
import com.example.apfinalproject.model.Event
import com.example.apfinalproject.model.OSMLocation
import com.example.apfinalproject.model.User
import com.example.apfinalproject.model.invalidUser
import com.example.apfinalproject.storage.Storage
import com.example.apfinalproject.storage.ViewModelDBHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class MainViewModel : ViewModel() {
    companion object {
        private const val TAG = "MainViewModel"
    }

    private var actionBarBinding: ActionBarBinding? = null
    private val storage = Storage()
    private val db = ViewModelDBHelper()
    private val filterTerm: MutableLiveData<String> = MutableLiveData()
    private var isMyEvents: MutableLiveData<Boolean> = MutableLiveData()
    private val userLocation = MutableLiveData<Location?>()

    fun updateUserLocation(location: Location?) {
        Log.d(TAG, "updateUserLocation: $location")
        userLocation.postValue(location)
    }

    fun observeUserLocation(): LiveData<Location?> {
        return userLocation
    }

    var activeUser =
        MutableLiveData<User>().apply {
            Log.d(TAG, ">>activeUser init")
            invalidUser
        }

    private var events = MutableLiveData<List<Event>?>()

    private var nonUserEvents =
        MediatorLiveData<List<Event>>().apply {
            addSource(events) { originalList ->
                val filteredList =
                    originalList?.filter { event ->
                        Log.d(TAG, "Filtering events: ${event.title}")
                        Log.d(TAG, "Active user: ${activeUser.value?.id}")
                        Log.d(TAG, "Event.yesSwipes: ${event.yesSwipes}")
                        Log.d(TAG, "event.noSwipes: ${event.noSwipes}")
                        event.creator != activeUser.value?.id &&
                                (activeUser.value?.id !in event.yesSwipes &&
                        activeUser.value?.id !in event.noSwipes)
                    }
                Log.d("NonUserEvents", "Fetched events: $filteredList")
                postValue(filteredList)
            }
        }

    // OSM
    private val osmAPI = OSMApi.create()
    private val osmRepository = OSMRepository(osmAPI)
    private var netTypeEvents =
        MediatorLiveData<List<Event>>().apply {
            addSource(nonUserEvents) { events ->
                val term = filterTerm.value
                var filteredEvents = events
                if (!term.isNullOrEmpty()) {
                    filteredEvents =
                        events.filter { event ->
                            event.type == term
                        }
                }
                postValue(filteredEvents)
            }
            addSource(filterTerm) { term ->
                val events = nonUserEvents.value
                var filteredEvents = events
                if (!term.isNullOrEmpty() && !events.isNullOrEmpty()){

                    val today = LocalDate.now()

                    when(term){
                        "Today" -> {
                            Log.d("Today", today.toString())
                            filteredEvents = events.filter { event ->
                                Log.d("Today", "event.date = ${event.date}")
                                val eventDate = LocalDate.parse(event.date) // Replace "event.date" with the actual property name
                                eventDate.isEqual(today)
                            }
                        }
                        "This Week" -> {
                            val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                            val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
                            filteredEvents = events.filter { event ->
                                val eventDate = LocalDate.parse(event.date) // Replace "event.date" with the actual property name
                                eventDate.isAfter(startOfWeek.minusDays(1)) && eventDate.isBefore(endOfWeek)
                            }
                        }
                        "This Month" -> {
                            val endOfMonth = today.withDayOfMonth(today.lengthOfMonth())
                            val startOfMonth = today.withDayOfMonth(1)
                            filteredEvents = events.filter { event ->
                                val eventDate = LocalDate.parse(event.date) // Replace "event.date" with the actual property name
                                eventDate.isAfter(startOfMonth.minusDays(1)) && eventDate.isBefore(endOfMonth)
                            }
                        }
                        else -> {
                            filteredEvents =
                                events.filter { event ->
                                    event.type == term
                                }
                        }
                    }

                }
                postValue(filteredEvents)
            }
        }

    private var netUserEvents = MediatorLiveData<List<Event>>().apply {
        // Respond to changes in the switch
        addSource(isMyEvents) { filterAndPostEvents() }

        // Respond to changes in the events list
        addSource(events) { filterAndPostEvents() }

        // Respond to changes in the active user
        addSource(activeUser) { filterAndPostEvents() }
    }

    private fun filterAndPostEvents() {
        val switch = isMyEvents.value ?: true // Optionally handle the default case
        val userEvents: List<Event>? = if (switch) {
            events.value?.filter { event ->
                event.creator == activeUser.value?.id
            }
        } else {
            events.value?.filter { event ->
                activeUser.value?.id in event.yesSwipes
            }
        }
        netUserEvents.postValue(userEvents)
    }

    fun getStorage(): Storage {
        return storage
    }

    fun observeUserEvents(): LiveData<List<Event>> {
        return netUserEvents
    }

    fun observeNetTypeEvents(): LiveData<List<Event>> {
        return netTypeEvents
    }

    fun observeAllEvents(): LiveData<List<Event>?> {
        return events
    }

    fun setFilter(filter: String) {
        filterTerm.value = filter
        Log.d(TAG, "Filter: $filter")
    }

    fun setEvents(switch: Boolean) {
        isMyEvents.value = switch
    }

    var interestsLiveData =
        MediatorLiveData<List<String>>().apply {
            value = listOf()
            addSource(activeUser) { user ->
                postValue(user.userInterests)
            }
        }

    /** Checks if the user has already created a profile, then sets active user
     * @param userId: String - the user's id
     * @param resultListener: (User) -> Unit - navigates to CreateUserFrag if user is invalidUser
     */
    fun setActiveUser(
        userId: String,
        resultListener: (User) -> Unit,
    ) {
        Log.d(TAG, "checking isUserInDB: $userId")
        db.fetchUserByUid(userId) { user ->
            Log.d(TAG, "isUserInDB: ${user?.id} : ${user?.displayName}")
            if (user != invalidUser) {
                Log.d(TAG, "user exists in db")
                activeUser.postValue(user)
                db.fetchEvents { eventList ->
                    events.postValue(eventList)
                }
                interestsLiveData.postValue(user?.userInterests)
            } else {
                Log.d(TAG, "user is invalid")
                activeUser.postValue(invalidUser)
            }
            if (user != null) {
                resultListener(user)
            }
        }
    }

    fun observeActiveUser(): LiveData<User> {
        return activeUser
    }

    fun getActiveUser(): User? {
        return activeUser.value
    }

    fun observeInterests(): LiveData<List<String>> {
        return interestsLiveData
    }

    fun initActionBarBinding(it: ActionBarBinding) {
        actionBarBinding = it
    }

    fun hideActionBar() {
        actionBarBinding?.root?.isGone = true
    }

    fun showActionBar() {
        actionBarBinding?.root?.isGone = false
    }

    fun updateUser(newUser: User) {
        activeUser.value = newUser
        activeUser.value?.id?.let { db.updateUser(it, newUser) }
    }

    fun fetchUserImage(
        uuid: String,
        imageView: ImageView,
    ) {
        Log.d(TAG, "fetchUserImage: $uuid")
        val path = storage.getUserPhoto(uuid)
        Log.d(TAG, "fetchUserImage: $path")
        Glide.fetch(path, imageView)
    }

    fun calculateDistanceInMiles(
        userLat: Double,
        userLon: Double,
        eventLat: Double,
        eventLon: Double,
    ): Double {
        val earthRadius = 3958.75 // in miles, change to 6371 for kilometer output

        val dLat = Math.toRadians((eventLat - userLat))
        val dLng = Math.toRadians((eventLon - userLon))

        val sinLatDeg = sin(dLat / 2)
        val sinLngDeg = sin(dLng / 2)

        val a = sinLatDeg.pow(2.0) + (sinLngDeg.pow(2.0) * cos(Math.toRadians(userLat)) * cos(Math.toRadians(eventLat)))
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    fun fetchEventImage(
        uuid: String,
        imageView: ImageView,
    ) {
        Log.d(TAG, "fetchEventImage: $uuid")
        val path = storage.getEventPhoto(uuid)
        Log.d(TAG, "fetchEventImage: $path")
        Glide.fetch(path, imageView)
    }

    fun addUser(newUser: User) {
        Log.d(TAG, "addNewUser: ${newUser.id}")
        db.createUser(newUser) { user ->
            Log.d(TAG, "addNewUser: ${user.id}")
            activeUser.postValue(user)
        }
    }

    fun addEvent(newEvent: Event) {
        db.createEvent(newEvent) {}
    }

    fun uploadImage(
        imageUri: Uri,
        collection: String,
        resultListener: (String) -> Unit,
    ) {
        Log.d(TAG, "uploadImage to $collection: $imageUri")
        viewModelScope.launch(Dispatchers.IO) {
            storage.uploadImage(imageUri, collection, resultListener)
        }
    }

    fun addEventSwipe(
        eventId: String,
        direction: Int,
    ) {
        Log.d(TAG, "addEventSwipe: $eventId, $direction")
        when (direction) {
            4 -> db.addEventSwipe(eventId, activeUser.value?.id!!, "noSwipes")
            8 -> db.addEventSwipe(eventId, activeUser.value?.id!!, "yesSwipes")
        }
    }

    fun removeEventFromView(event: Event) {
        val currentEvents = nonUserEvents.value?.toMutableList()
        currentEvents?.remove(event)
        nonUserEvents.postValue(currentEvents)
    }

    fun fetchUserByUid(
        uid: String,
        resultListener: (User?) -> Unit,
    ) {
        db.fetchUserByUid(uid) {
            resultListener(it)
        }
    }

    fun fetchUsersByIds(
        ids: List<String>,
        resultListener: (List<User>) -> Unit,
    ) {
        db.fetchUsersByIds(ids) {
            resultListener(it)
        }
    }

    fun fetchMyEvents(resultListener: (List<Event>) -> Unit) {
        Log.d(TAG, "fetchMyEvents: ${activeUser.value?.id}")
        if (activeUser.value != invalidUser && activeUser.value != null) {
            db.fetchMyEvents(activeUser.value?.id!!) {
                resultListener(it)
            }
        }
    }

    fun enterChatRoom(
        userId: String,
        otherUserId: String,
        resultListener: (Conversation) -> Unit,
    ) {
        Log.d(TAG, "enterChatRoom start")
        Log.d(TAG, "searching for chat room: $userId, $otherUserId")
        db.findChatRoom(userId, otherUserId) { conv ->
            conv?.let {
                Log.d(TAG, "chat room found ${conv.id}")
                resultListener(conv)
            } ?: run {
                Log.d(TAG, "chat room not found. Creating one.")
                createChatRoom(userId, otherUserId) { conv ->
                    resultListener(conv)
                }
            }
        }
    }

    private fun createChatRoom(
        userId: String,
        otherUserId: String,
        resultListener: (Conversation) -> Unit,
    ) {
        Log.d(TAG, "createChatRoom start")
        Log.d(TAG, "creating chat room: $userId, $otherUserId")
        db.createChatRoom(userId, otherUserId) { conv ->
            Log.d(TAG, "chat room created ${conv.id}")
            db.addConversationIDtoUser(userId, conv.id)
            db.addConversationIDtoUser(otherUserId, conv.id)
            resultListener(conv)
        }
    }

    fun updateEvent(event: Event) {
        Log.d(TAG, "updateEvent: ${event.id}")
        db.updateEvent(event)
    }

    suspend fun getEventCoords(locationName: String): OSMLocation {
        val locationCoords = osmRepository.fetchLocation(locationName)
        Log.d("OSM", "Fetched location: $userLocation")
        return locationCoords
    }

    fun fetchConversationsByUserID(
        userID: String,
        resultListener: (List<Conversation>) -> Unit,
    ) {
        db.fetchUserConvIDs(userID) { convIDs ->
            if (convIDs.isEmpty()) {
                resultListener(listOf())
            } else {
                db.fetchConvsByIDs(convIDs) { convs ->
                    resultListener(convs)
                }
            }
        }
    }

    fun fetchEventList() {
        db.fetchEvents { eventList ->
            events.postValue(eventList)
        }
    }
}
