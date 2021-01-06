package com.sachin.android.widget

import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Shader
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.sachin.app.chat.R

class GradientTextView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    var colors = intArrayOf(Color.BLUE, Color.MAGENTA, Color.RED)
    private var shader = LinearGradient(0F, 0F, 600F, 600F, colors, null, Shader.TileMode.MIRROR)

    private val localMatrix = Matrix()
    private var rotate = 0F

    init {

        val a = context.obtainStyledAttributes(attrs, R.styleable.GradientTextView, defStyleAttr, 0)
        colors = textToIntArray(a.getTextArray(R.styleable.GradientTextView_colors))
        rotate = a.getFloat(R.styleable.GradientTextView_rotate, 0F)

        a.recycle()

        localMatrix.setRotate(rotate)

        shader.setLocalMatrix(localMatrix)
    }

    private fun textToIntArray(textArray: Array<CharSequence>?): IntArray {
        val temp = ArrayList<Int>()
        textArray?.forEach {
            temp.add(Color.parseColor(it.toString()))
        }

        return temp.toIntArray()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        shader = LinearGradient(
                0F,
                0F,
                w.div(2).toFloat(),
                h.div(2).toFloat(),
                colors,
                null,
                Shader.TileMode.MIRROR
        )

        paint.shader = shader
    }
}