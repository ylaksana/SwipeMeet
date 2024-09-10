package com.example.apfinalproject.storage

import android.util.Log
import com.example.apfinalproject.model.Conversation
import com.example.apfinalproject.model.Event
import com.example.apfinalproject.model.User
import com.example.apfinalproject.model.invalidUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class ViewModelDBHelper {
    companion object {
        private const val TAG = "ViewModelDBHelper"
    }

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val queryLimit: Long = 100

    private fun generateUUID(): String {
        return UUID.randomUUID().toString()
    }

    /**
     // Fetches user by uid from "users" collection and converts it to a User object
     // Returns null if user is not found or if there is an error
     */
    fun fetchUserByUid(
        uid: String,
        resultListener: (User?) -> Unit,
    ) {
        Log.d(TAG, "fetchUserByUid started for $uid")
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "fetchUserByUid succeeded for $uid")
                val user = result.toObject(User::class.java)
                Log.d(TAG, "fetchUserByUid: ${user?.id}")
                // NB: This is done on a background thread

                if (user == null) {
                    Log.d(TAG, "fetchUserByUid: user is null, doesn't not exist in db")
                    resultListener(invalidUser)
                } else {
                    Log.d(TAG, "fetchUserByUid: user is not null, exists in db")
                    resultListener(user)
                }
            }
            .addOnFailureListener {
                Log.d(TAG, "fetchUserByUid: query failed", it)
                resultListener(invalidUser)
            }
    }

    fun createUser(
        user: User,
        resultListener: (User) -> Unit,
    ) {
        Log.d(TAG, "createUser started")
        db.collection("users")
            .document(user.id)
            .set(user)
            .addOnSuccessListener {
                Log.d(TAG, "createUser succeeded")
                resultListener(user)
            }
            .addOnFailureListener {
                Log.d(TAG, "createUser failed", it)
                resultListener(invalidUser)
            }
    }

    fun createEvent(
        event: Event,
        resultListener: () -> Unit,
    ) {
        Log.d(TAG, "createEvent started")
        val eventId = generateUUID()
        event.id = eventId

        db.collection("events")
            .document(eventId)
            .set(event)
            .addOnSuccessListener {
                Log.d(TAG, "createEvent succeeded")
                resultListener()
            }
            .addOnFailureListener {
                Log.d(TAG, "createEvent failed", it)
                resultListener()
            }
    }

    fun fetchEvents(resultListener: (List<Event>) -> Unit) {
        Log.d(TAG, "fetchEvents started")
        val query = db.collection("events")
        Log.d(TAG, "query: events")
        query
            .limit(queryLimit)
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "events fetch ${result!!.documents.size}")
                // NB: This is done on a background thread
                resultListener(
                    result.documents.mapNotNull {
                        it.toObject(Event::class.java)
                    },
                )
            }
            .addOnFailureListener {
                Log.d(TAG, "events fetch FAILED ", it)
                resultListener(listOf())
            }
    }

    fun fetchUsersByIds(
        ids: List<String>,
        resultListener: (List<User>) -> Unit,
    ) {
        Log.d(TAG, "fetchUserByList started")
        val query = db.collection("users").whereIn("id", ids)
        Log.d(TAG, "query: users")
        query
            .limit(queryLimit)
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "users fetch ${result!!.documents.size}")
                // NB: This is done on a background thread
                resultListener(
                    result.documents.mapNotNull {
                        it.toObject(User::class.java)
                    },
                )
            }
            .addOnFailureListener {
                Log.d(TAG, "users fetch FAILED ", it)
                resultListener(listOf())
            }
    }

    fun updateUser(
        userID: String,
        newUser: User,
    ) {
        Log.d(TAG, "updateUser started")
        db.collection("users")
            .document(userID)
            .set(newUser)
            .addOnSuccessListener {
                Log.d(TAG, "updateUser succeeded")
            }
            .addOnFailureListener {
                Log.d(TAG, "updateUser failed", it)
            }
    }

    fun addEventSwipe(
        eventId: String,
        userId: String,
        direction: String,
    ) {
        Log.d(TAG, "addEventSwipe started for $eventId, $userId, $direction")
        db.collection("events")
            .document(eventId)
            .update(direction, FieldValue.arrayUnion(userId))
            .addOnSuccessListener {
                Log.d(TAG, "addEventSwipe succeeded")
            }
            .addOnFailureListener {
                Log.d(TAG, "addEventSwipe failed", it)
            }
    }

    fun fetchMyEvents(
        userId: String,
        resultListener: (List<Event>) -> Unit,
    ) {
        Log.d(TAG, "fetchMyEvents started")
        val query = db.collection("events").whereEqualTo("creator", userId)
        Log.d(TAG, "query: $query")
        query
            .limit(queryLimit)
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "events fetch enter")
                Log.d(TAG, "events fetch ${result?.documents?.size}")
                // NB: This is done on a background thread
                resultListener(
                    result.documents.mapNotNull {
                        it.toObject(Event::class.java)
                    },
                )
            }
            .addOnFailureListener {
                Log.d(TAG, "events fetch FAILED ", it)
                resultListener(listOf())
            }
    }

    fun findChatRoom(
        userId: String,
        otherUserId: String,
        resultListener: (Conversation?) -> Unit,
    ) {
        Log.d(TAG, "findChatRoom started")
        val chatRoomRef = db.collection("chats")
        // this is weird but need to do it this way in order to match a list in a list
        val userIDs = listOf(userId, otherUserId).sorted()

        Log.d(TAG, "query: chatRooms $userIDs")

        Log.d(TAG, "query: chatRooms")
        chatRoomRef
            .whereEqualTo("userIDs", userIDs)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "chatRooms fetch ${result!!.documents.size}")
                // NB: This is done on a background thread
                if (result.documents.isNotEmpty()) {
                    Log.d(TAG, "chatRooms fetch: chatRoom found ${result.documents[0].id}")
                    result.documents[0].toObject(Conversation::class.java)?.let {
                        resultListener(it)
                    }
                } else {
                    Log.d(TAG, "chatRooms fetch: chatRoom not found")
                    resultListener(null)
                }
            }
            .addOnFailureListener {
                Log.d(TAG, "chatRooms fetch FAILED ", it)
            }
    }

    fun createChatRoom(
        userId: String,
        otherUserId: String,
        resultListener: (Conversation) -> Unit,
    ) {
        Log.d(TAG, "createChatRoom started")
        val chatRoomRef = db.collection("chats")
        val userIDs = listOf(userId, otherUserId).sorted()
        val roomID = generateUUID()
        val newChatRoom = Conversation(roomID, userIDs)

        Log.d(TAG, "query: chatRooms")
        chatRoomRef
            .document(roomID)
            .set(newChatRoom)
            .addOnSuccessListener { _ ->
                Log.d(TAG, "chatRooms create succeeded")
                // NB: This is done on a background thread
                resultListener(newChatRoom)
            }
            .addOnFailureListener {
                Log.d(TAG, "chatRooms create FAILED ", it)
            }
    }

    fun addConversationIDtoUser(
        userId: String,
        conversationID: String,
    ) {
        Log.d(TAG, "addConversationIDtoUser started")
        db.collection("users")
            .document(userId)
            .update("conversationIDs", FieldValue.arrayUnion(conversationID))
            .addOnSuccessListener {
                Log.d(TAG, "addConversationIDtoUser succeeded")
            }
            .addOnFailureListener {
                Log.d(TAG, "addConversationIDtoUser failed", it)
            }
    }

    fun updateEvent(event: Event) {
        Log.d(TAG, "updateEvent started")
        db.collection("events")
            .document(event.id)
            .set(event)
            .addOnSuccessListener {
                Log.d(TAG, "updateEvent succeeded")
            }
            .addOnFailureListener {
                Log.d(TAG, "updateEvent failed", it)
            }
    }

    fun fetchUserConvIDs(
        userId: String,
        resultListener: (List<String>) -> Unit,
    ) {
        Log.d(TAG, "fetchUserConvIDs started")
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                Log.d(TAG, "fetchUserConvIDs succeeded")
                val user = document.toObject(User::class.java)
                if (user != null) {
                    Log.d(TAG, "fetchUserConvIDs: user found. id size: ${user.conversationIDs.size}")
                    resultListener(user.conversationIDs)
                } else {
                    Log.d(TAG, "fetchUserConvIDs: user is null")
                    resultListener(listOf())
                }
            }
            .addOnFailureListener {
                Log.d(TAG, "fetchUserConvIDs failed", it)
                resultListener(listOf())
            }
    }

    fun fetchConvsByIDs(
        convIDs: List<String>,
        resultListener: (List<Conversation>) -> Unit,
    ) {
        Log.d(TAG, "fetchConvByIDs started")
        db.collection("chats")
            .whereIn("id", convIDs)
            .get()
            .addOnSuccessListener { result ->
                Log.d(TAG, "fetchConvByIDs succeeded")
                resultListener(
                    result.documents.mapNotNull {
                        it.toObject(Conversation::class.java)
                    },
                )
            }
            .addOnFailureListener {
                Log.d(TAG, "fetchConvByIDs failed", it)
                resultListener(listOf())
            }
    }
}
