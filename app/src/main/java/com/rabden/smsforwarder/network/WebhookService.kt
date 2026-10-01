package com.rabden.smsforwarder.network

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Url

data class WebhookPayload(
    @SerializedName("sender") val sender: String,
    @SerializedName("message") val message: String,
    @SerializedName("device") val device: String
)

interface WebhookService {
    @POST
    suspend fun postRaw(
        @Url url: String,
        @Body payload: WebhookPayload,
        @HeaderMap headers: Map<String, String>
    ): retrofit2.Response<Unit>

    companion object {
        fun create(): WebhookService {
            val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .build()

            return Retrofit.Builder()
                .baseUrl("https://placeholder.com/") // baseUrl is required but overridden by @Url
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WebhookService::class.java)
        }
    }
}
