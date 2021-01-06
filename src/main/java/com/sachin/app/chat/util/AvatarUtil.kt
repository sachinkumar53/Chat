package com.sachin.app.chat.util

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.sachin.app.chat.R

object AvatarUtil {

    private val AVATARS = intArrayOf(
            R.drawable.avatar_1,
            R.drawable.avatar_2,
            R.drawable.avatar_3,
            R.drawable.avatar_4,
            R.drawable.avatar_5,
            R.drawable.avatar_6,
            R.drawable.avatar_7,
            R.drawable.avatar_8,
            R.drawable.avatar_9,
            R.drawable.avatar_10,
            R.drawable.avatar_11,
            R.drawable.avatar_12,
            R.drawable.avatar_13,
            R.drawable.avatar_14,
            R.drawable.avatar_15,
            R.drawable.avatar_16,
            R.drawable.avatar_17,
            R.drawable.avatar_18,
            R.drawable.avatar_19,
            R.drawable.avatar_20,
            R.drawable.avatar_21
    )

    //fun getRandomAvatar(): Int = AVATARS[AVATARS.indices.random()]

    fun getRandomAvatarString(): String = "avatar_${(1..21).random()}"

    fun getAvatarFromString(context: Context, avatar: String): Int =
            context.resources.getIdentifier(avatar, "drawable", context.applicationContext.packageName)

    fun isAvatarURL(url: String): Boolean = url.startsWith("avatar", true)

    fun loadAvatar(context: Context, url: String?, imageView: ImageView) {
        when {
            url == null -> Glide.with(context).load(R.drawable.default_avatar).into(imageView)

            isAvatarURL(url) -> {
                val avatar = getAvatarFromString(context, url)
                Glide.with(context).load(avatar).placeholder(R.drawable.default_avatar).into(imageView)
            }

            else -> Glide
                    .with(context)
                    .load(url)
                    .override(context.dpToPx(36))
                    .placeholder(R.drawable.default_avatar)
                    .into(imageView)
        }
    }

    fun loadAvatar(fragment: Fragment, url: String?, imageView: ImageView) =
            this.loadAvatar(fragment.requireContext(), url, imageView)

    fun loadAvatar(view: View, url: String?, imageView: ImageView) = this.loadAvatar(view.context, url, imageView)
}