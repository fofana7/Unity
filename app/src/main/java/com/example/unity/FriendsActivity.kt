package com.example.unity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class FriendsActivity : AppCompatActivity() {

    private lateinit var adapter: FriendAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var progressBar: ProgressBar
    private var myFriends = mutableListOf<UserResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_friends)

        sessionManager = SessionManager(this)
        progressBar = findViewById(R.id.progressBar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        val rvFriends = findViewById<RecyclerView>(R.id.rvFriends)
        rvFriends.layoutManager = LinearLayoutManager(this)
        
        adapter = FriendAdapter(mutableListOf()) { user ->
            handleFriendAction(user)
        }
        rvFriends.adapter = adapter

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> loadMyFriends()
                    1 -> loadSuggestions()
                    2 -> loadRequests()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        setupBottomNavigation()
        
        loadMyFriends()
    }

    private fun loadMyFriends() {
        val token = sessionManager.fetchAuthToken() ?: return
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getMyFriends("Bearer $token")
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val wrapper = response.body()!!
                    myFriends = (wrapper.friends ?: emptyList()).toMutableList()
                    adapter.updateData(myFriends)
                    
                    if (myFriends.isEmpty()) {
                        Toast.makeText(this@FriendsActivity, "Vous n'avez pas encore d'amis. Allez dans 'Suggestions' !", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Log.e("FRIENDS", "Erreur amis", e)
            }
        }
    }

    private fun loadSuggestions() {
        val token = sessionManager.fetchAuthToken() ?: return
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getAllUsers("Bearer $token")
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val wrapper = response.body()!!
                    val myId = sessionManager.fetchUserId()
                    val suggestions = (wrapper.users ?: emptyList()).filter { it.id != myId }
                    adapter.updateData(suggestions)
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Log.e("FRIENDS", "Erreur suggestions", e)
            }
        }
    }

    private fun loadRequests() {
        adapter.updateData(emptyList())
        Toast.makeText(this, "Bientôt disponible", Toast.LENGTH_SHORT).show()
    }

    private fun handleFriendAction(user: UserResponse) {
        val token = sessionManager.fetchAuthToken() ?: return
        lifecycleScope.launch {
            try {
                val request = FriendActionRequest(friendId = user.id)
                val response = RetrofitClient.instance.addFriend("Bearer $token", request)
                if (response.isSuccessful) {
                    Toast.makeText(this@FriendsActivity, "Demande envoyée !", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("FRIENDS", "Erreur action", e)
            }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_friends
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, DashboardActivity::class.java)); finish(); true }
                R.id.nav_friends -> true
                R.id.nav_messages -> { startActivity(Intent(this, MessagesActivity::class.java)); finish(); true }
                R.id.nav_profile -> { startActivity(Intent(this, ProfileActivity::class.java)); finish(); true }
                else -> false
            }
        }
    }
}
