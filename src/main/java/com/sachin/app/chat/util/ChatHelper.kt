package com.sachin.app.chat.util

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.format.DateFormat
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.iamkdblue.videocompressor.VideoCompress
import com.sachin.app.chat.constants.Status
import com.sachin.app.chat.constants.Type
import com.sachin.app.chat.database.AppDatabase
import com.sachin.app.chat.model.ChatMessage
import com.sachin.app.chat.model.Conversation
import com.sachin.app.chat.model.Friend
import com.sachin.app.chat.notification.FCData
import com.sachin.app.chat.notification.FCMApiClient
import com.sachin.app.chat.notification.FCMApiService
import com.sachin.app.chat.notification.FCMessage
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.*

object ChatHelper {
    private const val TAG = "ChatHelper"
    private const val FILE_BASE_PATH = "file://"
    private const val DOWNLOADING = "Downloading"

    fun readMessage(friendUID: String, message: ChatMessage, onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null || message.uid == uid || message.status == Status.SEEN || message.id == null) return

        FirebaseDatabase.getInstance().reference
                .child("messages")
                .child(friendUID)
                .child(uid)
                .child(message.id!!).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(p0: DataSnapshot) {

                        Log.w(TAG, "readMessage: Value = ${p0.value}")

                        if (p0.exists() && p0.value != null && p0.hasChild("status")) {

                            p0.ref.child("status")
                                    .setValue(Status.SEEN)
                                    .addOnSuccessListener { onSuccess() }
                                    .addOnFailureListener {
                                        Log.e(TAG, "Read message error", it)
                                        onFailure()
                                    }
                        }
                    }

                    override fun onCancelled(p0: DatabaseError) {
                        Log.e(TAG, "Read message error", p0.toException())
                        onFailure()
                    }
                })
    }

    fun sendTextMessage(friendUID: String, message: ChatMessage, onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val messageReference = FirebaseDatabase.getInstance().reference.child("messages")
        val key = messageReference.child(friendUID).push().key!!
        message.id = key

        val map = mapOf(
                "$uid/$friendUID/$key" to message,
                "$friendUID/$uid/$key" to message
        )

        messageReference.updateChildren(map)
                .addOnSuccessListener {
                    messageReference.child(uid).child(friendUID)
                            .child(key)
                            .child("status")
                            .setValue(Status.SENT)
                            .addOnSuccessListener {
                                onSuccess()
                            }.addOnFailureListener {
                                onFailure()
                            }
                }.addOnFailureListener {
                    Log.e(TAG, "sendTextMessage: ", it)
                    onFailure()
                }
    }

    fun notifyNewMessage(context: Context, friendUID: String,message: ChatMessage){
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val user = context.getLocalUser()
        FCMApiClient.client!!.create(FCMApiService::class.java)
                .sendNotification(FCMessage(user.deviceToken!!, FCData(user.name, user.photoUrl,,message.text,message.id,uid,friendUID)))
                .enqueue(object :Callback<ResponseBody>{
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        Log.w(TAG, "onResponse: ${response.body()}" )
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.e(TAG, "onFailure: ",t )
                    }
                })
    }

    /*fun sendMessage(friendUID: String, message: ChatMessage, onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val messageReference = FirebaseDatabase.getInstance().reference.child("messages")
        val key = messageReference.child(friendUID).push().key!!
        message.id = key

        messageReference
                .child(uid)
                .child(friendUID)
                .child(key)
                .setValue(message)
                .addOnSuccessListener {
                    messageReference.child(uid)
                            .child(friendUID)
                            .child(key)
                            .child("status")
                            .setValue(Status.SENT).addOnSuccessListener {
                                onSuccess()
                            }.addOnFailureListener {
                                onFailure()
                            }
                }.addOnFailureListener {
                    Log.e(TAG, "sendMessage", it)
                    onFailure()
                }
    }*/

    suspend fun uploadImage(context: Context, uri: Uri, key: String, onSuccess: (String) -> Unit = {}, onFailure: () -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val file = FileUtil.from(context, uri)
        val imageFile = Compressor.compress(context, file) {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true

            //val temp = BitmapFactory.decodeFile(file.absolutePath, options)
            //if (temp.width > temp.height)
            resolution(960, 540)
            /*else
                resolution(540, 960)*/
            quality(70)
            format(Bitmap.CompressFormat.JPEG)
        }

        FirebaseStorage.getInstance().reference.child("images").child(uid).child("$key.jpg")
                .putFile(Uri.fromFile(imageFile)).addOnSuccessListener {
                    it.storage.downloadUrl
                            .addOnSuccessListener { url -> onSuccess(url.toString()) }
                            .addOnFailureListener { onFailure() }
                }.addOnFailureListener {
                    Log.e(TAG, "uploadImage ", it)
                    onFailure()
                }
    }


    fun uploadVideo(context: Context, path: String, key: String, onSuccess: (String) -> Unit = {}, onFailure: () -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val parent = context.getExternalFilesDir(null)
        val folder = File(parent, "media")
        if (!folder.exists())
            folder.mkdirs()

        val outFileName = "$key.mp4"

        val destPath = File(folder, outFileName).absolutePath

        VideoCompress.compressVideoMedium(path, destPath, object : VideoCompress.CompressListener {
            override fun onStart() {}

            override fun onSuccess(compressVideoPath: String?) {
                FirebaseStorage.getInstance().reference.child("videos").child(uid).child("$key.mp4")
                        .putFile(Uri.fromFile(File(path))).addOnSuccessListener {
                            it.storage.downloadUrl
                                    .addOnSuccessListener { url -> onSuccess(url.toString()) }
                                    .addOnFailureListener { onFailure() }
                        }.addOnFailureListener {
                            Log.e(TAG, "Upload video", it)
                            onFailure()
                        }
            }

            override fun onProgress(percent: Float) {
                Log.w(TAG, "Compressing video file $outFileName = $percent")
            }

            override fun onFail() {
                onFailure()
            }
        })
    }


    fun uploadAudio(path: String, key: String, onSuccess: (String) -> Unit = {}, onFailure: () -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val file = File(path)
        val uri = Uri.fromFile(file)
        val ext = file.extension

        FirebaseStorage.getInstance().reference.child("audios").child(uid).child("$key.$ext")
                .putFile(uri).addOnSuccessListener {
                    it.storage.downloadUrl
                            .addOnSuccessListener { url -> onSuccess(url.toString()) }
                            .addOnFailureListener { onFailure() }
                }.addOnFailureListener {
                    logError("Upload audio error", it)
                    onFailure()
                }
    }

    fun transferMessage(friendUID: String, message: ChatMessage, onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val messageReference = FirebaseDatabase.getInstance().reference.child("messages")
        messageReference.child(friendUID).child(uid)
                .child(message.id!!)
                .setValue(message)
                .addOnSuccessListener {
                    onSuccess()
                }.addOnFailureListener {
                    logError("Transfer message error", it)
                    onFailure()
                }
    }

    fun loadMoreMessages(friendUID: String, firstShownMessage: String, onSuccess: (List<ChatMessage>) -> Unit = {}, onFailure: () -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().reference
                .child("messages")
                .child(uid)
                .child(friendUID).orderByKey().endAt(firstShownMessage).limitToLast(40).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(p0: DataSnapshot) {
                        //Log.w("TAG", "onDataChange: Has children ${p0.hasChildren()}" )
                        if (p0.hasChildren()) {
                            val list = arrayListOf<ChatMessage>()
                            for (child in p0.children) {
                                val message = child.getValue(ChatMessage::class.java)
                                message?.let { list.add(it) }
                            }

                            onSuccess(list)
                        } else onSuccess(listOf())
                    }

                    override fun onCancelled(p0: DatabaseError) {
                        onFailure()
                        logWarn("Load more message error")
                    }
                })
    }

    fun copyMessageToClipBoard(context: Context, message: ChatMessage) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (message.type == Type.TEXT) {
            val clipData = ClipData.newPlainText("Copied text", message.text)
            clipboardManager.setPrimaryClip(clipData)
            context.showToast("Message copied")
        }
    }


    fun downloadMessage(context: Context, message: ChatMessage) {
        if (message.url == null) {
            context.showToast("No download link")
            return
        }

        when (message.type) {
            Type.IMAGE -> downloadImage(context, message)
            Type.AUDIO -> downloadAudio(context, message)
            Type.VIDEO -> downloadVideo(context, message)
            else -> context.showToast("No download link")
        }
    }

    fun downloadImage(context: Context, message: ChatMessage) {
        val sdCard = context.getExternalFilesDir(null).toString()
        val name = "IMG_${System.currentTimeMillis()}.jpg"
        val destination = "$sdCard/Chat/Images/$name"

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(message.url))
        request.setTitle(name)
        request.setDescription(DOWNLOADING)
        val uri = Uri.parse("$FILE_BASE_PATH$destination")
        request.setDestinationUri(uri)
        downloadManager.enqueue(request)
        context.showToast("$DOWNLOADING image")
    }

    fun downloadAudio(context: Context, message: ChatMessage) {
        val sdCard = context.getExternalFilesDir(null).toString()
        val name = "AUD_${System.currentTimeMillis()}.mp3"
        val destination = "$sdCard/Chat/Audios/$name"

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(message.url))
        request.setTitle(name)
        request.setDescription(DOWNLOADING)
        val uri = Uri.parse("$FILE_BASE_PATH$destination")
        request.setDestinationUri(uri)
        downloadManager.enqueue(request)
        context.showToast("$DOWNLOADING audio")
    }

    fun downloadVideo(context: Context, message: ChatMessage) {
        val sdCard = context.getExternalFilesDir(null).toString()
        val name = "VID_${System.currentTimeMillis()}.mp4"
        val destination = "$sdCard/Chat/Videos/$name"

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(message.url))
        request.setTitle(name)
        request.setDescription(DOWNLOADING)
        val uri = Uri.parse("$FILE_BASE_PATH$destination")
        request.setDestinationUri(uri)
        downloadManager.enqueue(request)
        context.showToast("$DOWNLOADING video")
    }

    suspend fun showDetails(context: Context, message: ChatMessage) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            context.showToast("Couldn't show details")
            return
        }

        val myMessage = message.uid == uid

        val user = if (myMessage) context.getLocalUser() else AppDatabase.getDatabase(context).friendDao().findByUid(uid)?.toUser()

        if (user == null) {
            context.showToast("Couldn't show details")
            return
        }

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Details")
        val dateFormat = DateFormat.getBestDateTimePattern(Locale.getDefault(), "hh:mm dd MMMM yyyy")
        val items = arrayOf(
                "Sender : ${user.name}",
                "Type : ${message.type?.capitalize()}",
                "Status : ${Status.getMessageStatus(message.status)}",
                "Time : ${DateFormat.format(dateFormat, message.time)}"
        )

        builder.setItems(items, null)
        builder.setPositiveButton("Ok") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.listView.onItemClickListener = null
        dialog.show()
    }

    fun readConversation(friendUID: String, onSuccess: () -> Unit={}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseDatabase.getInstance().reference
                .child("conversations")
                .child(uid)
                .child(friendUID).addListenerForSingleValueEvent(object :ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists() && snapshot.hasChild("seen"))
                            snapshot.ref.child("seen").setValue(true).addOnSuccessListener { onSuccess() }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
    }
}