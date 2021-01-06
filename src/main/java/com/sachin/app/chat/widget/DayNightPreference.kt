package com.sachin.app.chat.widget

import android.content.Context
import android.util.AttributeSet
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import com.sachin.app.chat.R
import com.shrikanthravi.library.NightModeButton

class DayNightPreference @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : SwitchPreferenceCompat(context, attrs, defStyleAttr, R.style.SwitchPreference) {

    init {
        widgetLayoutResource = R.layout.preference_day_night_switch
    }


    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        if (holder == null) return

        val dayNightSwitch = holder.itemView.findViewById<NightModeButton>(R.id.day_night_switch)
        dayNightSwitch.setOnSwitchListener(null)
        //dayNightSwitch.setDayChecked(isChecked)
        dayNightSwitch.setOnSwitchListener {
            if (!callChangeListener(it)) {
                //view.setDayChecked(!it)
                return@setOnSwitchListener
            }

            isChecked = it
        }
    }
}