package com.sachin.app.chat.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.sachin.app.chat.R
import com.sachin.app.chat.database.AppDatabase
import com.sachin.app.chat.model.Friend
import com.sachin.app.chat.ui.ChatActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class SharingShortcutsManager {
    val categoryTextShareTarget = "com.sachin.app.chat.category.TEXT_SHARE_TARGET"

    private val maxShortcuts = 4

    fun pushDirectShareTargets(context: Context) {
        val shortcuts = ArrayList<ShortcutInfoCompat>()

        // Category that our sharing shortcuts will be assigned to
        val contactCategories = setOf(categoryTextShareTarget)

        GlobalScope.launch {
            val friendList = AppDatabase.getDatabase(context).friendDao().getAllFriends()
            // Adding maximum number of shortcuts to the list
            for (index in friendList.indices) {
                if (index == (maxShortcuts - 1))
                    break

                val friend = friendList[index]

                // Item that will be sent if the shortcut is opened as a static launcher shortcut
                val staticLauncherShortcutIntent = Intent(Intent.ACTION_SEND)
                staticLauncherShortcutIntent.component = ComponentName(context.applicationContext.packageName + ".ui", ChatActivity::class.java.canonicalName!!)

                if (friend.photoUrl != null) {
                    if (AvatarUtil.isAvatarURL(friend.photoUrl!!)) {
                        shortcuts.add(
                                ShortcutInfoCompat.Builder(context, friend.uid)
                                        .setShortLabel(friend.name!!)
                                        .setIcon(IconCompat.createWithResource(context, AvatarUtil.getAvatarFromString(context, friend.photoUrl!!)))
                                        .setIntent(staticLauncherShortcutIntent)
                                        .setLongLived(true)
                                        .setCategories(contactCategories)
                                        .setPerson(Person.Builder().setName(friend.name).build())
                                        .build()
                        )
                    } else {
                        Glide.with(context).asBitmap().load(friend.photoUrl).override(context.dpToPx(36))
                                .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                shortcuts.add(
                                        ShortcutInfoCompat.Builder(context, friend.uid)
                                                .setShortLabel(friend.name!!)
                                                .setIcon(IconCompat.createWithBitmap(resource))
                                                .setIntent(staticLauncherShortcutIntent)
                                                .setLongLived(true)
                                                .setCategories(contactCategories)
                                                .setPerson(Person.Builder().setName(friend.name).build())
                                                .build()
                                )
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {}
                        })
                    }
                } else {
                    // Creates a new Sharing Shortcut and adds it to the list
                    // The id passed in the constructor will become EXTRA_SHORTCUT_ID in the received Intent
                    shortcuts.add(
                            ShortcutInfoCompat.Builder(context, friend.uid)
                                    .setShortLabel(friend.name!!)
                                    // Icon that will be displayed in the share target
                                    .setIcon(IconCompat.createWithResource(context, R.drawable.default_avatar))
                                    .setIntent(staticLauncherShortcutIntent)
                                    // Make this sharing shortcut cached by the system
                                    // Even if it is unpublished, it can still appear on the sharesheet
                                    .setLongLived(true)
                                    .setCategories(contactCategories)
                                    // Person objects are used to give better suggestions
                                    .setPerson(Person.Builder().setName(friend.name).build())
                                    .build()
                    )
                }
            }

            ShortcutManagerCompat.addDynamicShortcuts(context, shortcuts)
        }
    }

    /**
     * Remove all dynamic shortcuts
     */
    fun removeAllDirectShareTargets(context: Context) {
        ShortcutManagerCompat.removeAllDynamicShortcuts(context)
    }
}
