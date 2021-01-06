package com.sachin.app.chat.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.sachin.app.chat.R
import com.sachin.app.chat.adapter.FriendListAdapter
import com.sachin.app.chat.adapter.SentInviteAdapter
import com.sachin.app.chat.constants.Constant
import com.sachin.app.chat.database.AppDatabase
import com.sachin.app.chat.model.SentInvite
import com.sachin.app.chat.model.User
import com.sachin.app.chat.util.*
import com.sachin.app.chat.widget.GradientProgress
import kotlinx.android.synthetic.main.fragment_friend_list.*

class FriendListFragment : Fragment(){
    private val currentUser by lazy { FirebaseAuth.getInstance().currentUser!! }
    private val friendListAdapter by lazy { FriendListAdapter() }
    private val userReference by lazy { FirebaseDatabase.getInstance().reference.child("users") }
    private val inviteReference by lazy { FirebaseDatabase.getInstance().reference.child("invites") }
    private val inviteList = ArrayList<SentInvite>()
    private val sentInviteAdapter by lazy { SentInviteAdapter(inviteList) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_friend_list, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        users_recyclerView.run {
            layoutManager = LinearLayoutManager(context)
            adapter = friendListAdapter
        }

        friendListAdapter.setOnFriendClickListener {
            startActivity(Intent(activity, ChatActivity::class.java).apply {
                putExtra(Constant.USER_UID, it.uid)
            })
        }

        sentInviteAdapter.setOnInviteSentClickListener {
            val inviteSent = inviteList[it]

            if (inviteSent.isSent) cancelInvite(inviteSent)
            else sendInvite(inviteSent)
        }

        search_user_edit_text.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                users_recyclerView.adapter = friendListAdapter
            } else {
                users_recyclerView.adapter = sentInviteAdapter
            }
        }
        user_progress_bar.indeterminateDrawable = GradientProgress(resources.dpToPx(300).toFloat()).apply {
            color1 = requireContext().getColor(R.color.gradient_color_1)
            color2 = requireContext().getColor(R.color.gradient_color_2)
        }

        search_user_edit_text.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (search_user_edit_text.text.isNullOrBlank() || search_user_edit_text.text.isNullOrBlank()) {
                    showToast("Enter email to search")
                    return@setOnEditorActionListener false
                }
                searchUser(search_user_edit_text.text!!.trim().toString())
            }
            false
        }
    }

    private fun searchUser(email: String): Boolean {
        switchToProgressView(true)

        val query = userReference.orderByChild("email").equalTo(email)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                if (p0.value == null || !p0.exists()) {
                    switchToProgressView(false)
                    switchToEmptyView(true)
                } else {

                    var inviteUser: User? = null

                    for (child in p0.children) {
                        inviteUser = child.getValue(User::class.java)
                    }

                    switchToProgressView(false)

                    inviteUser?.let {
                        inviteReference.child("${currentUser.uid}/sent/${it.uid}").addListenerForSingleValueEvent(object : ValueListener() {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                inviteList.add(SentInvite(inviteUser, snapshot.value != null))
                                sentInviteAdapter.notifyDataSetChanged()
                            }
                        })
                    }
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                switchToProgressView(false)
                switchToEmptyView(true)
            }
        })

        return true
    }

    private fun sendInvite(invite: SentInvite) {
        val inviteUser = invite.user

        if (inviteUser == null) {
            context?.showToast("User is null")
            return
        }

        inviteReference
                .child(currentUser.uid)
                .child("sent")
                .child(inviteUser.uid)
                .setValue(inviteUser).addOnCompleteListener {
                    if (it.isSuccessful) {
                        invite.isSent = true
                        sentInviteAdapter.notifyDataSetChanged()
                    } else context?.showToast("Failed to send invite")
                }
    }

    private fun cancelInvite(inviteSent: SentInvite) {
        val inviteUser = inviteSent.user

        if (inviteUser == null) {
            context?.showToast("User is null")
            return
        }

        inviteReference.child("${currentUser.uid}/sent/${inviteUser.uid}").removeValue().addOnSuccessListener {
            inviteSent.isSent = false
            sentInviteAdapter.notifyDataSetChanged()
        }.addOnFailureListener {
            logError("Failed to cancel invite", it)
            showToast("Failed to cancel invite")
        }
    }

    private fun switchToEmptyView(show: Boolean) {
        users_recyclerView.visibility = if (show) View.GONE else View.VISIBLE
        user_empty_view.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun switchToProgressView(show: Boolean) {
        search_user_edit_text.isClickable = !show
        user_progress_view.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        AppDatabase.getDatabase(context).friendDao().getAllFriendsLive().observe(this,
                Observer { friendListAdapter.setFriends(it) })

    }
}
