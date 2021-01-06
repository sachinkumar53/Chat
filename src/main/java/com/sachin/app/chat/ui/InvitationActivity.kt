package com.sachin.app.chat.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.sachin.app.chat.R
import com.sachin.app.chat.adapter.InvitationPagerAdapter
import kotlinx.android.synthetic.main.activity_invitation.*

class InvitationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invitation)

        view_pager.adapter = InvitationPagerAdapter(this)

        TabLayoutMediator(tab_layout, view_pager) { tab, position ->
            tab.text = getText(TAB_TITLES[position])
        }.attach()
    }

    companion object {
        private val TAB_TITLES = arrayOf(
                R.string.tab_text_received,
                R.string.tab_text_sent
        )
    }
}
