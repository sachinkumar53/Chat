package com.sachin.app.chat.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.sachin.app.chat.R
import com.sachin.app.chat.util.showToast
import kotlinx.android.synthetic.main.fragment_delete_account.*

class DeleteAccountFragment : BottomSheetDialogFragment() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val databaseReference by lazy { FirebaseDatabase.getInstance().reference }
    private val currentUserReference by lazy { databaseReference.child("users") }
    private val currentMessageReference by lazy { databaseReference.child("messages") }
    private val currentProfilePicReference by lazy { FirebaseStorage.getInstance().reference.child("profile_images") }
    private val storage by lazy { FirebaseStorage.getInstance().reference.storage }
    //private val currentImageReference by lazy { FirebaseStorage.getInstance().reference.child("images") }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.fragment_delete_account, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        delete_button.setOnClickListener { deleteAccount() }
    }

    private fun deleteAccount() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            context?.showToast("Current user is null")
            return
        }

        if (!checkFields()) return

        val credential = EmailAuthProvider.getCredential(
                email_edit_text.text.toString(), password_edit_text.text.toString())

        showProgress(true)
        currentUser.reauthenticate(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                deleteData(currentUser)
            } else {
                showProgress(false)
                context?.showToast("Failed to authenticate details")
            }
        }
    }

    private fun deleteData(currentUser: FirebaseUser) {
        currentMessageReference.child(currentUser.uid).removeValue().addOnCompleteListener {
            currentProfilePicReference.child(currentUser.uid).child("profile_pic.png").downloadUrl.addOnCompleteListener {
                if (it.isSuccessful) {
                    storage.getReferenceFromUrl(it.result.toString()).delete().addOnCompleteListener {
                        deleteUser(currentUser)
                    }
                } else deleteUser(currentUser)
            }
        }
    }

    private fun deleteUser(currentUser: FirebaseUser) {
        currentUserReference.child(currentUser.uid).removeValue().addOnCompleteListener {
            if (it.isSuccessful) {
                currentUser.delete().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        startActivity(Intent(activity, SignInActivity::class.java))
                        dismiss()
                        context?.showToast("Account deleted")
                        activity?.finishAffinity()
                    } else context?.showToast("Error deleting account")
                }
            } else context?.showToast("Something went wrong")
            showProgress(false)
        }
    }

    private fun checkFields(): Boolean {
        if (email_edit_text.text.isNullOrEmpty()) {
            email_edit_text.error = "Enter your email"
            return false
        }

        if (password_edit_text.text.isNullOrEmpty()) {
            email_edit_text.error = "Enter your password"
            return false
        }

        return true
    }

    private fun showProgress(show: Boolean) {
        delete_progress.visibility = if (show) View.VISIBLE else View.GONE
        email_edit_text.isEnabled = !show
        password_edit_text.isEnabled = !show
        delete_button.isEnabled = !show
    }

    override fun onDestroyView() {
        password_edit_text.setText("")
        super.onDestroyView()
    }
}