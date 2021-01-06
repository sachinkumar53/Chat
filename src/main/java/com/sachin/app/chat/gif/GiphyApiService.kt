package com.sachin.app.chat.gif

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface GiphyApiService {

    @GET("v1/gifs/trending")
    fun getTrendingGIFs(
            @Query("api_key")
            apiKey: String,

            @Query("limit")
            limit: Int,

            @Query("offset")
            offSet: Int = 0
    ): Call<ResponseBody>


    @GET("v1/stickers/trending")
    fun getTrendingStickers(
            @Query("api_key")
            apiKey: String,

            @Query("limit")
            limit: Int,

            @Query("offset")
            offSet: Int = 0
    ): Call<ResponseBody>


    @GET("v1/gifs/search")
    fun searchGIFs(
            @Query("api_key")
            apiKey: String,

            @Query("q")
            query: String,

            @Query("limit")
            limit: Int,

            @Query("offset")
            offSet: Int = 0
    ): Call<ResponseBody>


    @GET("v1/stickers/search")
    fun searchStickers(
            @Query("api_key")
            apiKey: String,

            @Query("q")
            query: String,

            @Query("limit")
            limit: Int,

            @Query("offset")
            offSet: Int = 0
    ): Call<ResponseBody>
}