package com.sachin.app.chat.ui

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.format.DateFormat
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.sachin.app.chat.R
import com.sachin.app.chat.model.ChatMessage
import com.sachin.app.chat.model.User
import com.sachin.app.chat.util.AvatarUtil
import com.sachin.app.chat.util.FirebaseUtils
import kotlinx.android.synthetic.main.activity_image_viewer.*
import java.util.*


class ImageViewerActivity : AppCompatActivity() {
    private var isFullScreen = false
    private lateinit var chatMessage: ChatMessage
    private lateinit var eventListener: ValueEventListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        if (intent == null || intent.extras == null ||
                !intent.hasExtra("message") || intent.getSerializableExtra("message") == null) {
            finish()
            return
        }

        chatMessage = intent.getSerializableExtra("message") as ChatMessage

        setSupportActionBar(toolbar)
        addInfoToToolbar()

        Glide.with(this).load(chatMessage.url).into(object : CustomTarget<Drawable>() {
            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                imageView.setImageDrawable(resource)
            }

            override fun onLoadCleared(placeholder: Drawable?) {}
        })

        imageView.setOnClickListener { toggleFullscreen() }
    }

    private fun addInfoToToolbar() {
        eventListener = object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                val user = p0.getValue(User::class.java)
                AvatarUtil.loadAvatar(this@ImageViewerActivity, user!!.photoUrl, toolbar_profile_image)
                toolbar_user_name.text = user.name
            }

            override fun onCancelled(p0: DatabaseError) {}
        }

        FirebaseUtils.userReference.child(chatMessage.uid!!).addValueEventListener(eventListener)
        toolbar_user_status.text = DateFormat.getTimeFormat(this).format(Date(chatMessage.time))
    }

    override fun onDestroy() {
        FirebaseUtils.userReference.child(chatMessage.uid!!).removeEventListener(eventListener)
        super.onDestroy()
    }

    private fun toggleFullscreen() {
        if (isFullScreen) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            supportActionBar?.show()
            isFullScreen = false
        } else {
            supportActionBar?.hide()
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            isFullScreen = true
        }
    }
}
