package com.example.data.remote

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class IncidentWebhookPayload(
    val incidentId: String,
    val userId: String,
    val bloodType: String,
    val vehicleType: String,
    val department: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val severity: String,
    val minimumFeeGtq: Double,
    val timestampUtcMillis: Long
)

interface CloudSyncApiService {
    @POST
    suspend fun pushIncidentWebhook(
        @Url fullHttpsUrl: String,
        @Body payload: IncidentWebhookPayload
    ): Response<Unit>

    companion object {
        fun create(): CloudSyncApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl("https://api.vidaqr.gt/")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(CloudSyncApiService::class.java)
        }
    }
}
