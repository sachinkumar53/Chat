package com.sachin.app.chat.emoji

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.facebook.drawee.view.SimpleDraweeView
import com.sachin.app.chat.R
import com.sachin.app.chat.listener.OnItemClickListener
import com.sachin.app.chat.util.dpToPx

class AnimEmojiAdapter() : RecyclerView.Adapter<AnimEmojiAdapter.AnimEmojiViewHolder>() {
    var onItemClickListener: OnItemClickListener? = null

    inner class AnimEmojiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val emojiView: SimpleDraweeView = itemView.findViewById(R.id.emoji_view)
    }

    override fun getItemCount(): Int = AnimEmoji.emoticons.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimEmojiViewHolder =
            AnimEmojiViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.anim_emoji_item, parent, false))

    override fun onBindViewHolder(holder: AnimEmojiViewHolder, position: Int) {
        Glide.with(holder.itemView)
                .asGif()
                .override(holder.itemView.dpToPx(48))
                .load(AnimEmoji.emoticons[position])
                .into(holder.emojiView)

        holder.emojiView.setOnClickListener { if (onItemClickListener != null) onItemClickListener!!.onItemClick(position) }
    }
}