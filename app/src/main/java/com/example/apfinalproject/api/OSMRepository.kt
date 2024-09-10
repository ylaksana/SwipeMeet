package com.example.apfinalproject.api

import com.example.apfinalproject.model.OSMLocation

class OSMRepository(private val api: OSMApi) {
    suspend fun fetchLocation(location: String): OSMLocation {
        return api.search(location).firstOrNull() ?: OSMLocation(0, 0.0, 0.0, "null") // Provide a default OSMLocation here
    }
}
