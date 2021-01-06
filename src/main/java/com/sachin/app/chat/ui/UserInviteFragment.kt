package com.sachin.app.chat.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sachin.app.chat.R
import com.sachin.app.chat.adapter.SentInviteAdapter
import com.sachin.app.chat.model.SentInvite
import com.sachin.app.chat.model.User
import com.sachin.app.chat.util.*
import kotlinx.android.synthetic.main.fragment_invitation.*

class UserInviteFragment : Fragment(), SearchView.OnQueryTextListener {
    private val user by lazy { FirebaseAuth.getInstance().currentUser!! }
    private val userReference by lazy { FirebaseDatabase.getInstance().reference.child("users") }
    private val inviteReference by lazy { FirebaseDatabase.getInstance().reference.child("invites") }
    private val inviteList = ArrayList<SentInvite>()
    private val sentInviteAdapter by lazy { SentInviteAdapter(inviteList) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as UserListActivity).searchView.setOnQueryTextListener(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_user_invite, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        invite_recyclerView.layoutManager = LinearLayoutManager(context)
        invite_recyclerView.adapter = sentInviteAdapter

        sentInviteAdapter.setOnInviteSentClickListener {
            val inviteSent = inviteList[it]

            if (inviteSent.isSent) cancelInvite(inviteSent)
            else sendInvite(inviteSent)
        }
    }

    private fun sendInvite(invite: SentInvite) {
        val inviteUser = invite.user

        if (inviteUser == null) {
            context?.showToast("User is null")
            return
        }

        inviteReference
                .child(user.uid)
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

        inviteReference.child("${user.uid}/sent/${inviteUser.uid}").removeValue().addOnSuccessListener {
            inviteSent.isSent = false
            sentInviteAdapter.notifyDataSetChanged()
        }.addOnFailureListener {
            logError("Failed to cancel invite", it)
            showToast("Failed to cancel invite")
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        (activity as UserListActivity).progressBar.show()
        query?.let {
            val q = userReference.orderByChild("email").equalTo(it)
            q.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.value == null || !p0.exists())
                        Snackbar.make(invite_recyclerView, "No user found", Snackbar.LENGTH_LONG)
                    else {

                        var inviteUser: User? = null
                        for (child in p0.children) {
                            inviteUser = child.getValue(User::class.java)
                        }

                        (activity as UserListActivity).progressBar.hide()
                        inviteUser?.let {
                            inviteReference.child("${user.uid}/sent/${it.uid}").addListenerForSingleValueEvent(object : ValueListener() {
                                override fun onDataChange(snapshot: DataSnapshot){
                                    inviteList.add(SentInvite(inviteUser, snapshot.value != null))
                                    sentInviteAdapter.notifyDataSetChanged()
                                }
                            })
                        }
                    }
                }

                override fun onCancelled(p0: DatabaseError) {}
            })
        }
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return false
    }
}
