package com.sachin.app.chat.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.navigation.findNavController
import com.sachin.app.chat.R
import kotlinx.android.synthetic.main.activity_user_list.*

class UserListActivity : AppCompatActivity() {
    internal lateinit var searchView: SearchView
    internal val progressBar by lazy { search_progress_bar }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)
        setSupportActionBar(toolbar_user)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_users, menu)
        searchView = menu?.findItem(R.id.action_search)?.actionView as SearchView

        searchView.setOnCloseListener {
            findNavController(R.id.nav_host_user_fragment).navigateUp()
            true
        }

        searchView.setOnSearchClickListener {
            findNavController(R.id.nav_host_user_fragment).navigate(R.id.userInviteFragment)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                if (item.isActionViewExpanded)
                    item.collapseActionView()
                true
            }

            R.id.action_invitation -> {
                startActivity(Intent(this, InvitationActivity::class.java))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}
