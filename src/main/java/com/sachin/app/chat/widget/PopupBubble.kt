package com.sachin.app.chat.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sachin.app.chat.R
import com.sachin.app.chat.util.smoothScrollToEnd
import kotlinx.android.synthetic.main.popup_bubble.view.*
import org.jetbrains.annotations.NotNull

class PopupBubble @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null)
    : FrameLayout(context, attrs) {
    private var isAnimating = false
    private var firstVisibleIndex = -1
    private var lastVisibleIndex = -1

    init {
        setBackgroundResource(R.drawable.popup_bubble_background)
        outlineProvider = ViewOutlineProvider.BACKGROUND
        elevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2F, resources.displayMetrics)
        visibility = View.GONE
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        View.inflate(context, R.layout.popup_bubble, this)
    }

    fun attachWithRecyclerView(recyclerView: RecyclerView) {
        val lm = (recyclerView.layoutManager as LinearLayoutManager)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (firstVisibleIndex == -1 || lastVisibleIndex == -1) {
                        firstVisibleIndex = lm.findFirstCompletelyVisibleItemPosition()
                        lastVisibleIndex = lm.findLastCompletelyVisibleItemPosition()
                    }
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val currentLastIndex = lm.findLastVisibleItemPosition()
                if (currentLastIndex <= firstVisibleIndex && !isPopupShown())
                    showPopup()
                else if (currentLastIndex >= lastVisibleIndex)
                    hidePopup()
            }
        })

        popup_bubble_view.setOnClickListener {
            recyclerView.smoothScrollToEnd()
        }
    }

    fun isPopupShown(): Boolean = visibility == View.VISIBLE

    fun showPopup(animate: Boolean = true) {
        if (animate) {
            if (isAnimating) return
            isAnimating = true
            visibility = View.VISIBLE
            popup_bubble_view.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            val w = popup_bubble_view.measuredWidth
            val h = popup_bubble_view.measuredHeight
            val animator = ValueAnimator.ofInt(0, w)
            animator.addUpdateListener {
                popup_bubble_view.layoutParams.apply {
                    width = it.animatedValue as Int
                    height = (h * it.animatedFraction).toInt()
                }
                popup_bubble_view.requestLayout()
            }
            with(animator, {
                doOnEnd {
                    isAnimating = false
                    popup_bubble_text.visibility = View.VISIBLE
                    popup_bubble_icon.visibility = View.VISIBLE
                }
                duration = ANIM_DURATION
                startDelay = START_DELAY
                interpolator = DecelerateInterpolator()
            })
            animator.start()
        } else visibility = View.VISIBLE
    }

    fun hidePopup(animate: Boolean = true) {
        if (animate) {
            if (isAnimating) return
            isAnimating = true
            //visibility = View.VISIBLE
            popup_bubble_view.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            val w = popup_bubble_view.measuredWidth
            val h = popup_bubble_view.measuredHeight
            val animator = ValueAnimator.ofInt(w, h)
            animator.addUpdateListener {
                popup_bubble_view.layoutParams.apply {
                    width = it.animatedValue as Int
                    height = (h * (1f - it.animatedFraction)).toInt()
                }
                popup_bubble_view.requestLayout()
            }
            with(animator, {
                doOnStart {
                    popup_bubble_text.visibility = View.INVISIBLE
                    popup_bubble_icon.visibility = View.INVISIBLE
                }

                doOnEnd {
                    isAnimating = false
                    visibility = View.GONE
                }
                duration = ANIM_DURATION
                startDelay = START_DELAY
                interpolator = DecelerateInterpolator()
            })
            animator.start()
        } else popup_bubble_view.visibility = View.GONE
    }

    fun setText(text: String) {
        popup_bubble_text.text = text
    }

    private fun setIcon(@DrawableRes iconResId: Int) {
        popup_bubble_icon.setImageResource(iconResId)
    }

    private fun setIcon(@NotNull icon: Drawable) {
        popup_bubble_icon.setImageDrawable(icon)
    }

    fun setNewMessage(count: Int = 1) {
        popup_bubble_icon.visibility = View.GONE
        if (count > 1)
            popup_bubble_text.text = "$count new messages"
        else popup_bubble_text.text = "$count new message"
    }

    companion object {
        private const val ANIM_DURATION = 350L
        private const val START_DELAY = 100L
    }
}