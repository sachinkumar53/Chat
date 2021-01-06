package com.sachin.app.chat.listener

import android.view.View
import com.sachin.app.chat.model.ChatMessage

interface OnMessageClickListener {
    fun onMessageClick(chatMessage: ChatMessage, view: View)
    fun onMessageLongClick(chatMessage: ChatMessage, view: View)
}