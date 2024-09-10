package com.example.apfinalproject.model

import com.google.gson.annotations.SerializedName

data class OSMLocation(
    @SerializedName("place_id")
    val placeId: Long,
    @SerializedName("lat")
    val latitude: Double,
    @SerializedName("lon")
    val longitude: Double,
    @SerializedName("display_name")
    val displayName: String,
)
