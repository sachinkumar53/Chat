package com.sachin.app.chat.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.sachin.app.chat.R
import com.sachin.app.chat.adapter.ReceivedInviteAdapter
import com.sachin.app.chat.adapter.SentInviteAdapter
import com.sachin.app.chat.database.AppDatabase
import com.sachin.app.chat.model.Friend
import com.sachin.app.chat.model.ReceivedInvite
import com.sachin.app.chat.model.SentInvite
import com.sachin.app.chat.model.User
import com.sachin.app.chat.util.logError
import com.sachin.app.chat.util.showToast
import kotlinx.android.synthetic.main.fragment_invitation.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class InvitationFragment : Fragment() {
    private val currentUser = FirebaseAuth.getInstance().currentUser!!
    private var section: Int = 0
    private val sentInvites = arrayListOf<SentInvite>()
    private val receivedInvites = arrayListOf<ReceivedInvite>()

    private val receivedInviteAdapter by lazy {
        ReceivedInviteAdapter(receivedInvites).apply {
            setOnInviteAcceptListener { acceptInvite(it) }
            setOnInviteRejectListener { rejectInvite(it) }
        }
    }

    private val sentInviteAdapter by lazy { SentInviteAdapter(sentInvites) }
    private val inviteReference by lazy { FirebaseDatabase.getInstance().reference.child("invites") }
    private val sentInviteReference by lazy { inviteReference.child(currentUser.uid).child("sent") }
    private val receivedInviteReference by lazy { inviteReference.child(currentUser.uid).child("received") }

    private val db by lazy { AppDatabase.getDatabase(requireContext()) }

    private val friendDao by lazy { db.friendDao() }

    private lateinit var sentListener: ChildEventListener
    private lateinit var receivedListener: ChildEventListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        section = arguments?.getInt(ARG_SECTION_NUMBER, 0) ?: section

        sentListener = object : ChildEventListener {
            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                val user = p0.getValue(User::class.java)
                sentInvites.add(SentInvite(user, true))
                sentInviteAdapter.notifyDataSetChanged()
            }

            override fun onChildRemoved(p0: DataSnapshot) {}

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {}

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {}

            override fun onCancelled(p0: DatabaseError) {}
        }

        receivedListener = object : ChildEventListener {
            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                val invite = p0.getValue(ReceivedInvite::class.java)
                invite?.let {
                    receivedInvites.add(invite)
                    receivedInviteAdapter.notifyDataSetChanged()
                }
            }

            override fun onChildRemoved(p0: DataSnapshot) {}

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {}

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {}

            override fun onCancelled(p0: DatabaseError) {}
        }

        if (section == 0) {
            receivedInviteReference.addChildEventListener(receivedListener)

        } else {
            sentInviteReference.addChildEventListener(sentListener)
            sentInviteAdapter.setOnInviteSentClickListener {
                val inviteSent = sentInvites[it]
                if (inviteSent.isSent)
                    cancelInvite(inviteSent)
                else
                    sendInvite(inviteSent)
            }
        }
    }

    private fun sendInvite(invite: SentInvite) {
        val inviteUser = invite.user

        if (inviteUser == null) {
            context?.showToast("User is null")
            return
        }

        inviteReference.child(currentUser.uid)
                .child("sent")
                .child(inviteUser.uid)
                .setValue(inviteUser).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
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

        inviteReference.child(inviteUser.uid).child("received/${currentUser.uid}")
                .removeValue().addOnCompleteListener {
                    inviteSent.isSent = false
                    sentInviteAdapter.notifyDataSetChanged()
                }.addOnFailureListener {
                    logError("Failed to cancel invite", it)
                    showToast("Failed to cancel invite")
                }
    }

    private fun acceptInvite(invite: ReceivedInvite) {
        val user = invite.user ?: return

        val friendReference = FirebaseDatabase.getInstance().reference.child("friends").child(currentUser.uid)
        friendReference.child(user.uid).setValue(user).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                GlobalScope.launch {
                    val friend = Friend.fromUser(user)
                    friendDao.insert(friend)
                    Log.d("InvitationFragment", "Saved to local database : $friend")
                }

                val i = receivedInvites.indexOf(invite)
                receivedInvites.remove(invite)
                receivedInviteAdapter.notifyItemRemoved(i)
            } else showToast("Couldn't accept invite")
            task.exception?.let { logError("Couldn't accept invite", it) }
        }
    }


    private fun rejectInvite(invite: ReceivedInvite) {
        val user = invite.user ?: return
        inviteReference.child(currentUser.uid).child("received")
                .child(user.uid).removeValue().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val i = receivedInvites.indexOf(invite)
                        receivedInvites.remove(invite)
                        receivedInviteAdapter.notifyItemRemoved(i)
                    } else showToast("Couldn't reject invite")
                    task.exception?.let { logError("Couldn't reject invite", it) }
                }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_invitation, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        invite_recyclerView.layoutManager = LinearLayoutManager(context)
        invite_recyclerView.adapter = if (section == 0) receivedInviteAdapter else sentInviteAdapter
    }

    override fun onDestroy() {
        super.onDestroy()

        sentInvites.clear()

        receivedInviteReference.removeEventListener(receivedListener)
        sentInviteReference.removeEventListener(sentListener)
    }

    companion object {
        private const val ARG_SECTION_NUMBER = "section_number"

        @JvmStatic
        fun newInstance(sectionNumber: Int): InvitationFragment {
            return InvitationFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }
}