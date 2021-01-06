package com.sachin.app.chat.ui

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.sachin.app.chat.R
import com.sachin.app.chat.adapter.FriendListAdapter
import com.sachin.app.chat.database.AppDatabase
import com.sachin.app.chat.database.FriendDao
import com.sachin.app.chat.constants.Constant
import kotlinx.android.synthetic.main.activity_friend_chooser.*

class FriendChooserActivity : AppCompatActivity() {
    private val friendListAdapter by lazy { FriendListAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_chooser)

        val friendDao = AppDatabase.getDatabase(this).friendDao()
        LoadFriends(friendDao).execute()

        friend_list.apply {
            layoutManager = LinearLayoutManager(this@FriendChooserActivity)
            adapter = friendListAdapter
        }

        friendListAdapter.setOnFriendClickListener {
            val data = Intent()
            data.putExtra(Constant.USER_UID, it.uid)
            setResult(RESULT_OK, data)
            finish()
        }


    }

    private inner class LoadFriends(private val friendDao: FriendDao) : AsyncTask<Void?, Void?, Void?>() {
        override fun doInBackground(vararg params: Void?): Void? {
            val friends = friendDao.getAllFriends()
            friendListAdapter.setFriends(friends)
            return null
        }
    }
}