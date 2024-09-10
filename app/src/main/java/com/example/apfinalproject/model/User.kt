package com.example.apfinalproject.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class User(
    private val nullableName: String? = null,
    private val nullableEmail: String? = null,
    val id: String = "-1",
    var firstName: String = "Unknown",
    var lastName: String = "User",
    var bio: String = "Empty Bio",
    var profileImage: String = "default",
    var conversationIDs: List<String> = listOf(),
    var userInterests: List<String> = listOf(),
    var pastEvents: List<String> = listOf(),
) : Parcelable {
    val displayName: String = nullableName ?: "User logged out"
    val email: String = nullableEmail ?: "User logged out"

    fun copy(
        firstName: String = this.firstName,
        lastName: String = this.lastName,
        bio: String = this.bio,
        profileImage: String = this.profileImage,
        userInterests: List<String> = this.userInterests,
        pastEvents: List<String> = this.pastEvents,
        conversationIDs: List<String> = this.conversationIDs,
    ): User {
        val newUser = User(this.displayName, this.email, this.id)
        newUser.firstName = firstName
        newUser.lastName = lastName
        newUser.bio = bio
        newUser.profileImage = profileImage
        newUser.userInterests = userInterests
        newUser.pastEvents = pastEvents
        newUser.conversationIDs = conversationIDs
        return newUser
    }
}

const val invalidUserUid = "-1"
val invalidUser = User(null, null, invalidUserUid)
