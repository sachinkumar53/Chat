package com.sachin.app.chat.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

data class User(
        var name: String? = null,
        var email: String? = null,
        var photoUrl: String? = null,
        var about: String? = "Hey! I'm using this app",
        var uid: String = "",
        var deviceToken: String? = null,
        var room: String? = null,
        var typing: Boolean? = null,
        var onlineStatus: Boolean = false,
        var timeStamp: Long = 0L //last active
) : Serializable