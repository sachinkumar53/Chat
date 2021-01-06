package com.sachin.app.chat.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.forEachIndexed
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sachin.app.chat.R
import com.sachin.app.chat.constants.Constant
import com.sachin.app.chat.model.User
import com.sachin.app.chat.util.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val currentUser by lazy { auth.currentUser }
    private val userReference by lazy { FirebaseDatabase.getInstance().reference.child("users") }
    private lateinit var eventListener: ValueEventListener

    private val bottomIcons = intArrayOf(
            R.drawable.ic_conversation,
            R.drawable.ic_user,
            R.drawable.ic_email,
            R.drawable.ic_settings
    )

    private val titles = intArrayOf(
            R.string.title_activity_chat,
            R.string.title_activity_friends,
            R.string.title_activity_invitation,
            R.string.title_activity_settings
    )

    private var lastSelectedFragmentId = 0
    private val itemIdStack = Stack<Int>()
    private val conversationFragment by lazy { ConversationFragment() }
    private val friendFragment by lazy { FriendListFragment() }
    private val inviteFragment by lazy { InvitationFragment() }
    private val settingFragment by lazy { SettingsFragment() }

    @SuppressLint("BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!checkSetup()) return

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 787)
        }

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 454)
        }

        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${applicationContext.packageName}")))
        }

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager

        if (!pm.isIgnoringBatteryOptimizations(applicationContext.packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:" + applicationContext.packageName)
            startActivity(intent)
        }

        val dark = getPreferences().getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar_main)
        supportActionBar?.elevation = resources.dpToPx(1).toFloat()


        val shortcutsManager = SharingShortcutsManager().also { it.pushDirectShareTargets(this) }
        eventListener = object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                val currentUser = p0.getValue(User::class.java)
                FirebaseUtils.currentUser = currentUser
                currentUser?.let { updateProfileImage(it) }
            }

            override fun onCancelled(p0: DatabaseError) {}
        }

        userReference.child(currentUser?.uid!!).addValueEventListener(eventListener)

        updateProfileImage(getLocalUser())

        profile_image_main.setOnClickListener {
            val option = ActivityOptions.makeSceneTransitionAnimation(this, it,
                    getString(R.string.transition_shared_profile_image))
            startActivity(Intent(this, ProfileActivity::class.java).apply {
                putExtra(Constant.USER_UID, getLocalUser().uid)
            }, option.toBundle())
        }

        initBottomNavigation()
    }

    private fun updateProfileImage(user: User) =
            AvatarUtil.loadAvatar(this, user.photoUrl, profile_image_main)

    private fun checkSetup(): Boolean {
        if (currentUser == null) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return false
        }

        if (!isSetupComplete()) {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
            return false
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (currentUser != null)
            userReference.child(currentUser!!.uid).removeEventListener(eventListener)
    }

    private fun initBottomNavigation() {
        bottom_navigation_main.itemIconTintList = null
        selectFragment(R.id.chats)
        bottom_navigation_main.setOnNavigationItemSelectedListener {
            return@setOnNavigationItemSelectedListener if (it.itemId == lastSelectedFragmentId) false else selectFragment(it.itemId)
        }
    }

    private fun setTitle(fragment: Fragment) {
        supportActionBar?.title = when (fragment) {
            conversationFragment -> getText(titles[0])
            friendFragment -> getText(titles[1])
            inviteFragment -> getText(titles[2])
            settingFragment -> getText(titles[3])
            else -> getText(R.string.app_name)
        }
    }

    override fun onBackPressed() {
        if (itemIdStack.size < 2) {
            super.onBackPressed()
        } else {
            itemIdStack.pop()
            selectFragment(itemIdStack.peek(), false)
        }
    }

    private fun selectFragment(itemId: Int, push: Boolean = true): Boolean {
        lastSelectedFragmentId = itemId

        if (push)
            itemIdStack.push(itemId)

        updateNavigationItem(bottom_navigation_main.menu.findItem(itemId))

        return when (itemId) {
            R.id.chats -> {
                setTitle(conversationFragment)
                supportFragmentManager.beginTransaction().replace(R.id.fragment_container, conversationFragment).commit()
                true
            }

            R.id.friends -> {
                setTitle(friendFragment)
                supportFragmentManager.beginTransaction().replace(R.id.fragment_container, friendFragment).commit()
                true
            }

            R.id.invites -> {
                setTitle(inviteFragment)
                supportFragmentManager.beginTransaction().replace(R.id.fragment_container, inviteFragment).commit()
                true
            }

            R.id.settings -> {
                setTitle(settingFragment)
                supportFragmentManager.beginTransaction().replace(R.id.fragment_container, settingFragment).commit()
                true
            }

            else -> false
        }
    }

    private fun updateNavigationItem(item: MenuItem) {
        var i = 0
        bottom_navigation_main.menu.forEachIndexed { index, it ->
            if (it.itemId == item.itemId) {
                i = index
                return@forEachIndexed
            } else if (it.isChecked)
                it.isChecked = false
            it.icon = getDrawable(bottomIcons[index])!!.mutate().apply { setTint(getColor(R.color.navigation_item_unselected_color)) }
        }

        item.icon = getDrawable(bottomIcons[i])!!.mutate()
                .applyGradient(resources,
                        getColor(R.color.gradient_color_1),
                        getColor(R.color.gradient_color_2)
                )
    }
}