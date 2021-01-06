package com.sachin.app.chat.adapter

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.google.firebase.auth.FirebaseAuth
import com.sachin.app.chat.R
import com.sachin.app.chat.adapter.ChatMessageAdapter.MessageViewHolder
import com.sachin.app.chat.constants.Status
import com.sachin.app.chat.constants.Status.getMessageStatus
import com.sachin.app.chat.constants.Type
import com.sachin.app.chat.listener.OnMessageClickListener
import com.sachin.app.chat.model.ChatMessage
import com.sachin.app.chat.model.Friend
import com.sachin.app.chat.util.*
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.message_audio_out.view.*
import kotlinx.android.synthetic.main.message_gif_in.view.*
import kotlinx.android.synthetic.main.message_image_in.view.*
import kotlinx.android.synthetic.main.message_image_in.view.chat_bubble_layout
import kotlinx.android.synthetic.main.message_text_in.view.avatar_view
import kotlinx.android.synthetic.main.message_text_in.view.message
import kotlinx.android.synthetic.main.message_text_in.view.time
import kotlinx.android.synthetic.main.typing_indicator.view.*
import java.util.*

class ChatMessageAdapter(private val messageList: List<ChatMessage>) : RecyclerView.Adapter<MessageViewHolder>() {
    private val currentUser = FirebaseAuth.getInstance().currentUser
    var friend: Friend? = null
    private var onMessageClickListener: OnMessageClickListener? = null

    override fun getItemViewType(position: Int): Int {
        val message = messageList[position]
        val isOut = message.uid == currentUser?.uid

        return when (message.type) {
            Type.INDICATOR -> TYPE_INDICATOR
            Type.TEXT -> if (isOut) TYPE_TEXT_OUT else TYPE_TEXT_IN
            Type.IMAGE -> if (isOut) TYPE_IMAGE_OUT else TYPE_IMAGE_IN
            Type.AUDIO -> if (isOut) TYPE_AUDIO_OUT else TYPE_AUDIO_IN
            Type.VIDEO -> if (isOut) TYPE_VIDEO_OUT else TYPE_VIDEO_IN
            Type.EMOJI -> if (isOut) TYPE_EMOJI_OUT else TYPE_EMOJI_IN
            Type.GIF -> if (isOut) TYPE_GIF_OUT else TYPE_GIF_IN
            else -> TYPE_TEXT_OUT
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_TEXT_IN -> TextMessageHolder(layoutInflater
                    .inflate(R.layout.message_text_in, parent, false))

            TYPE_TEXT_OUT -> TextMessageHolder(layoutInflater
                    .inflate(R.layout.message_text_out, parent, false))

            TYPE_IMAGE_IN -> ImageMessageHolder(layoutInflater
                    .inflate(R.layout.message_image_in, parent, false))

            TYPE_IMAGE_OUT -> ImageMessageHolder(layoutInflater
                    .inflate(R.layout.message_image_out, parent, false))

            TYPE_GIF_IN -> GIFMessageHolder(layoutInflater
                    .inflate(R.layout.message_gif_in, parent, false))

            TYPE_GIF_OUT -> GIFMessageHolder(layoutInflater
                    .inflate(R.layout.message_gif_out, parent, false))

            TYPE_AUDIO_IN -> AudioMessageHolder(layoutInflater
                    .inflate(R.layout.message_audio_in, parent, false))

            TYPE_AUDIO_OUT -> AudioMessageHolder(layoutInflater
                    .inflate(R.layout.message_audio_out, parent, false))

            TYPE_VIDEO_IN -> VideoMessageHolder(layoutInflater
                    .inflate(R.layout.message_video_in, parent, false))

            TYPE_VIDEO_OUT -> VideoMessageHolder(layoutInflater
                    .inflate(R.layout.message_video_out, parent, false))

            TYPE_INDICATOR -> IndicatorViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.typing_indicator, parent, false))
/*
            TYPE_EMOJI_IN -> EmojiViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.chat_emoji_message_layout, parent, false))*/

            else -> MessageViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.message_text_out, parent, false))
        }
    }

    override fun getItemCount(): Int = messageList.size

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]

        when (getItemViewType(position)) {
            TYPE_TEXT_IN, TYPE_TEXT_OUT -> (holder as TextMessageHolder).bind(position, message)
            TYPE_IMAGE_IN, TYPE_IMAGE_OUT -> (holder as ImageMessageHolder).bind(position, message)
            TYPE_GIF_IN, TYPE_GIF_OUT -> (holder as GIFMessageHolder).bind(position, message)
            TYPE_AUDIO_IN, TYPE_AUDIO_OUT -> (holder as AudioMessageHolder).bind(position, message)
            TYPE_VIDEO_IN, TYPE_VIDEO_OUT -> (holder as VideoMessageHolder).bind(position, message)
            TYPE_INDICATOR -> (holder as IndicatorViewHolder).updateIndicator()
        }

    }

    fun setOnMessageClickListener(onMessageClickListener: OnMessageClickListener) {
        this.onMessageClickListener = onMessageClickListener
    }

    inner class GIFMessageHolder(containerView: View) : TextMessageHolder(containerView) {

        override fun bind(position: Int, message: ChatMessage) {
            super.bind(position,message, TYPE_GIF_IN)

            Glide.with(containerView)
                    .asGif()
                    .thumbnail(0.1F)
                    .load(message.url)
                    .into(containerView.message_gif)

            containerView.chat_bubble_layout.visibility =
                    if (message.text?.equals("GIF", true) == true) GONE else VISIBLE
        }
    }

    inner class AudioMessageHolder(containerView: View) : TextMessageHolder(containerView) {

        override fun bind(position: Int, message: ChatMessage) {
            super.bind(position, message, TYPE_AUDIO_IN)

            logWarn("Data is ${message.data}")
            if (message.data != null) {
                val data = message.data!!.split("~")
                data.forEach { logWarn(it) }
                val name = data[0]
                val isLoading = data[1].toBoolean()
                val isPlaying = data[2].toBoolean()

                containerView.message_audio_loading.visibility = if (isLoading) VISIBLE else GONE
                containerView.message.text = name
                containerView.findViewById<ImageButton>(R.id.message_play_pause).apply {
                    isClickable = !isLoading
                    setImageDrawable(
                            if (isPlaying) containerView.context.getDrawable(R.drawable.ic_baseline_pause_circle_filled_24)
                            else containerView.context.getDrawable(R.drawable.ic_baseline_play_circle_filled_24)
                    )
                }
            }
        }
    }

    inner class VideoMessageHolder(containerView: View) : TextMessageHolder(containerView) {

        override fun bind(position: Int, message: ChatMessage) {
            super.bind(position, message, TYPE_VIDEO_IN)

            containerView.chat_bubble_layout.visibility =
                    if (message.text?.equals("Video", true) == true) GONE else VISIBLE

            if (message.url != null) {
                Glide.with(containerView)
                        .load(message.url)
                        .thumbnail(0.5F)
                        .override(containerView.resources.dpToPx(200))
                        .into(containerView.message_image)
                itemView.setOnClickListener {
                    onMessageClickListener?.onMessageClick(message, it)
                }
            }
        }
    }

    inner class ImageMessageHolder(containerView: View) : TextMessageHolder(containerView) {

        override fun bind(position: Int, message: ChatMessage) {
            super.bind(position, message, TYPE_IMAGE_IN)

            containerView.chat_bubble_layout.visibility =
                    if (message.text?.equals("Image", true) == true) GONE else VISIBLE

            if (message.url != null) {
                Glide.with(containerView)
                        .load(message.url)
                        .thumbnail(0.5F)
                        .override(containerView.resources.dpToPx(200))
                        .into(containerView.message_image)
            }

            containerView.message_image.setOnClickListener {
                onMessageClickListener?.onMessageClick(message, it)
            }
        }
    }

    open inner class TextMessageHolder(containerView: View) : MessageViewHolder(containerView) {

        open fun bind(position: Int, message: ChatMessage) {
            bind(position, message, TYPE_TEXT_IN)
        }

        fun bind(position: Int, message: ChatMessage, typeIn: Int) {
            val viewType = getItemViewType(position)
            val previous = position - 1

            if (viewType == typeIn) {
                containerView.avatar_view.visibility = VISIBLE

                if (friend == null || friend?.photoUrl == null)
                    Glide.with(containerView).load(R.drawable.default_avatar)
                            .into(containerView.avatar_view)
                else AvatarUtil.loadAvatar(containerView, friend!!.photoUrl, containerView.avatar_view)

                if (previous >= 0) {
                    if (messageList[previous].uid == message.uid) {
                        containerView.avatar_view.visibility = INVISIBLE
                    }
                }
            }

            val time = DateFormat.getTimeFormat(itemView.context).format(Date(message.time))
            containerView.time.show()

            when {
                position == messageList.preLastPosition() -> {
                    containerView.time.text = if (viewType == typeIn) time
                    else containerView.resources.getString(R.string.status_time_format, getMessageStatus(message.status), time)
                }

                position < messageList.preLastPosition() -> {
                    if (viewType != typeIn && (message.status == Status.SENDING ||
                                    message.status == Status.UPLOADING || message.status == Status.FAILED))
                        containerView.time.text = containerView.resources.getString(R.string.status_time_format, getMessageStatus(message.status), time)
                    else containerView.time.hide(true)
                }
                else -> containerView.time.hide(true)
            }
            /*if (position <= messageList.preLastPosition()) {

            } else*/

            containerView.findViewById<ConstraintLayout>(R.id.message_layout).layoutParams.apply {
                (this as RecyclerView.LayoutParams)
                topMargin = containerView.dpToPx(4)
            }

            //setup background
            containerView.findViewById<View>(R.id.chat_bubble_layout).setBackgroundResource(
                    if (viewType == typeIn) R.drawable.chat_bubble_in
                    else R.drawable.chat_bubble_out
            )

            if (previous >= 0) {
                if (messageList[previous].uid == message.uid) {
                    //message from same user
                    containerView.findViewById<View>(R.id.chat_bubble_layout).setBackgroundResource(
                            if (viewType == typeIn) R.drawable.chat_bubble_in_2 else R.drawable.chat_bubble_out_2
                    )

                    containerView.findViewById<ConstraintLayout>(R.id.message_layout).layoutParams.apply {
                        (this as RecyclerView.LayoutParams)
                        topMargin = 0
                    }
                }
            }

            //end setup background

            containerView.message.text = message.text

            containerView.setOnLongClickListener {
                onMessageClickListener?.onMessageLongClick(message, containerView)
                true
            }
        }
    }


    inner class IndicatorViewHolder(override val containerView: View) : MessageViewHolder(containerView) {

        fun updateIndicator() {
            if (friend == null) return

            AvatarUtil.loadAvatar(containerView, friend!!.photoUrl, containerView.room_indicator)

            containerView.room_indicator.alpha =
                    if (friend?.room == currentUser?.uid &&
                            friend?.onlineStatus == true) 1F else 0.4F

            containerView.typing_indicator.visibility = if (friend?.typing == true) VISIBLE else GONE
        }
    }

/*

    inner class EmojiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chatContainer: FrameLayout = itemView.findViewById(R.id.chat_container)
        private val chatLayout: LinearLayout = itemView.findViewById(R.id.chat_layout)
        private val imageViewEmoji: ImageView = itemView.findViewById(R.id.message_emoji)
        private val textViewTime: TextView = itemView.findViewById(R.id.message_time)
        private val textViewSeen: TextView = itemView.findViewById(R.id.message_seen)

        fun bind(position: Int, message: ChatMessage) {
            val context = itemView.context

            with(message.uid == currentUser?.uid, {
                chatLayout.layoutParams.apply {
                    (this as FrameLayout.LayoutParams).gravity = if (this@with) END else START
                }

                chatLayout.gravity = if (this) END else START

                textViewTime.setTextColor(
                        if (this) context.getColor(R.color.message_text_color_secondary_out)
                        else context.getColor(R.color.message_text_color_secondary_in))

                if ((position - 1) >= 0) {
                    if (messageList[(position - 1)].uid == message.uid) {
                        chatContainer.layoutParams.apply {
                            (this as RecyclerView.LayoutParams)
                            topMargin = 0
                        }
                    }
                }
            })

            if (message.url != null) {
                val id = message.url!!.toInt()
                Glide.with(itemView).asGif().load(id).into(imageViewEmoji)
            }

            textViewTime.text = DateFormat.getTimeFormat(itemView.context).format(Date(message.time))

            if (position == (messageList.size - 1) && message.uid == currentUser?.uid) {
                textViewSeen.visibility = VISIBLE
                textViewSeen.text = when (message.status) {
                    STATUS_SENT -> "Sent"
                    STATUS_DELIVERED -> "Delivered"
                    STATUS_SEEN -> "Seen"
                    else -> ""
                }
            } else textViewSeen.visibility = GONE

            itemView.setOnLongClickListener {
                onMessageClickListener?.onMessageLongClick(message)
                true
            }
        }
    }
*/


    open inner class MessageViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer

    companion object {
        private const val TYPE_TEXT_IN = 0
        private const val TYPE_TEXT_OUT = 1

        private const val TYPE_IMAGE_IN = 2
        private const val TYPE_IMAGE_OUT = 3

        private const val TYPE_EMOJI_IN = 4
        private const val TYPE_EMOJI_OUT = 5

        private const val TYPE_INDICATOR = 6

        private const val TYPE_VIDEO_IN = 7
        private const val TYPE_VIDEO_OUT = 8

        private const val TYPE_AUDIO_IN = 9
        private const val TYPE_AUDIO_OUT = 10

        private const val TYPE_GIF_IN = 11
        private const val TYPE_GIF_OUT = 12
    }
}

private fun <E> List<E>.preLastPosition(): Int = this.size - 2
