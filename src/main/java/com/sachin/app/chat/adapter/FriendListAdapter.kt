package com.sachin.app.chat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sachin.app.chat.R
import com.sachin.app.chat.model.Friend
import com.sachin.app.chat.util.AvatarUtil
import com.sachin.app.chat.util.Utils
import com.sachin.app.chat.util.hide
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.profile_image_list_item.view.*


class FriendListAdapter : RecyclerView.Adapter<FriendListAdapter.FriendViewHolder>() {
    private val friendList = arrayListOf<Friend>()
    private var onFriendClick: (friend: Friend) -> Unit = {}

    fun setFriends(list: List<Friend>) {
        friendList.clear()
        friendList.addAll(list)
        notifyDataSetChanged()
    }

    inner class FriendViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bind(friend: Friend) {

            AvatarUtil.loadAvatar(itemView, friend.photoUrl, containerView.profile_image)
            containerView.user_name.text = friend.name
            containerView.message.text = friend.about
            containerView.message_time.hide(true)
            itemView.setOnClickListener { onFriendClick(friend) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder =
            FriendViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.profile_image_list_item, parent, false))

    override fun getItemCount(): Int = friendList.size

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val user = friendList[position]
        holder.bind(user)
    }

    fun setOnFriendClickListener(onFriendClick: (friend: Friend) -> Unit) {
        this.onFriendClick = onFriendClick
    }
}