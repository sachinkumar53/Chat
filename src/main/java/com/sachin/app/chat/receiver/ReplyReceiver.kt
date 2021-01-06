package com.sachin.app.chat.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.sachin.app.chat.model.ChatMessage
import com.sachin.app.chat.util.ChatHelper

class ReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return

        val text = getMessageText(intent)?.toString()
        val userUID = intent.getStringExtra("uid1")
        val friendUID = intent.getStringExtra("uid2")
        val id = intent.getStringExtra("id")

        if (!text.isNullOrEmpty() && !text.isNullOrBlank() && friendUID != null) {
            val message = ChatMessage(text = text, uid = userUID)
            ChatHelper.sendTextMessage(friendUID, message, {
                Log.d("SAC", "Message sent successfully")
            })
        }
    }

    private fun getMessageText(intent: Intent): CharSequence? =
            RemoteInput.getResultsFromIntent(intent)?.getCharSequence("key_text_reply")
}