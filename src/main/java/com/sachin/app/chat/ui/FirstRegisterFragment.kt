package com.sachin.app.chat.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceId
import com.sachin.app.chat.R
import com.sachin.app.chat.model.User
import com.sachin.app.chat.util.*
import kotlinx.android.synthetic.main.fragment_register_first.*

class FirstRegisterFragment : Fragment() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val userReference by lazy { FirebaseDatabase.getInstance().reference.child("users") }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_register_first, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        register_button.setOnClickListener {
            if (!checkFields()) return@setOnClickListener
            register()
        }
    }

    private fun showProgress(show: Boolean) {
        if (show) requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        else requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        progress_bar_r1.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun register() {
        showProgress(true)

        val name = name_edit_text.text?.trim().toString()
        val email = email_edit_text.text?.trim().toString()

        auth.createUserWithEmailAndPassword(email,
                password_edit_text.text?.trim().toString()
        ).addOnSuccessListener {
            FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { token ->
                if (token.isSuccessful) {
                    val deviceToken = token.result?.token
                    val uid = auth.currentUser!!.uid
                    setupNewUser(name, email, uid, deviceToken!!)
                }
            }
        }.addOnFailureListener {
            showProgress(false)
            context?.showToast("Authentication failed!")
        }
    }

    private fun setupNewUser(name: String, email: String, uid: String, deviceToken: String) {
        val avatar = AvatarUtil.getRandomAvatarString()
        val user = User(name, email, avatar, uid = uid, deviceToken = deviceToken)

        userReference.child(uid).setValue(user)
                .addOnCompleteListener { task ->
                    showProgress(false)
                    if (!task.isSuccessful)
                        context?.showToast("Some error has been occur")

                    findNavController().navigate(R.id.secondFragment)
                    with(requireContext()) {
                        deleteLocalUser()
                        saveLocalUser(user)
                        setSetupComplete(true)
                    }
                }
    }

    private fun checkFields(): Boolean {
        if (!(email_edit_text.text?.isValidEmail()!!)) {
            email_edit_text.error = getText(R.string.email_error_text)
            return false
        }

        if (!(password_edit_text.text?.isValidPassword()!!)) {
            password_edit_text.error = getText(R.string.password_error_text)
            return false
        }

        if (password_edit_text.text.toString() != confirm_password_edit_text.text.toString()) {
            confirm_password_edit_text.error = getText(R.string.password_error_text)
            return false
        }

        if (name_edit_text.text.isNullOrBlank() || name_edit_text.text.isNullOrEmpty())
            return false

        return true
    }
}