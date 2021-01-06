package com.sachin.app.chat.widget

import android.animation.TimeAnimator
import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable

class GradientProgress(private val pixelsPerSecond: Float) : Drawable(), Animatable, TimeAnimator.TimeListener {
    private val paint = Paint()
    private var x: Float = 0.toFloat()
    private val animator = TimeAnimator()

    var color1 = Color.RED
    var color2 = Color.BLUE

    init {
        animator.setTimeListener(this)
    }

    override fun onBoundsChange(bounds: Rect) {
        paint.shader = LinearGradient(0f, 0f, bounds.width().toFloat(), 0f, color1, color2, Shader.TileMode.MIRROR)
    }

    override fun draw(canvas: Canvas) {
        canvas.clipRect(bounds)
        canvas.translate(x, 0f)
        canvas.drawPaint(paint)
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun start() {
        animator.start()
    }

    override fun stop() {
        animator.cancel()
    }

    override fun isRunning(): Boolean = animator.isRunning

    override fun onTimeUpdate(animation: TimeAnimator, totalTime: Long, deltaTime: Long) {
        x = pixelsPerSecond * totalTime / 1000
        invalidateSelf()
    }
}