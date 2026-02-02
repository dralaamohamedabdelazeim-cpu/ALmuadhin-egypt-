package com.example.almuadhin.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AladhanApi {

    // Date format: dd-MM-yyyy (e.g., 22-01-2026)
    @GET("v1/timings/{date}")
    suspend fun timingsByCoordinates(
        @Path("date") date: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int
    ): AladhanResponse

    @GET("v1/timingsByCity/{date}")
    suspend fun timingsByCity(
        @Path("date") date: String,
        @Query("city") city: String,
        @Query("country") country: String,
        @Query("method") method: Int
    ): AladhanResponse
}
