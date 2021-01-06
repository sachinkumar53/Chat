package com.sachin.app.chat.adapter

import android.content.Context
import android.graphics.Typeface
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sachin.app.chat.R
import com.sachin.app.chat.model.Conversation
import com.sachin.app.chat.util.AvatarUtil
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.profile_image_list_item.view.*
import java.text.SimpleDateFormat
import java.util.*


class ConversationAdapter(private val chatList: ArrayList<Conversation>) : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {
    private var onConversationClick: (View,Conversation) -> Unit = {_,_->}
    private var onConversationLongClick: (Conversation) -> Unit = {}

    inner class ConversationViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bind(conversation: Conversation) {

            AvatarUtil.loadAvatar(itemView, conversation.photoUrl, containerView.profile_image)

            containerView.user_name.text = conversation.senderName
            containerView.message.text = conversation.text
            containerView.message.setTypeface(containerView.message.typeface, if (!conversation.seen) Typeface.BOLD else Typeface.NORMAL)
            containerView.message_time.text = getTimeText(itemView.context, conversation.timeStamp)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder =
            ConversationViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.profile_image_list_item, parent, false))

    override fun getItemCount(): Int = chatList.size

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val conversation = chatList[position]
        holder.bind(conversation)
        holder.itemView.setOnClickListener { onConversationClick(holder.containerView.profile_image,conversation) }
        holder.itemView.setOnLongClickListener {
            onConversationLongClick(conversation)
            true
        }
    }

    fun setOnChatClickListener(onConversationClick: (View,Conversation) -> Unit) {
        this.onConversationClick = onConversationClick
    }

    fun setOnChatClickLongListener(onConversationLongClick: (Conversation) -> Unit) {
        this.onConversationLongClick = onConversationLongClick
    }

    private fun getTimeText(context: Context, time: Long): String {
        val date = Date(time)
        val text = DateFormat.getTimeFormat(context).format(date)
        return when {
            date.isToday() -> text
            date.isYesterDay() -> "Yesterday $text"
            else -> SimpleDateFormat("dd MMM yyyy").format(date).toString()
        }
    }
}

private fun Date.isToday(): Boolean = DateUtils.isToday(time)

private fun Date.isYesterDay(): Boolean {
    val c1 = Calendar.getInstance() // today
    c1.add(Calendar.DAY_OF_YEAR, -1) // yesterday

    val c2 = Calendar.getInstance()
    c2.time = this // your date

    return (c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
            && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR))
}