package com.sachin.app.chat.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.provider.MediaStore
import android.util.AttributeSet
import android.util.Log
import android.util.Patterns
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.annotation.StyleableRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.sachin.app.chat.model.User
import com.sachin.app.chat.constants.Constant.PICK_IMAGE

fun CharSequence?.isValidEmail() = !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this!!).matches()
fun CharSequence?.isValidPassword() = !isNullOrEmpty() && this?.length!! >= 8

fun Context.showToast(msg: String, duration: Int = Toast.LENGTH_SHORT) = Toast.makeText(this, msg, duration).show()

fun Context.getPreferences(): SharedPreferences = getSharedPreferences("chat_app", Context.MODE_PRIVATE)

fun Context.setName(name: String) = getPreferences().putString("user_name", name)
fun Context.setAbout(name: String) = getPreferences().putString("user_about", name)

fun Context.getName(): String = getPreferences().getString("user_name", "")!!
fun Context.getAbout(): String = getPreferences().getString("user_about", "Hey! I'm using this app")!!

fun Context.setSetupComplete(value: Boolean) = getPreferences().putBoolean("setup_complete", value)
fun Context.isSetupComplete(): Boolean = getPreferences().getBoolean("setup_complete", true)

fun Context.saveLocalUser(user: User) {
    getPreferences().putString("user_name", user.name ?: "")
    getPreferences().putString("user_about", user.about ?: "Hey! I'm using this app")
    getPreferences().putString("user_uid", user.uid ?: "")
    getPreferences().putString("user_email", user.email ?: "")
    getPreferences().putString("photo_url", user.photoUrl ?: "")
    getPreferences().putString("device_token", user.deviceToken ?: "")
    //getPreferences().putLong("time_stamp", user.timeStamp)
}

fun Context.deleteLocalUser() {
    getPreferences().edit().remove("user_name")
    getPreferences().edit().remove("user_about")
    getPreferences().edit().remove("user_uid")
    getPreferences().edit().remove("user_email")
    getPreferences().edit().remove("photo_url")
    getPreferences().edit().remove("device_token")
    //getPreferences().putLong("time_stamp", user.timeStamp)
}

fun Context.getLocalUser(): User {
    val name = getPreferences().getString("user_name", "")
    val about = getPreferences().getString("user_about", "Hey! I'm using this app")
    val uid = getPreferences().getString("user_uid", "")
    val email = getPreferences().getString("user_email", "")
    val url = getPreferences().getString("photo_url", "")
    val token = getPreferences().getString("device_token", "")
    return User(name, email, url, about, uid!!, token)
}


fun Resources.dpToPx(i: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, i.toFloat(), displayMetrics).toInt()

fun Context.dpToPx(i: Int): Int = resources.dpToPx(i)
fun View.dpToPx(i: Int): Int = resources.dpToPx(i)

fun Any.logError(exception: Exception) = Log.e(this::class.java.simpleName, exception.message, exception)
fun Any.logError(msg: String) = Log.e(this::class.java.simpleName, msg)
fun Any.logError(msg: String, exception: Exception) = Log.e(this::class.java.simpleName, msg, exception)
fun Any.logError(msg: String, t: Throwable) = Log.e(this::class.java.simpleName, msg, t)
fun Any.logWarn(msg: String) = Log.w(this::class.java.simpleName, msg)

fun SharedPreferences.putString(key: String, value: String) = edit().putString(key, value).commit()
fun SharedPreferences.putInt(key: String, value: Int) = edit().putInt(key, value).commit()
fun SharedPreferences.putBoolean(key: String, value: Boolean) = edit().putBoolean(key, value).apply()
fun SharedPreferences.putLong(key: String, value: Long) = edit().putLong(key, value).apply()

inline fun View.withStyleableRes(set: AttributeSet?, @StyleableRes attrs: IntArray, init: TypedArray.() -> Unit) {
    val typedArray = context.theme.obtainStyledAttributes(set, attrs, 0, 0)
    try {
        typedArray.init()
    } finally {
        typedArray.recycle()
    }

}

fun Fragment.showToast(msg: String, duration: Int = Toast.LENGTH_SHORT) {
    context?.let { it.showToast(msg, duration) }
}

fun FragmentActivity.toggleFragment(fragment: Fragment) {
    if (!fragment.isHidden) supportFragmentManager.beginTransaction().hide(fragment).commit()
    else supportFragmentManager.beginTransaction().show(fragment).commit()
}

fun FragmentActivity.hideFragment(fragment: Fragment) {
    if (!fragment.isHidden)
        supportFragmentManager.beginTransaction().hide(fragment).commit()
}

fun FragmentActivity.showFragment(fragment: Fragment) {
    if (fragment.isHidden)
        supportFragmentManager.beginTransaction().show(fragment).commit()
}

fun FragmentActivity.findFragmentById(@IdRes id: Int) = supportFragmentManager.findFragmentById(id)

fun Activity.startImagePicker() {
    val intent = Intent(Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    intent.type = "image/*"
    startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE)
}

fun RecyclerView.smoothScrollToEnd() {
    (adapter?.itemCount ?: 0 - 1).takeIf { i -> i > 0 }
            ?.let(this::smoothScrollToPosition)
}

fun View.isVisible(): Boolean = this.visibility == View.VISIBLE

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.hide(gone: Boolean = true) {
    this.visibility = if (gone) View.GONE else View.INVISIBLE
}

fun Drawable.applyGradient(resources: Resources, @ColorInt vararg colors: Int): Drawable {
    val w = intrinsicWidth
    val h = intrinsicHeight

    val gradient = LinearGradient(0F, 0F, w.toFloat(), 0F, colors, null, Shader.TileMode.CLAMP)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint().apply {
        isDither = false
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        shader = gradient
    }

    setBounds(0, 0, w, h)
    draw(canvas)
    canvas.drawRect(0F, 0F, w.toFloat(), h.toFloat(), paint)
    return BitmapDrawable(resources, bitmap)
}