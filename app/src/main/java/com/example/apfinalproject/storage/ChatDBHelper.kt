package com.example.apfinalproject.storage

import android.util.Log
import com.example.apfinalproject.model.Conversation
import com.example.apfinalproject.model.Message
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatDBHelper {
    private val TAG = "ChatDBHelper"
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val rootCollection = "chats"

    fun fetchMessages(
        conversationID: String,
        resultListener: (List<Message>) -> Unit,
    ) {
        db.collection(rootCollection)
            .document(conversationID)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Log.d(TAG, "fetchMessages: failed")
                    resultListener(listOf())
                } else {
                    Log.d(TAG, "fetchMessages: succeeded")
                    resultListener(
                        querySnapshot!!.documents.mapNotNull {
                            it.toObject(Message::class.java)
                        },
                    )
                }
            }
    }

    fun getChatUsers(
        conversationID: String,
        resultListener: (List<String>) -> Unit,
    ) {
        db.collection(rootCollection)
            .document(conversationID)
            .get()
            .addOnSuccessListener { result ->
                val conversation = result.toObject(Conversation::class.java)
                if (conversation?.userIDs != null) {
                    Log.d(TAG, "getChatUsers: conversation is not null")
                    resultListener(conversation.userIDs)
                } else {
                    Log.d(TAG, "getChatUsers: conversation or userIDs is null")
                    resultListener(listOf())
                }
            }
            .addOnFailureListener {
                Log.d(TAG, "getChatUsers: failed")
                resultListener(listOf())
            }
    }

    fun uploadMessage(
        conversationID: String,
        message: Message,
        resultListener: (Boolean) -> Unit,
    ) {
        val senderID = message.senderID
        Log.d(TAG, "uploadMessage: started ${message.messageText} $conversationID $senderID")
        db.collection(rootCollection)
            .document(conversationID)
            .collection("messages")
            .add(message)
            .addOnSuccessListener {
                Log.d(TAG, "uploadMessage: succeeded ${it.id}")
                resultListener(true)
            }
            .addOnFailureListener {
                Log.d(TAG, "uploadMessage: failed")
                resultListener(false)
            }
    }

    fun updateConversationLastMessage(
        conversationID: String,
        message: Message,
        resultListener: (Boolean) -> Unit,
    ) {
        Log.d(TAG, "updateConversationLastMessage: started")
        db.collection(rootCollection)
            .document(conversationID)
            .update(
                mapOf(
                    "lastMessage" to message.messageText,
                    "lastMessageTimestamp" to Timestamp.now(),
                    "lastMessageSender" to message.senderID,
                ),
            )
            .addOnSuccessListener {
                Log.d(TAG, "updateConversationLastMessage: succeeded")
                resultListener(true)
            }
            .addOnFailureListener {
                Log.d(TAG, "updateConversationLastMessage: failed")
                resultListener(false)
            }
    }
}
