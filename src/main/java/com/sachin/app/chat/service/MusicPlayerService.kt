package com.sachin.app.chat.service

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
import android.view.WindowManager.LayoutParams.TYPE_PHONE
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.plattysoft.leonids.ParticleSystem
import com.sachin.app.chat.R
import com.sachin.app.chat.util.dpToPx
import kotlin.math.roundToInt


class MusicPlayerService : Service() {
    private val windowManager by lazy { getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    private val totalViewWidth by lazy { dpToPx(100) }

    private val params by lazy {
        WindowManager.LayoutParams(totalViewWidth, totalViewWidth,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    TYPE_APPLICATION_OVERLAY
                else TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSPARENT)
    }

    private val frameLayout by lazy { FrameLayout(this) }
    private lateinit var view: View

    private var initialX = 0
    private var initialY = 0

    var initialTouchX = 0F
    var initialTouchY = 0F

    var dragging = false

    private val screenWidth by lazy { resources.displayMetrics.widthPixels }
    private val screenHeight by lazy { resources.displayMetrics.heightPixels }

    private var usableScreenHeight = 0

    private var viewWidth = 0
    private var viewHeight = 0

    private var edgeLeft = 0
    private var edgeRight = 0

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        setTheme(R.style.Theme_AppCompat)

        frameLayout.clipChildren = false

        view = LayoutInflater.from(this).inflate(R.layout.floating_audio_player, frameLayout)

        val gestureListener = GestureListener()
        val gestureDetector = GestureDetector(this, gestureListener)

        view.setOnTouchListener { _, event ->
            val detectedUp = event.action === MotionEvent.ACTION_UP

            if (!gestureDetector.onTouchEvent(event) && detectedUp) {
                gestureListener.onUpEvent(event)
                return@setOnTouchListener true
            }

            gestureDetector.onTouchEvent(event)
        }

        /*view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y

                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).roundToInt()
                    val dy = (event.rawY - initialTouchY).roundToInt()

                    if (dx.absoluteValue <= 5 || dy.absoluteValue <= 5) {
                        v.performClick()
                        v.animate().scaleX(1F).scaleY(1F).setInterpolator(AccelerateDecelerateInterpolator()).setDuration(150).start()
-                        dragging = false
                        return@setOnTouchListener true
                    }

                    //Calculate the X and Y coordinates of the view.
                    params.x = initialX + dx
                    params.y = initialY + dy

                    if (!dragging) {
                        v.animate().scaleX(0.8F).scaleY(0.8F).setInterpolator(AccelerateInterpolator()).setDuration(150).start()
                        dragging = true
                    }

                    //Update the layout with new X & Y coordinates
                    windowManager.updateViewLayout(view, params);
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val middle = edgeX / 2

                    params.x = if (params.x >= middle) (edgeX + 62) else -62

                    if (dragging) {
                        v.animate().scaleX(1F).scaleY(1F).setInterpolator(AccelerateDecelerateInterpolator()).setDuration(150).start()
                        dragging = false
                    }

                    windowManager.updateViewLayout(view, params)
                    true
                }

                else -> false
            }
        }*/

        viewWidth = resources.getDimensionPixelSize(R.dimen.floating_widget_size)

        edgeRight = screenWidth - (totalViewWidth + viewWidth) / 2
        edgeLeft = -(totalViewWidth - viewWidth) / 2

        Log.w("SAC", "screenWidth = $screenWidth  screenHeight = $screenHeight")
        Log.w("SAC", "viewWidth = $viewWidth  viewHeight = $viewHeight")
        Log.w("SAC", "totalViewWidth = $totalViewWidth  totalViewHeight = $totalViewWidth")
        Log.w("SAC", "edgeLeft = $edgeLeft  edgeRight = $edgeRight")

        usableScreenHeight = screenHeight - statusBarHeight()

        params.run {
            gravity = Gravity.START or Gravity.TOP
            x = 0
            y = 100
        }

        windowManager.addView(frameLayout, params)
    }

    private fun statusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else dpToPx(25)
    }

    private fun navigationBarHeight(): Int {
        if (hasNavigationBar()) {
            val id = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            return if (id > 0) resources.getDimensionPixelSize(id) else dpToPx(48)
        }
        return 0
    }

    private fun hasNavigationBar(): Boolean {
        val hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)
        val hasHomeKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME)
        val id: Int = resources.getIdentifier("config_showNavigationBar", "bool", "android")
        return !hasBackKey && !hasHomeKey || id > 0 && resources.getBoolean(id)
    }

    override fun onDestroy() {
        super.onDestroy()

        windowManager.removeViewImmediate(frameLayout)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        private const val URL = "https://storage.googleapis.com/exoplayer-test-media-0/play.mp3"
    }

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            if (e == null) return false

            val d = getDrawable(R.drawable.circle)!!

            ParticleSystem(frameLayout, 8, d, 500)
                    .setScaleRange(0.4F, 0.8F)
                    .setFadeOut(400, DecelerateInterpolator())
                    .setSpeedRange(0.05F, 0.1F)
                    .oneShot(view, 8, DecelerateInterpolator())

            view.animateClick()
            return true
        }

        override fun onDown(e: MotionEvent?): Boolean {
            if (e == null) return false

            initialX = params.x
            initialY = params.y

            initialTouchX = e.rawX
            initialTouchY = e.rawY

            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            return super.onFling(e1, e2, velocityX, velocityY)
        }

        override fun onDoubleTap(e: MotionEvent?): Boolean {
            return super.onDoubleTap(e)
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            if (e1 == null || e2 == null) return false

            val dx = (e2.rawX - e1.rawX).roundToInt()
            val dy = (e2.rawY - e1.rawY).roundToInt()

            params.x = initialX + dx
            params.y = initialY + dy

            windowManager.updateViewLayout(frameLayout, params)
            return true
        }

        fun onUpEvent(e: MotionEvent) {
            val middleX = (screenWidth - viewWidth) / 2

            params.x = if (params.x >= middleX) edgeRight else edgeLeft

            val edgeTop = -(totalViewWidth - viewHeight) / 2
            val edgeBottom = (screenHeight - totalViewWidth)

            params.y = when {
                params.y >= (screenHeight - totalViewWidth + 0) -> edgeBottom
                params.y <= edgeTop -> edgeTop
                else -> params.y
            }

            windowManager.updateViewLayout(frameLayout, params)
        }

        override fun onContextClick(e: MotionEvent?): Boolean {
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            return super.onSingleTapConfirmed(e)
        }

        override fun onShowPress(e: MotionEvent?) {
            super.onShowPress(e)
        }

        override fun onDoubleTapEvent(e: MotionEvent?): Boolean {
            return super.onDoubleTapEvent(e)
        }

        override fun onLongPress(e: MotionEvent?) {
            super.onLongPress(e)
        }
    }
}

private fun View.animateClick() {
    animate().scaleX(0.85F)
            .scaleY(0.85F)
            .setDuration(100)
            .setInterpolator(AccelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    animate().scaleX(1F)
                            .scaleY(1F)
                            .setDuration(100)
                            .setInterpolator(AccelerateDecelerateInterpolator())
                            .setListener(null)
                            .start()
                }
            }).start()
}
