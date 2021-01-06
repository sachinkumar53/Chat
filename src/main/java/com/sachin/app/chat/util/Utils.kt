package com.sachin.app.chat.util

import android.text.format.DateUtils.getRelativeTimeSpanString

object Utils {

    fun getLastSeenText(time: Long): CharSequence =
            if (getRelativeTimeSpanString(time).toString().equals("0 minutes ago",true)
                    || getRelativeTimeSpanString(time).toString().equals("in 0 minutes",true))
                "moments ago"
            else getRelativeTimeSpanString(time)

    fun downloadFile(){

    }
}