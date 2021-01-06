package com.sachin.app.chat.ui

import android.content.Intent
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.sachin.app.chat.R
import com.sachin.app.chat.model.User
import com.sachin.app.chat.util.*
import kotlinx.android.synthetic.main.activity_sign_in.*

class SignInActivity : AppCompatActivity() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private lateinit var userReference: DatabaseReference
    private lateinit var googleSignInClient: GoogleSignInClient
    private val user = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        userReference = FirebaseDatabase.getInstance().reference.child("users")

        if (auth.currentUser != null)
            finish()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        google_sign_in_button.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
            showProgress(true)
        }

        sign_in_button.setOnClickListener {
            if (!checkFields()) return@setOnClickListener
            signIn()
        }


        create_account.postDelayed({
            create_account.apply {
                paint.flags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
                paint.shader = LinearGradient(0F, 0F, width.toFloat(), 0F
                        , intArrayOf(getColor(R.color.gradient_color_1), getColor(R.color.gradient_color_2)),
                        null, Shader.TileMode.CLAMP)
                invalidate()
            }
        }, 100)

        create_account.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        email_edit_text.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (email_text_input_layout.error != null) email_text_input_layout.error = null
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}
        })

        password_edit_text.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (password_text_input_layout.error != null) password_text_input_layout.error = null
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}
        })

        email_edit_text.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus)
                checkFields()
        }

        password_edit_text.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus)
                checkFields()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                user.apply {
                    name = account.displayName
                    email = account.email
                    photoUrl = account.photoUrl.toString()
                }
                //Log.w(TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
                if (e.statusCode == 7)
                    showToast("Network error")

            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "signInWithCredential:success")
                        user.uid = auth.currentUser!!.uid
                        completeSignIn(auth.currentUser!!, true)
                    } else {
                        showToast("Sign in failed")
                        showProgress(false)
                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                    }

                }
    }


    private fun showProgress(show: Boolean) {
        progress_bar.visibility = if (show) View.VISIBLE else View.GONE

        if (show) window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun signIn() {
        showProgress(true)
        auth.signInWithEmailAndPassword(
                email_edit_text.text.toString().trim(),
                password_edit_text.text.toString().trim()
        ).addOnCompleteListener {
            it.addOnCompleteListener { result ->
                if (it.isSuccessful) {
                    completeSignIn(result.result?.user!!)
                } else showToast("Incorrect user details.")

                showProgress(false)
            }
        }
    }

    private fun completeSignIn(user: FirebaseUser, isGoogleSignIn: Boolean = false) {
        val currUserReference = userReference.child(user.uid)

        currUserReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                val u = p0.getValue(User::class.java)
                if (isGoogleSignIn && u?.uid == null) {
                    currUserReference.setValue(this@SignInActivity.user)
                            .addOnCompleteListener { t ->
                                Log.d(TAG, "New user register success: ${t.isSuccessful}")
                                updateUser(user.uid)
                            }
                } else updateUser(user.uid)

                currUserReference.removeEventListener(this)
            }

            override fun onCancelled(p0: DatabaseError) {}
        })
    }

    private fun updateUser(uid: String) {
        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                userReference.child(uid).child("deviceToken").setValue(task.result?.token)
            } else {
                showToast("Device token update error")
                logError("Failed to update device token")
            }

            showProgress(false)
            startActivity(Intent(this@SignInActivity, MainActivity::class.java))
            finish()
        }

        deleteLocalUser()
        saveLocalUser(user)
    }

    private fun checkFields(): Boolean {
        if (!(email_edit_text.text?.isValidEmail()!!)) {
            email_text_input_layout.error = getText(R.string.email_error_text)
            return false
        } else email_text_input_layout.error = null

        if (!(password_edit_text.text?.isValidPassword()!!)) {
            password_text_input_layout.error = getText(R.string.password_error_text)
            return false
        } else password_text_input_layout.error = null

        return true
    }

    companion object {
        private const val TAG = "SignInActivity"
        private const val RC_SIGN_IN = 25
    }
}