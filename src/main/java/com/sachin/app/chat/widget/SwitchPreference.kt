package com.sachin.app.chat.widget

import android.content.Context
import android.util.AttributeSet
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import com.sachin.app.chat.R
import com.suke.widget.SwitchButton

class SwitchPreference @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : SwitchPreferenceCompat(context, attrs, defStyleAttr, R.style.SwitchPreference) {

    init {
        widgetLayoutResource = R.layout.preference_switch
    }


    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        if (holder == null) return

        val switchButton = holder.itemView.findViewById<SwitchButton>(R.id.switchButton)
        switchButton.setOnCheckedChangeListener(null)
        switchButton.isChecked = isChecked
        switchButton.setOnCheckedChangeListener { view, isChecked ->
            if (!callChangeListener(isChecked)) {
                view.isChecked = !isChecked
                return@setOnCheckedChangeListener
            }

            this.isChecked = isChecked
        }
    }
}