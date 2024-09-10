package com.example.apfinalproject.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class Conversation(
    val id: String = "null conversation",
    val userIDs: List<String> = listOf(),
    val messages: List<Message> = listOf(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Timestamp? = null,
    val lastMessageSender: String = "",
) : Parcelable

@Parcelize
data class Message(
    val senderID: String = "",
    val messageText: String = "",
    @DocumentId val id: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null,
) : Parcelable
