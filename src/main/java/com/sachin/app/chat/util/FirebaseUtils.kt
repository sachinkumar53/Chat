package com.sachin.app.chat.util

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.sachin.app.chat.model.User

object FirebaseUtils {
    private const val TAG = "FirebaseUtils"

    val auth = FirebaseAuth.getInstance()
    var user = auth.currentUser
    var currentUser: User? = null

    val userReference by lazy { FirebaseDatabase.getInstance().reference.child("users") }
    val storageReference by lazy { FirebaseStorage.getInstance().reference }

    val profileImageReference by lazy { storageReference.child("profile_images") }
    val userProfilePicReference by lazy { profileImageReference.child(user?.uid!!).child("profile_pic.png") }

    fun deleteConversation(friendUID: String, onSuccess: () -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().reference

        val task1 = database.child("conversations").child(uid).child(friendUID).removeValue()
        val task2 = database.child("messages").child(uid).child(friendUID).removeValue()

        Tasks.whenAllComplete(task1, task2).addOnCompleteListener {
            if (it.isSuccessful){
                onSuccess()
                Log.d(TAG, "deleteConversation: Delete successful")
            }else{
                Log.e(TAG, "deleteConversation: Delete unsuccessful", it.exception)
            }
        }
    }
}