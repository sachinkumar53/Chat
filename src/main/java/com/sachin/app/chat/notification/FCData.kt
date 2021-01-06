package com.sachin.app.chat.notification

data class FCData(
        var title: String? = null,
        val icon: String? = null,
        val body: String? = null,
        val id: String? = null,
        val senderUid: String? = null,
        val receiverUid: String? = null,
        val type: String = "message"
)