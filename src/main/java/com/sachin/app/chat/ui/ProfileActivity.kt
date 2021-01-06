package com.sachin.app.chat.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sachin.app.chat.R
import com.sachin.app.chat.constants.Constant
import com.sachin.app.chat.database.AppDatabase
import com.sachin.app.chat.model.User
import com.sachin.app.chat.util.*
import com.sachin.app.chat.constants.Constant.CROP_IMAGE
import com.sachin.app.chat.constants.Constant.PICK_IMAGE
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.preference_item_email.*
import kotlinx.android.synthetic.main.preference_item_status.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.*

class ProfileActivity : AppCompatActivity(), ValueEventListener {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val currentUser by lazy { auth.currentUser!! }
    private lateinit var userUID: String
    private val userReference by lazy { FirebaseDatabase.getInstance().reference.child("users") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.attributes.dimAmount = 0.2F

        setContentView(R.layout.activity_profile)

        userUID = if (intent != null && intent.extras != null && intent.hasExtra(Constant.USER_UID))
            intent.getStringExtra(Constant.USER_UID)!! else {
            finish()
            return
        }

        userReference.child(userUID).apply { keepSynced(true) }.addValueEventListener(this)

        fab_edit_image.setOnClickListener { startImagePicker() }

        val v = if (userUID == currentUser.uid) View.VISIBLE else View.GONE

        fab_edit_image.visibility = v
        profile_buttons_layout.visibility = if (userUID == currentUser.uid) View.GONE else View.VISIBLE
        profile_sign_out_button.visibility = v
        profile_sign_out_button.setOnClickListener { signOut() }

        lifecycleScope.launch {
            val friend = AppDatabase.getDatabase(this@ProfileActivity).friendDao().findByUid(userUID)
            updateUI(friend?.toUser() ?: getLocalUser())
        }
    }

    private fun updateUI(user: User) {
        profile_name.text = user.name
        profile_status.text = if (userUID == currentUser.uid || user.onlineStatus) "Active now" else ("Active " + Utils.getLastSeenText(user.timeStamp))
        profile_about.text = user.about
        profile_email.text = user.email

        AvatarUtil.loadAvatar(this, user.photoUrl, profile_image)
    }

    private fun signOut(): Boolean {
        var result = false
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInClient.signOut().addOnCompleteListener {
            if (it.isSuccessful) {
                userReference.child(currentUser.uid)
                        .updateChildren(mapOf(Pair("onlineStatus", false), Pair("timeStamp", Date().time)))
                deleteLocalUser()
                GlobalScope.launch {
                    AppDatabase.getDatabase(this@ProfileActivity)
                            .clearAllTables()
                }
                auth.signOut()
                startActivity(Intent(this, SignInActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                })

                finishAffinity()
                result = true
            }
        }

        return result
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data

            if (uri == null) {
                showToast("Error selecting image")
                return
            }

            startActivityForResult(Intent(this, ImageCropActivity::class.java).apply {
                putExtras(data)
                setData(data.data)
            }, CROP_IMAGE)
        }

        if (requestCode == CROP_IMAGE && resultCode == Activity.RESULT_OK) {
            val extras = data?.extras
            if (extras != null) {
                val bitmap = extras.getParcelable<Bitmap>("photo")
                profile_image.setImageBitmap(bitmap)
                uploadImage(bitmap)
            }
        }
    }

    private fun uploadImage(bitmap: Bitmap?) {
        if (bitmap == null) return

        val os = ByteArrayOutputStream()

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)

        val data = os.toByteArray()

        FirebaseUtils.userProfilePicReference.putBytes(data).addOnSuccessListener {
            it.storage.downloadUrl.addOnSuccessListener { uri ->
                userReference.child(userUID).child("photoUrl").setValue(uri.toString())
            }.addOnFailureListener { e ->
                showToast("Failed to upload profile image: ${e.message}")
                logError(e)
            }
        }.addOnFailureListener {
            showToast("Failed to upload profile image: ${it.message}")
            logError(it)
        }
    }

    override fun onDataChange(p0: DataSnapshot) {
        val user = p0.getValue(User::class.java)
        user?.let { updateUI(it) }
    }

    override fun onCancelled(p0: DatabaseError) {}

    override fun onDestroy() {
        userReference.child(userUID).removeEventListener(this)
        super.onDestroy()
    }
}
