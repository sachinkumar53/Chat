package com.sachin.app.chat.model

import java.io.Serializable
import java.util.*

data class ChatMessage(
        var text: String? = null,
        var uid: String? = null,
        var type: String? = "text",
        var status: Int = 0,
        var url: String? = null,
        var data: String? = null,
        var id: String? = null,
        val time: Long = Date().time

) : Serializable