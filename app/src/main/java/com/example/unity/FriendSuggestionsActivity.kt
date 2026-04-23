package com.example.unity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class FriendSuggestionsActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var progressBar: ProgressBar
    private lateinit var rvSuggestions: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: FriendSuggestionsAdapter

    private var allUsers: List<UserResponse> = emptyList()
    private var myFriends: List<UserResponse> = emptyList()
    private var currentUser: UserResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_friend_suggestions)

        sessionManager = SessionManager(this)
        
        val topBar = findViewById<View>(R.id.topBar)
        if (topBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(topBar) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
                insets
            }
        }

        findViewById<ImageButton>(R.id.btnBack)?.setOnClickListener { finish() }

        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        rvSuggestions = findViewById(R.id.rvSuggestions)

        adapter = FriendSuggestionsAdapter(emptyList(), emptySet(), sessionManager.fetchUserId()) { user -> addFriend(user) }
        rvSuggestions.layoutManager = LinearLayoutManager(this)
        rvSuggestions.adapter = adapter

        fetchData()
    }

    private fun fetchData() {
        val token = sessionManager.fetchAuthToken() ?: return
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val meRes = RetrofitClient.instance.getMe("Bearer $token")
                if (meRes.isSuccessful) currentUser = meRes.body()?.user

                val friendsRes = RetrofitClient.instance.getMyFriends("Bearer $token")
                if (friendsRes.isSuccessful) myFriends = friendsRes.body() ?: emptyList()

                val allUsersRes = RetrofitClient.instance.getAllUsers("Bearer $token")
                if (allUsersRes.isSuccessful) allUsers = allUsersRes.body() ?: emptyList()

                filterAndDisplaySuggestions()
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun filterAndDisplaySuggestions() {
        progressBar.visibility = View.GONE
        val friendIds = myFriends.mapNotNull { it.id }.toSet()
        val currentUserId = currentUser?.id ?: sessionManager.fetchUserId()

        val suggestions = allUsers.filter { it.id != null && it.id != currentUserId && !friendIds.contains(it.id) }

        if (suggestions.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvSuggestions.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvSuggestions.visibility = View.VISIBLE
            adapter.updateData(suggestions, friendIds)
        }
    }

    private fun addFriend(user: UserResponse) {
        val token = sessionManager.fetchAuthToken() ?: return
        val myId = currentUser?.id ?: sessionManager.fetchUserId()
        
        lifecycleScope.launch {
            try {
                val req = FriendActionRequest(friendId = user.id ?: -1, requesterId = myId, status = "pending")
                val response = RetrofitClient.instance.addFriend("Bearer $token", req)
                
                if (response.isSuccessful) {
                    Toast.makeText(this@FriendSuggestionsActivity, "Demande envoyée !", Toast.LENGTH_SHORT).show()
                    fetchData()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FriendSuggestionsActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
