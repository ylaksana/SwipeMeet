package com.example.apfinalproject.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class Event(
    var id: String = "-1",
    var title: String = "No Title",
    var description: String = "No Description",
    var date: String = "No Date",
    var time: String = "No Time",
    var location: String = "No Location",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var creator: String = "No Creator",
    var type: String = "No Type",
    var imageName: String = "default.jpg",
    var yesSwipes: List<String> = listOf(),
    var noSwipes: List<String> = listOf(),
) : Parcelable {
    // Events with the same ID are considered equal.
    // Not sure if this is the best way to do it, but it works for now.
    override fun equals(other: Any?): Boolean =
        if (other is Event) {
            id == other.id
        } else {
            false
        }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + date.hashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + location.hashCode()
        result = 31 * result + creator.hashCode()
        return result
    }
}
