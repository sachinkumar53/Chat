package com.sachin.app.chat.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.sachin.app.chat.R
import com.sachin.app.chat.util.getPreferences
import com.sachin.app.chat.util.putBoolean
import com.sachin.app.chat.widget.SwitchPreference

class SettingsFragment : PreferenceFragmentCompat() {
    private val darkModeSwitch by lazy { findPreference<SwitchPreference>(getString(R.string.pref_dark_mode_key))!! }
    private val aboutAppPreference by lazy { findPreference<Preference>(getString(R.string.pref_about_app_key))!! }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_main, rootKey)

        darkModeSwitch.setOnPreferenceChangeListener { _, newValue ->
            val darkMode = (newValue as Boolean)
            AppCompatDelegate.setDefaultNightMode(if (darkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
            requireContext().getPreferences().putBoolean(getString(R.string.pref_dark_mode_key), darkMode)
            true
        }

        aboutAppPreference.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), CheckUpdateActivity::class.java))
            true
        }
    }
}