package com.sachin.app.chat.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.sachin.app.chat.R
import com.sachin.app.chat.adapter.ConversationAdapter
import com.sachin.app.chat.database.AppDatabase
import com.sachin.app.chat.model.Conversation
import com.sachin.app.chat.constants.Constant.USER_UID
import com.sachin.app.chat.util.FirebaseUtils
import com.sachin.app.chat.widget.SimpleDialogFragment
import kotlinx.android.synthetic.main.fragment_chat_list.*

class ConversationFragment : Fragment() {
    private val conversationDao by lazy { AppDatabase.getDatabase(requireContext()).conversationDao() }
    private val chatList = ArrayList<Conversation>()
    private val chatListAdapter by lazy { ConversationAdapter(chatList) }
    private val currentUser by lazy { FirebaseAuth.getInstance().currentUser!! }
    private val conversationReference by lazy { FirebaseDatabase.getInstance().reference.child("conversations").child(currentUser.uid) }
    private val messageReference by lazy { FirebaseDatabase.getInstance().reference.child("messages").child(currentUser.uid) }
    private val conversationListLive by lazy { conversationDao.getAllConversationsLive() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_chat_list, container, false)


    override fun onAttach(context: Context) {
        super.onAttach(context)

        conversationReference.keepSynced(true)
        messageReference.keepSynced(true)

        conversationListLive.observe(this, Observer { list ->
            chatList.clear()
            chatList.addAll(list)
            chatListAdapter.notifyDataSetChanged()
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chat_list_recyclerView.run {
            layoutManager = LinearLayoutManager(context)
            adapter = chatListAdapter
        }

        chatListAdapter.setOnChatClickListener { v, c ->
            //val option = ActivityOptions.makeSceneTransitionAnimation(requireActivity(), v,
             //       getString(R.string.transition_shared_profile_image))
            startActivity(Intent(activity, ChatActivity::class.java).apply {
                putExtra(USER_UID, c.senderUid)
            }) //, option.toBundle())
        }

        chatListAdapter.setOnChatClickLongListener { showDeleteAlertDialog(it) }
    }

    private fun showDeleteAlertDialog(conversation: Conversation) {
        SimpleDialogFragment().apply {
            setTitle(this@ConversationFragment.getString(R.string.delete_conversation_title))
            setMessage(this@ConversationFragment.getString(R.string.delete_conversation_message))
            setPositiveButtonTextColor(this@ConversationFragment.requireContext().getColor(R.color.delete_text_color))
            setPositiveButton(this@ConversationFragment.getString(R.string.delete)) {
                FirebaseUtils.deleteConversation(conversation.senderUid) {
                    chatList.remove(conversation)
                    chatListAdapter.notifyDataSetChanged()
                }
                it.dismiss()
            }
            setNegativeButton(this@ConversationFragment.getString(R.string.cancel))
        }.show(this)
    }
}

