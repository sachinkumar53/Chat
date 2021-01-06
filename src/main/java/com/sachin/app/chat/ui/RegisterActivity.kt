package com.sachin.app.chat.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.sachin.app.chat.R
import com.sachin.app.chat.model.User
import com.sachin.app.chat.util.getPreferences
import com.sachin.app.chat.util.isSetupComplete
import kotlinx.android.synthetic.main.activity_register.*

class RegisterActivity : AppCompatActivity() {
    val currUser by lazy { User() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val navHostFragment = nav_host_fragment as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.nav_graph_register)

        graph.startDestination = if (isSetupComplete()) R.id.firstFragment else R.id.secondFragment
        navHostFragment.navController.graph = graph
    }
}
