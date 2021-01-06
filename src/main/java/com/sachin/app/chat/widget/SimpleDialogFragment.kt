package com.sachin.app.chat.widget

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.sachin.app.chat.R
import com.sachin.app.chat.util.dpToPx
import kotlinx.android.synthetic.main.simple_dialog_layout.*

class SimpleDialogFragment : DialogFragment() {
    private var title: String = ""
    private var message: String = ""
    private var positiveButtonText: String = ""
    private var negativeButtonText: String = ""
    private var onPositiveClickListener: (DialogInterface) -> Unit = {}
    private var onNegativeClickListener: (DialogInterface) -> Unit = {}
    private var positiveButtonTextColor: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.simple_dialog_layout, container, false)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.run {
            setBackgroundDrawableResource(R.drawable.popup_menu_background)
            attributes = attributes.apply {
                gravity = Gravity.BOTTOM
                y = context.dpToPx(16)
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog_title.text = title
        dialog_message.text = message

        dialog_button_positive.run {
            text = positiveButtonText
            positiveButtonTextColor?.let { setTextColor(it) }
            dialog_button_positive.setOnClickListener {
                onPositiveClickListener(requireDialog())
                dismiss()
            }
        }

        dialog_button_negative.run {
            text = negativeButtonText
            setOnClickListener {
                onNegativeClickListener(requireDialog())
                dismiss()
            }
        }
    }

    fun setTitle(title: String): DialogFragment {
        this.title = title
        return this
    }

    fun setMessage(message: String): DialogFragment {
        this.message = message
        return this
    }

    fun setPositiveButtonTextColor(@ColorInt color: Int) {
        positiveButtonTextColor = color
    }

    fun setPositiveButton(text: String, onClickListener: (DialogInterface) -> Unit = {}): DialogFragment {
        positiveButtonText = text
        onPositiveClickListener = onClickListener
        return this
    }

    fun setNegativeButton(text: String, onClickListener: (DialogInterface) -> Unit = {}): DialogFragment {
        negativeButtonText = text
        onNegativeClickListener = onClickListener
        return this
    }

    fun show(activity: AppCompatActivity) {
        show(activity.supportFragmentManager, TAG)
    }

    fun show(fragment: Fragment) {
        show(fragment.parentFragmentManager, TAG)
    }

    companion object {
        private const val TAG = "SimpleDialog"
    }
}