package com.sachin.app.chat.notification

import com.google.gson.annotations.SerializedName

class FCMessage( //  "to" changed to token

        @SerializedName("to")
        var token: String,

        @SerializedName("data")
        var data: FCData
)