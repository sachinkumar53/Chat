package com.sachin.app.chat.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Conversation(
        @ColumnInfo
        var senderName: String? = null,

        @PrimaryKey
        var senderUid: String = "",

        @ColumnInfo
        var photoUrl: String? = null,

        @ColumnInfo
        var text: String? = null,

        @ColumnInfo
        var timeStamp: Long = 0L,

        @ColumnInfo
        var seen: Boolean = false
)