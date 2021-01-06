package com.sachin.app.chat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.sachin.app.chat.R
import com.sachin.app.chat.adapter.SentInviteAdapter.SentInviteHolder
import com.sachin.app.chat.model.SentInvite
import com.sachin.app.chat.util.AvatarUtil
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.invite_list_item_sent.view.*

class SentInviteAdapter(private val invites: ArrayList<SentInvite>) : RecyclerView.Adapter<SentInviteHolder>() {
    private var onInviteSentClickListener: (Int) -> Unit = {}


    inner class SentInviteHolder(override val containerView: View) : ViewHolder(containerView), LayoutContainer {

        fun bind(sentInvite: SentInvite) {
            containerView.invite_user_name.text = sentInvite.user?.name
            containerView.invite_user_status.text = sentInvite.user?.about
            sentInvite.user?.let { AvatarUtil.loadAvatar(containerView, it.photoUrl, containerView.invite_user_profile_img) }
            containerView.invite_button.text = if (sentInvite.isSent) "Cancel" else "Invite"
        }
    }

    fun setOnInviteSentClickListener(onInviteSentClickListener: (Int) -> Unit) {
        this.onInviteSentClickListener = onInviteSentClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SentInviteHolder =
            SentInviteHolder(LayoutInflater.from(parent.context).inflate(R.layout.invite_list_item_sent, parent, false))

    override fun getItemCount(): Int = invites.size

    override fun onBindViewHolder(holder: SentInviteHolder, position: Int) {
        holder.bind(invites[position])
        holder.containerView.invite_button.setOnClickListener { onInviteSentClickListener(position) }
    }
}