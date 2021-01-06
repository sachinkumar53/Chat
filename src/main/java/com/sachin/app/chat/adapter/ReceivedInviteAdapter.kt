package com.sachin.app.chat.adapter

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.sachin.app.chat.R
import com.sachin.app.chat.adapter.ReceivedInviteAdapter.ReceivedInviteHolder
import com.sachin.app.chat.model.ReceivedInvite
import com.sachin.app.chat.util.AvatarUtil
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.invite_list_item_received.view.*

class ReceivedInviteAdapter(private val invites: ArrayList<ReceivedInvite>) : RecyclerView.Adapter<ReceivedInviteHolder>() {
    private var onInviteAcceptClickListener: (ReceivedInvite) -> Unit = {}
    private var onInviteRejectClickListener: (ReceivedInvite) -> Unit = {}


    inner class ReceivedInviteHolder(override val containerView: View) : ViewHolder(containerView), LayoutContainer {

        fun bind(invite: ReceivedInvite) {
            containerView.invite_user_name.text = invite.user?.name
            containerView.invite_user_status.text = invite.user?.about
            invite.user?.let { AvatarUtil.loadAvatar(containerView, it.photoUrl, containerView.invite_user_profile_img) }

            invite.time?.let {
                containerView.invite_time.text = DateUtils.getRelativeTimeSpanString(containerView.context, it)
            }

            containerView.invite_accept.setOnClickListener { onInviteAcceptClickListener(invite) }
            containerView.invite_reject.setOnClickListener { onInviteRejectClickListener(invite) }
        }
    }

    fun setOnInviteAcceptListener(onInviteAcceptClickListener: (ReceivedInvite) -> Unit) {
        this.onInviteAcceptClickListener = onInviteAcceptClickListener
    }

    fun setOnInviteRejectListener(onInviteRejectClickListener: (ReceivedInvite) -> Unit) {
        this.onInviteRejectClickListener = onInviteRejectClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceivedInviteHolder =
            ReceivedInviteHolder(LayoutInflater.from(parent.context).inflate(R.layout.invite_list_item_received, parent, false))

    override fun getItemCount(): Int = invites.size

    override fun onBindViewHolder(holder: ReceivedInviteHolder, position: Int) = holder.bind(invites[position])
}