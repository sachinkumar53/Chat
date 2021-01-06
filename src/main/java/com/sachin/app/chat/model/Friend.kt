package com.sachin.app.chat.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Friend(

        @ColumnInfo(name = "name")
        var name: String? = null,

        @ColumnInfo(name = "email")
        var email: String? = null,

        @ColumnInfo(name = "photo_url")
        var photoUrl: String? = null,

        @ColumnInfo(name = "about")
        var about: String? = "Hey! I'm using this app",

        @PrimaryKey
        var uid: String = "",

        @ColumnInfo(name = "device_token")
        var deviceToken: String? = null,

        //@ColumnInfo(name = "room")
        var room: String? = null,

        //@ColumnInfo(name = "typing")
        var typing: Boolean? = null,

        @ColumnInfo(name = "online_status")
        var onlineStatus: Boolean = false,

        @ColumnInfo(name = "time_stamp")
        var timeStamp: Long = 0L //last active
) {


    fun toUser(): User =
            User(name, email, photoUrl, about, uid, deviceToken, room, typing, onlineStatus, timeStamp)



    companion object {

        @JvmStatic
        fun fromUser(user: User): Friend =
                Friend(
                        user.name,
                        user.email,
                        user.photoUrl,
                        user.about,
                        user.uid,
                        user.deviceToken,
                        user.room,
                        user.typing,
                        user.onlineStatus,
                        user.timeStamp
                )
    }
}