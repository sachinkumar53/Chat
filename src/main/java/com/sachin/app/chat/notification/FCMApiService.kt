package com.sachin.app.chat.notification

import com.sachin.app.chat.constants.Constant
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface FCMApiService {
    @Headers("Content-Type:application/json", "Authorization:key=" + Constant.SERVER_KEY)

    @POST("fcm/send")
    fun sendNotification(@Body notification: FCMessage?): Call<ResponseBody>
}