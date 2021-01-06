package com.sachin.app.chat.gif

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object GiphyApiClient {
    private const val BASE_URL_GIPHY = "http://api.giphy.com/"
    private var retrofit: Retrofit? = null

    @JvmStatic
    fun getGIFClient(context: Context): Retrofit {
        if (retrofit == null) {
            retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL_GIPHY)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(OkHttpClient.Builder()
                            .addNetworkInterceptor(CacheInterceptor(context, 15))
                            .cache(CacheInterceptor.getCache(context))
                            .build()
                    ).build()
        }

        return retrofit!!
    }

}