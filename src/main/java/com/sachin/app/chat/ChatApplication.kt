package com.sachin.app.chat

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.facebook.drawee.backends.pipeline.Fresco
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.sachin.app.chat.database.AppDatabase
import com.sachin.app.chat.model.Conversation
import com.sachin.app.chat.model.Friend
import com.sachin.app.chat.ui.MainActivity
import com.sachin.app.chat.util.logError
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.one.EmojiOneProvider
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ChatApplication : Application(), Application.ActivityLifecycleCallbacks, FirebaseAuth.AuthStateListener {
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = Runnable { setUserOnline(false) }

    private val auth by lazy { FirebaseAuth.getInstance() }

    private val appDatabase by lazy { AppDatabase.getDatabase(this) }


    private val databaseReference by lazy { FirebaseDatabase.getInstance().reference }
    private val friendDatabase by lazy { databaseReference.child("friends") }
    private val inviteDatabase by lazy { databaseReference.child("invites") }
    private val conversationDatabase by lazy { databaseReference.child("conversations") }
    private val messageDatabase by lazy { databaseReference.child("messages") }

    private var currentUser: FirebaseUser? = null

    private lateinit var friendListener: ChildEventListener
    private lateinit var conversationListener: ChildEventListener


    override fun onCreate() {
        super.onCreate()

        Fresco.initialize(this)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        registerActivityLifecycleCallbacks(this)
        EmojiManager.install(EmojiOneProvider())

        // Create a builder for ApiService
        //val builder = ApiService.Builder(this, IApiClient::class.java)
        // add your tenor API key here
        //builder.apiKey(Constant.TENOR_API_KEY)
        // initialize the Tenor ApiClient
        //ApiClient.init(this, builder)

        currentUser = auth.currentUser
        auth.addAuthStateListener(this)

        init()

        currentUser?.let {
            keepSynced(it.uid)
            friendDatabase.child(it.uid).addChildEventListener(friendListener)
            conversationDatabase.child(it.uid).addChildEventListener(conversationListener)
        }
    }

    private fun init() {

        val friendDao = appDatabase.friendDao()
        val conversationDao = appDatabase.conversationDao()

        conversationListener = object : ChildEventListener {
            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                val conversation = p0.getValue(Conversation::class.java)
                Log.e(TAG, "onChildAdded: $conversation" )

                if (conversation == null) {
                    Log.e(TAG, "onChildAdded: Conversation is null: Value = $conversation" )
                    return
                }

                GlobalScope.launch {
                    val oldConversation = conversationDao.findByUid(conversation.senderUid)
                    if (oldConversation == null)
                        conversationDao.insert(conversation)
                    else if (oldConversation != conversation)
                        conversationDao.update(conversation)
                }
            }

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {
                val conversation = p0.getValue(Conversation::class.java)
                if (conversation == null) {
                    Log.e(TAG, "onChildChanged: Conversation is null: Value = $conversation" )
                    return
                }

                GlobalScope.launch {
                    val oldConversation = conversationDao.findByUid(conversation.senderUid)
                    if (oldConversation == null)
                        conversationDao.insert(conversation)
                    else if (oldConversation != conversation)
                        conversationDao.update(conversation)
                }
            }

            override fun onChildRemoved(p0: DataSnapshot) {
                val conversation = p0.getValue(Conversation::class.java)
                if (conversation == null) {
                    Log.e(TAG, "onChildRemoved: Conversation is null: Value = $conversation" )
                    return
                }

                GlobalScope.launch {
                    val oldConversation = conversationDao.findByUid(conversation.senderUid)
                    if (oldConversation != null)
                        conversationDao.delete(oldConversation)
                }
            }

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {}

            override fun onCancelled(p0: DatabaseError) {}
        }


        friendListener = object : ChildEventListener {
            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                val friend = p0.getValue(Friend::class.java)
                Log.w(TAG, "onChildAdded: $friend" )
                if (friend == null) {
                    Log.e(TAG, "onChildAdded: Friend is null: Value = $friend" )
                    return
                }

                GlobalScope.launch {
                    val oldFriend = friendDao.findByUid(friend.uid)
                    if (oldFriend == null)
                        friendDao.insert(friend)
                    else if (oldFriend != friend)
                        friendDao.update(friend)
                }
            }

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {
                val friend = p0.getValue(Friend::class.java)
                if (friend == null) {
                    Log.e(TAG, "onChildChanged: Friend is null: Value = $friend" )
                    return
                }

                GlobalScope.launch {
                    val oldFriend = friendDao.findByUid(friend.uid)
                    if (oldFriend == null)
                        friendDao.insert(friend)
                    else if (oldFriend != friend)
                        friendDao.update(friend)
                }
            }

            override fun onChildRemoved(p0: DataSnapshot) {
                val friend = p0.getValue(Friend::class.java)
                if (friend == null) {
                    Log.e(TAG, "onChildRemoved: Friend is null: Value = $friend" )
                    return
                }

                GlobalScope.launch {
                    val oldFriend = friendDao.findByUid(friend.uid)
                    if (oldFriend != null)
                        friendDao.delete(oldFriend)
                }
            }

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {}

            override fun onCancelled(p0: DatabaseError) {}
        }
    }

    private fun keepSynced(uid: String) {
        friendDatabase.child(uid).keepSynced(true)
        inviteDatabase.child(uid).child("received").keepSynced(true)
        conversationDatabase.child(uid).keepSynced(true)
        messageDatabase.child(uid).keepSynced(true)
    }

    override fun onAuthStateChanged(p0: FirebaseAuth) {
        if (p0.currentUser == null) {
            currentUser?.let {
                friendDatabase.child(it.uid).removeEventListener(friendListener)
                conversationDatabase.child(it.uid).removeEventListener(conversationListener)
            }
            currentUser = p0.currentUser
        } else {
            currentUser = p0.currentUser
            currentUser?.let {
                keepSynced(it.uid)
                friendDatabase.child(it.uid).addChildEventListener(friendListener)
                conversationDatabase.child(it.uid).addChildEventListener(conversationListener)
            }
        }
    }

    override fun onActivityPaused(activity: Activity) {
        handler.postDelayed(runnable, DELAY)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        handler.removeCallbacks(runnable)
    }

    override fun onActivityResumed(activity: Activity) {
        handler.removeCallbacks(runnable)

    }

    override fun onActivityStarted(activity: Activity) {
        handler.removeCallbacks(runnable)
        setUserOnline(true)
    }

    override fun onActivityStopped(activity: Activity) {

    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activity is MainActivity)
            setUserOnline(false)
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    private fun setUserOnline(online: Boolean) {
        FirebaseAuth.getInstance().currentUser?.let {
            FirebaseDatabase.getInstance().reference.child("users").child(it.uid)
                    .child("onlineStatus").setValue(online)

            // add time stamp while going offline
            if (!online) {
                FirebaseDatabase.getInstance().reference.child("users").child(it.uid)
                        .child("timeStamp").setValue(ServerValue.TIMESTAMP)
            }
        }
    }

    companion object {
        private const val TAG = "ChatApplication"
        private const val DELAY = 500L
    }
}