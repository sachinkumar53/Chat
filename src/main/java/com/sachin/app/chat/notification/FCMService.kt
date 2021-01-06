package com.sachin.app.chat.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.DEFAULT_ALL
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sachin.app.chat.R
import com.sachin.app.chat.constants.Status
import com.sachin.app.chat.receiver.ReplyReceiver
import com.sachin.app.chat.ui.ChatActivity
import com.sachin.app.chat.constants.Constant
import com.sachin.app.chat.util.logError


class FCMService : FirebaseMessagingService() {
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        FirebaseDatabase.getInstance().reference.child(currentUser.uid)
                .child("deviceToken").setValue(token)
                .addOnCompleteListener {
                    if (it.isSuccessful) Log.d(TAG, "Device token successfully updated")
                    else Log.e(TAG, "Failed to add device token", it.exception)
                }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.w("SACHIN", "Message received ${remoteMessage.data}")

        createNotificationChannel()

        val data = remoteMessage.data
        val name = data["name"]
        val type = data["type"]
        val uid = data["uid"]

        val senderUid = data["senderUid"]
        val receiverUid = data["receiverUid"]

        val id = data["id"]
        val title = data["title"] ?: "New message"
        val body = data["body"] ?: ""
        val icon = data["icon"]

        if (type == "message" && senderUid != null && receiverUid != null && id != null) {
            FirebaseDatabase.getInstance().reference
                    .child("messages")
                    .child(senderUid)
                    .child(receiverUid)
                    .child(id)
                    .child("status")
                    .setValue(Status.DELIVERED)
                    .addOnFailureListener { logError("Set delivered message error", it) }

            if (icon != null) {
                Glide.with(this)
                        .asBitmap()
                        .load(icon)
                        .circleCrop()
                        .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        notifyMessage(title, body, resource, senderUid, receiverUid, id)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
            } else notifyMessage(title, body, null, senderUid, receiverUid, id)
        } else {
            if (icon != null) {
                Glide.with(this).asBitmap().load(icon).transform(CircleCrop()).into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        notify(title, body, senderUid, type, resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
            } else notify(title, body, senderUid, type, null)
        }
    }

    private fun notify(title: String, body: String, uid: String?, type: String?, icon: Bitmap?) {

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(icon)
                .setContentTitle(title)
                .setContentText(body)
                .setDefaults(DEFAULT_ALL)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (type == "message") {
            val inboxStyle = NotificationCompat.InboxStyle()
            inboxStyle.setBigContentTitle(title)
            //inboxStyle.setSummaryText("$count new messages")
            /*messages.forEach {
                logWarn("Added line $it")
                inboxStyle.addLine(it)
            }*/
            builder.setStyle(inboxStyle)
        }

        uid?.let {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra(Constant.USER_UID, it)

            val pendingIntent = PendingIntent.getActivity(this, 7884, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            builder.setContentIntent(pendingIntent)
        }

        notificationManager.notify(458, builder.build())
    }


    //private var count = 0

    private fun notifyMessage(title: String, body: String, icon: Bitmap?, uid1: String, uid2: String, id: String) {
        var messages: ArrayList<String>? = notificationMap.get(uid1)
        if (messages == null) {
            messages = arrayListOf(body)
            notificationMap[uid1] = messages
        }

        prevUID = uid1

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_chat)
                .setLargeIcon(icon)
                .setContentTitle(title)
                .setContentText(body)
                .setColor(getColor(R.color.material_light_blue_500))
                //.setDefaults(DEFAULT_ALL)
                .setAutoCancel(true)
                //.setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(Uri.parse("android.resource://" + applicationContext.packageName + "/" + R.raw.pristine))


        if (messages.size > 1) {
            val inboxStyle = NotificationCompat.InboxStyle()
            inboxStyle.setBigContentTitle(title)
            inboxStyle.setSummaryText(getString(R.string.notification_inbox_style_message_summary, messages.size))
            builder.setContentText(getString(R.string.notification_inbox_style_message_text, messages.size))
            messages.forEach { inboxStyle.addLine(it) }
            builder.setStyle(inboxStyle)
        }

        val remoteInput = RemoteInput.Builder("key_text_reply").run {
            setLabel(getString(R.string.notification_action_reply_hint_text))
            build()
        }

        val replyPendingIntent = PendingIntent.getBroadcast(applicationContext, 7889,
                getReplyIntent(uid1, uid2, id), PendingIntent.FLAG_UPDATE_CURRENT)

        // Create the reply action and add the remote input.
        val action = NotificationCompat.Action.Builder(R.drawable.ic_baseline_send_24, getString(R.string.notification_action_reply), replyPendingIntent)
                        .addRemoteInput(remoteInput)
                        .build()

        builder.addAction(action)

        val intent = Intent(this, ChatActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.putExtra(Constant.USER_UID, uid1)

        val pendingIntent = PendingIntent.getActivity(this, 7884, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(pendingIntent)

        val default = System.currentTimeMillis().toInt()
        val notId = NOTIFICATION_IDS[uid1] ?: default

        if (!NOTIFICATION_IDS.containsKey(uid1))
            NOTIFICATION_IDS[uid1] = default

        NotificationManagerCompat.from(this).notify(notId, builder.build())
    }

    private fun getReplyIntent(userUID: String, friendUID: String, id: String): Intent {
        val replyIntent = Intent(this, ReplyReceiver::class.java)
        replyIntent.putExtra("uid1", userUID)
        replyIntent.putExtra("uid2", friendUID)
        replyIntent.putExtra("id", id)
        return replyIntent
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = CHANNEL_DESC }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    companion object {
        private val TAG = FCMService::class.java.simpleName
        private const val CHANNEL_ID = "fcm_notification_channel"
        private const val CHANNEL_NAME = "fcm_channel"
        private const val CHANNEL_DESC = "Cloud messaging channel for notification"

        private val NOTIFICATION_IDS = hashMapOf<String, Int>()
        private var prevUID = ""
        val notificationMap = linkedMapOf<String, ArrayList<String>>()
    }
}