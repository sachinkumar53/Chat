package com.sachin.app.chat.ui

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.sachin.app.chat.R
import com.sachin.app.chat.adapter.ChatMessageAdapter
import com.sachin.app.chat.constants.Constant
import com.sachin.app.chat.constants.Status
import com.sachin.app.chat.constants.Type
import com.sachin.app.chat.database.AppDatabase
import com.sachin.app.chat.gif.GifFragment
import com.sachin.app.chat.gif.GifItem
import com.sachin.app.chat.listener.OnMessageClickListener
import com.sachin.app.chat.model.ChatMessage
import com.sachin.app.chat.model.Friend
import com.sachin.app.chat.notification.FCMService
import com.sachin.app.chat.util.*
import com.sachin.app.chat.util.Utils.getLastSeenText
import com.sachin.app.chat.widget.MessagePopupMenu
import com.sachin.app.chat.widget.MessagePopupMenu.Companion.ITEM_COPY
import com.sachin.app.chat.widget.MessagePopupMenu.Companion.ITEM_DELETE
import com.sachin.app.chat.widget.MessagePopupMenu.Companion.ITEM_DETAILS
import com.sachin.app.chat.widget.MessagePopupMenu.Companion.ITEM_DOWNLOAD
import com.vanniktech.emoji.EmojiPopup
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.engine.impl.GlideEngine
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.chat_app_bar_layout.*
import kotlinx.android.synthetic.main.chat_bottom_bar_layout.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*


class ChatActivity : AppCompatActivity(), TextWatcher, ChildEventListener, OnMessageClickListener {
    private val updateTypingTask = Runnable { setCurrentUserTyping(false) }

    private val messageList = arrayListOf<ChatMessage>()
    private val currentUser = FirebaseAuth.getInstance().currentUser!!
    private var chatUserUID: String? = null
    private var firstShownMessage: String? = null
    private var friend: Friend? = null

    private val currentUserReference by lazy { FirebaseDatabase.getInstance().reference.child("users").child(currentUser.uid) }
    private val chatUserReference by lazy { FirebaseDatabase.getInstance().reference.child("users").child(chatUserUID!!) }
    private val messageReference by lazy { FirebaseDatabase.getInstance().reference.child("messages").child(currentUser.uid) }
    private val chatMessageAdapter by lazy { ChatMessageAdapter(messageList) }

    private val gifFragment by lazy { GifFragment().apply { setOnGIFClickListener { sendGif(it) } } }

    private val userListener = object : ValueListener() {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            val friend = dataSnapshot.getValue(Friend::class.java)
            friend?.let {
                this@ChatActivity.friend = it
                updateToolbar(friend)
                chatMessageAdapter.friend = friend
                chatMessageAdapter.notifyDataSetChanged()
                GlobalScope.launch {
                    AppDatabase.getDatabase(this@ChatActivity)
                            .friendDao()
                            .update(friend)
                }
            }
        }
    }

    private var isTyping = false
    private val handler = Handler(Looper.getMainLooper())


    private val updateLastSeenTask = object : Runnable {
        override fun run() {
            friend?.let {
                toolbar_user_status.text = getString(R.string.last_seen_format, if (it.onlineStatus) "now" else getLastSeenText(it.timeStamp))
            }
            handler.postDelayed(this, 30 * 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_chat)

        if (intent == null) {
            finish()
            showToast("Error")
            return
        }

        chatUserUID = intent.getStringExtra(Constant.USER_UID)

        if (chatUserUID == null)
            chatUserUID = intent.getStringExtra(Intent.EXTRA_SHORTCUT_ID)

        if (chatUserUID == null)
            startActivityForResult(Intent(this, FriendChooserActivity::class.java), RC_CHOOSE_FRIEND)

        else setupThings()
    }

    private fun setupThings() {

        currentUserReference.keepSynced(true)
        chatUserReference.keepSynced(true)

        currentUserReference.child("room").setValue(chatUserUID)


        setupToolbar()

        messageReference.child(chatUserUID!!)
                .orderByKey()
                .limitToLast(50)
                .addChildEventListener(this)

        messageList.add(ChatMessage().toIndicator())

        message_recyclerView.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        message_recyclerView.adapter = chatMessageAdapter

        updateSendButton()

        send_button.setOnClickListener { sendTextMessage() }

        swipe_refresh_layout.setOnRefreshListener { loadMoreMessage() }

        setUpBottomIcons()

        if (isSendIntent(intent)) {
            message_edit_text.setText(intent.getStringExtra(Intent.EXTRA_TEXT))
            updateSendButton()
        }

        message_edit_text.addTextChangedListener(this)

        chatMessageAdapter.setOnMessageClickListener(this)
    }

    private fun loadMoreMessage() {
        if (firstShownMessage == null) {
            swipe_refresh_layout.isRefreshing = false
            return
        }

        ChatHelper.loadMoreMessages(chatUserUID!!, firstShownMessage!!, {
            it.forEachIndexed { index, chatMessage ->
                //avoid duplicate messages
                if (!messageList.contains(chatMessage))
                    messageList.add(index, chatMessage)
            }

            swipe_refresh_layout.isRefreshing = false
            chatMessageAdapter.notifyDataSetChanged()
        })
    }

    private fun sendTextMessage() {
        val message = ChatMessage(text = message_edit_text.text.toString().trim(),
                uid = currentUser.uid, status = Status.SENDING)

        ChatHelper.sendTextMessage(chatUserUID!!, message)

        messageList.addAtPreLastPosition(message)
        chatMessageAdapter.notifyDataSetChanged()
        message_edit_text.setText("")
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        updateSendButton()
    }

    override fun afterTextChanged(s: Editable?) {
        handler.removeCallbacks(updateTypingTask) //cancel if any already have
        handler.postDelayed(updateTypingTask, 2 * 1000)

        if (s?.isNotEmpty()!! && s.toString().trim().isNotEmpty() && !isTyping) {
            setCurrentUserTyping(true)
        } else if (s.toString().trim().isEmpty() && isTyping) {
            handler.removeCallbacks(updateTypingTask) //cancel stopped typing
            setCurrentUserTyping(false)
        }
    }

    private fun setCurrentUserTyping(value: Boolean) {
        isTyping = value
        currentUserReference.child("typing").setValue(value)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    private fun setupToolbar() {
        setSupportActionBar(toolbar_chat)
        toolbar_chat.setNavigationOnClickListener { finish() }

        lifecycleScope.launch {
            val friend = AppDatabase.getDatabase(this@ChatActivity).friendDao().findByUid(chatUserUID!!)
            friend?.let {
                this@ChatActivity.friend = friend
                updateToolbar(friend)
                chatMessageAdapter.friend = friend
                chatMessageAdapter.notifyDataSetChanged()
            }
        }

        chatUserReference.addValueEventListener(userListener)
    }

    override fun onDestroy() {
        super.onDestroy()

        messageReference.child(chatUserUID!!).removeEventListener(this)
        chatUserReference.removeEventListener(userListener)
        currentUserReference.child("room").setValue("")
    }

    private fun updateToolbar(friend: Friend) {
        toolbar_user_name.text = friend.name
        toolbar_user_status.text =
                getString(R.string.last_seen_format,
                        if (friend.onlineStatus) "now"
                        else Utils.getLastSeenText(friend.timeStamp))

        AvatarUtil.loadAvatar(this, friend.photoUrl, toolbar_profile_image)

        toolbar_profile_image.setOnClickListener {
            val options = ActivityOptions.makeSceneTransitionAnimation(this, it,
                    getString(R.string.transition_shared_profile_image))
            startActivity(Intent(this, ProfileActivity::class.java).apply {
                putExtra(Constant.USER_UID, chatUserUID)
            }, options.toBundle())
        }

        handler.postDelayed(updateLastSeenTask, 30 * 1000)
    }

    private fun updateSendButton() {
        send_button.isEnabled = message_edit_text.text?.trim()?.isNotEmpty() ?: false

        if (!send_button.isEnabled)
            send_button.setImageDrawable(
                    getDrawable(R.drawable.ic_send)!!
                            .mutate().apply { setTint(Color.parseColor("#B0BEC5")) }
            )
        else send_button.setImageDrawable(
                getDrawable(R.drawable.ic_send_gradient)!!)
    }

    override fun onChildAdded(p0: DataSnapshot, p1: String?) {
        val message = p0.getValue(ChatMessage::class.java) ?: return

        if (firstShownMessage == null)
            firstShownMessage = message.id

        if (!messageList.contains(message)) {
            messageList.addAtPreLastPosition(message)
            chatMessageAdapter.notifyDataSetChanged()
        }

        ChatHelper.readMessage(chatUserUID!!, message)

        message_recyclerView.scrollToPosition((messageList.size - 1))
    }

    override fun onChildChanged(p0: DataSnapshot, p1: String?) {
        val message = p0.getValue(ChatMessage::class.java) ?: return
        messageList.replaceMessage(message)?.let { chatMessageAdapter.notifyDataSetChanged() }
    }

    override fun onChildRemoved(p0: DataSnapshot) {
        val message = p0.getValue(ChatMessage::class.java) ?: return
        val old = messageList.findMessageById(message.id)
        old?.let {
            messageList.removeAt(messageList.indexOf(old))
            chatMessageAdapter.notifyDataSetChanged()
        }
    }

    override fun onChildMoved(p0: DataSnapshot, p1: String?) {}

    override fun onCancelled(p0: DatabaseError) {}

    private fun setUpBottomIcons() {
        val emojiPopUp = EmojiPopup.Builder.fromRootView(root_layout).build(message_edit_text)
        emoticon_button.setOnClickListener { emojiPopUp.toggle() }

        image_button.setOnClickListener { launchPicker() }

        video_button.setOnClickListener { launchPicker(false) }

        camera_button.setOnClickListener {
            startActivityForResult(Intent.createChooser(Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    , "Capture image using"), RC_TAKE_PICTURE)
        }

        audio_button.setOnClickListener {
            startActivityForResult(Intent(this,
                    AudioPickerActivity::class.java), RC_PICK_AUDIO)
        }

        sticker_button.setOnClickListener {
            if (!gifFragment.isAdded)
                supportFragmentManager.beginTransaction().replace(R.id.gif_place_holder, gifFragment).commit()
        }
    }

    private fun sendGif(gif: GifItem) {
        val message = ChatMessage(text = "GIF", uid = currentUser.uid, url = gif.url, type = Type.GIF, status = Status.SENDING)
        ChatHelper.sendTextMessage(chatUserUID!!, message)
    }

    override fun onBackPressed() {
        if (gifFragment.isAdded)
            supportFragmentManager.beginTransaction().remove(gifFragment).commit()
        else super.onBackPressed()
    }

    override fun onMessageClick(chatMessage: ChatMessage, view: View) {
        if (chatMessage.type == Type.VIDEO) {
            val intent = Intent(this, VideoPlayerActivity::class.java)
            intent.putExtra(Constant.VIDEO_URL, chatMessage.url)
            startActivity(intent)
        } else if (chatMessage.type == Type.IMAGE) {
            val options = ActivityOptions.makeSceneTransitionAnimation(this, view, getString(R.string.transition_shared_image))
            val intent = Intent(this, ImageViewerActivity::class.java)
            intent.putExtra("message", chatMessage)
            startActivity(intent, options.toBundle())
        }
    }

    override fun onMessageLongClick(chatMessage: ChatMessage, view: View) {
        val popup = MessagePopupMenu(this)
        popup.removeItem(if (chatMessage.type == Type.TEXT) ITEM_DOWNLOAD else ITEM_COPY)
        popup.setOnItemClickListener {
            when (it) {
                ITEM_COPY -> ChatHelper.copyMessageToClipBoard(this, chatMessage)
                ITEM_DOWNLOAD -> ChatHelper.downloadMessage(this, chatMessage)
                ITEM_DETAILS -> lifecycleScope.launch { ChatHelper.showDetails(this@ChatActivity, chatMessage) }
                ITEM_DELETE -> showToast("Delete feature will be added later")
            }
        }

        popup.show(view)
    }

    private fun launchPicker(image: Boolean = true) {
        Matisse.from(this)
                .choose(if (image) MimeType.ofImage() else MimeType.ofVideo())
                .theme(R.style.ImagePickerTheme)
                .imageEngine(GlideEngine())
                .showSingleMediaType(true)
                .countable(true)
                .thumbnailScale(0.5F)
                .gridExpectedSize(resources.getDimensionPixelSize(R.dimen.grid_expected_size))
                .maxSelectable(if (image) 4 else 1)
                .showPreview(true)
                .forResult(if (image) RC_PICK_IMAGE else RC_PICK_VIDEO)
    }

    override fun onResume() {
        super.onResume()
        if (chatUserUID != null) {
            GlobalScope.launch {
                val conversationDao = AppDatabase.getDatabase(this@ChatActivity).conversationDao()
                val conversation = conversationDao.findByUid(chatUserUID!!)
                conversation?.let {
                    conversation.seen = true
                    conversationDao.update(conversation)
                }
                ChatHelper.readConversation(chatUserUID!!)
            }

            if (FCMService.notificationMap.contains(chatUserUID))
                FCMService.notificationMap[chatUserUID!!]?.clear()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_CHOOSE_FRIEND) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                chatUserUID = data.getStringExtra(Constant.USER_UID)
                setupThings()
            } else finish()
            return
        }

        if (resultCode == Activity.RESULT_OK && data != null)
            handleResult(requestCode, data)
        else showToast("Error")
    }

    private fun handleResult(requestCode: Int, data: Intent) {
        when (requestCode) {
            RC_PICK_IMAGE -> {
                Matisse.obtainResult(data).forEach {
                    val path = ImageUtils.getRealPathFromURI(contentResolver, it)

                    val message = ChatMessage(uid = currentUser.uid, text = "Image",
                            type = Type.IMAGE, status = Status.SENDING, url = path)

                    val key = messageReference.child(chatUserUID!!).push().key!!

                    message.id = key

                    messageReference.child(chatUserUID!!).child(key).setValue(message)

                    GlobalScope.launch {
                        messageReference.child(chatUserUID!!).child(key).child("status").setValue(Status.UPLOADING)

                        ChatHelper.uploadImage(this@ChatActivity, it, key, { downloadUrl ->
                            val map = mapOf("status" to Status.SENT, "url" to downloadUrl)
                            message.url = downloadUrl
                            ChatHelper.transferMessage(chatUserUID!!, message, {
                                messageReference.child(chatUserUID!!).child(key).updateChildren(map).addOnSuccessListener {
                                    message.status = Status.SENT
                                    chatMessageAdapter.notifyDataSetChanged()
                                }
                            }, {
                                messageReference.child(chatUserUID!!).child(key).updateChildren(map).addOnSuccessListener {
                                    message.status = Status.FAILED
                                    chatMessageAdapter.notifyDataSetChanged()
                                }
                            })
                        })
                    }
                }
            }

            RC_PICK_VIDEO -> {
                Matisse.obtainPathResult(data).forEach {

                    val message = ChatMessage(uid = currentUser.uid, text = "Video",
                            type = Type.VIDEO, status = Status.SENDING, url = it)

                    val key = messageReference.child(chatUserUID!!).push().key!!
                    message.id = key

                    messageReference.child(chatUserUID!!).child(key).setValue(message)
                    messageReference.child(chatUserUID!!).child(key).child("status").setValue(Status.UPLOADING)

                    ChatHelper.uploadVideo(this@ChatActivity, it, key, { downloadUrl ->
                        val map = mapOf("status" to Status.SENT, "url" to downloadUrl)
                        message.url = downloadUrl

                        ChatHelper.transferMessage(chatUserUID!!, message, {
                            messageReference.child(chatUserUID!!).child(key).updateChildren(map).addOnSuccessListener {
                                message.status = Status.SENT
                                chatMessageAdapter.notifyDataSetChanged()
                            }
                        }, {
                            messageReference.child(chatUserUID!!).child(key).updateChildren(map).addOnSuccessListener {
                                message.status = Status.FAILED
                                chatMessageAdapter.notifyDataSetChanged()
                            }
                        })
                    })
                }
            }

            RC_PICK_AUDIO -> {
                val list = data.getStringArrayListExtra(AudioPickerActivity.SELECTED_AUDIO_FILES)
                list?.forEach {
                    val mediaMetadataRetriever = MediaMetadataRetriever()
                    mediaMetadataRetriever.setDataSource(it)
                    val name = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)

                    val data = "$name~false~false"
                    val message = ChatMessage(uid = currentUser.uid, text = "Audio",
                            data = data,
                            type = Type.AUDIO, status = Status.SENDING, url = it)

                    val key = messageReference.child(chatUserUID!!).push().key!!
                    message.id = key

                    messageReference.child(chatUserUID!!).child(key).setValue(message)
                    messageReference.child(chatUserUID!!).child(key).child("status").setValue(Status.UPLOADING)

                    ChatHelper.uploadAudio(it, key, { downloadUrl ->
                        val map = mapOf("status" to Status.SENT, "url" to downloadUrl)

                        message.url = downloadUrl
                        ChatHelper.transferMessage(chatUserUID!!, message, {
                            messageReference.child(chatUserUID!!).child(key).updateChildren(map).addOnSuccessListener {
                                message.status = Status.SENT
                                chatMessageAdapter.notifyDataSetChanged()
                            }
                        }, {
                            messageReference.child(chatUserUID!!).child(key).updateChildren(map).addOnSuccessListener {
                                message.status = Status.FAILED
                                chatMessageAdapter.notifyDataSetChanged()
                            }
                        })
                    })
                }
            }
        }
    }

    private fun isSendIntent(intent: Intent): Boolean = intent.action == Intent.ACTION_SEND && intent.type == "text/plain"

    companion object {
        private const val RC_PICK_IMAGE = 1156
        private const val RC_TAKE_PICTURE = 7974
        private const val RC_PICK_AUDIO = 8449
        private const val RC_PICK_VIDEO = 3165

        private const val RC_CHOOSE_FRIEND = 651
    }
}

//---------------------------Extensions------------------------//

private fun <E> ArrayList<E>.addAtPreLastPosition(element: E) =
        if (size > 0)
            add(size - 1, element)
        else add(element)

private fun ChatMessage.toIndicator(): ChatMessage = ChatMessage(type = Type.INDICATOR)

private fun ArrayList<ChatMessage>.findMessageById(id: String?): ChatMessage? {
    if (id == null) return null

    for (chat in this) {
        if (chat.id.equals(id, true))
            return chat
    }

    return null
}

private fun ArrayList<ChatMessage>.replaceMessage(message: ChatMessage): Int? {
    val chat = findMessageById(message.id)
    chat?.let {
        val i = indexOf(it)
        this[i] = message
        return i
    }

    return null
}
