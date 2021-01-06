package com.sachin.app.chat.constants

object Status {
    const val SENDING = 0
    const val SENT = 1
    const val DELIVERED = 2
    const val SEEN = 3
    const val FAILED = -1
    const val UPLOADING = 4

    fun getMessageStatus(status: Int): String =
            when (status) {
                SEEN -> "Seen"
                SENT -> "Sent"
                FAILED -> "Failed"
                DELIVERED -> "Delivered"
                SENDING -> "Sending"
                UPLOADING -> "Uploading"
                else -> ""
            }
}