package com.sachin.app.chat.notification

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object FCMApiClient {
    private const val BASE_URL = "https://fcm.googleapis.com/"
    private var retrofit: Retrofit? = null

    val client: Retrofit?
        get() {
            if (retrofit == null) {
                retrofit = Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
            }
            return retrofit
        }
}