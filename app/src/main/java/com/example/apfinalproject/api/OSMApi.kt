package com.example.apfinalproject.api

import com.example.apfinalproject.model.OSMLocation
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface OSMApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
    ): List<OSMLocation>

    companion object {
        var url =
            HttpUrl.Builder()
                .scheme("https")
                .host("nominatim.openstreetmap.org")
                .build()

        fun create(): OSMApi = create(url)

        private fun create(httpUrl: HttpUrl): OSMApi {
            val client =
                OkHttpClient.Builder()
                    .addInterceptor(
                        HttpLoggingInterceptor().apply {
                            this.level = HttpLoggingInterceptor.Level.BASIC
                        },
                    )
                    .build()
            return Retrofit.Builder()
                .baseUrl(httpUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OSMApi::class.java)
        }
    }
}
