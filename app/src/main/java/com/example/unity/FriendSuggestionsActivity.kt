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
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        rvSuggestions = findViewById(R.id.rvSuggestions)

        adapter = FriendSuggestionsAdapter(emptyList()) { user ->
            addFriend(user)
        }
        rvSuggestions.layoutManager = LinearLayoutManager(this)
        rvSuggestions.adapter = adapter

        fetchData()
    }

    private fun fetchData() {
        val token = sessionManager.fetchAuthToken() ?: return

        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        rvSuggestions.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Récupération de l'utilisateur actuel
                val meResponse = RetrofitClient.instance.getMe("Bearer $token")
                if (meResponse.isSuccessful) {
                    currentUser = meResponse.body()?.user
                }

                // Récupération des amis actuels
                val friendsResponse = RetrofitClient.instance.getMyFriends("Bearer $token")
                if (friendsResponse.isSuccessful) {
                    myFriends = friendsResponse.body()?.friends ?: friendsResponse.body()?.users ?: emptyList()
                }

                // Récupération de tous les utilisateurs (amis potentiels)
                val allUsersResponse = RetrofitClient.instance.getAllUsers("Bearer $token")
                if (allUsersResponse.isSuccessful) {
                    allUsers = allUsersResponse.body()?.users ?: emptyList()
                }

                filterAndDisplaySuggestions()

            } catch (e: Exception) {
                Log.e("SUGGESTIONS", "Erreur réseau", e)
                Toast.makeText(this@FriendSuggestionsActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun filterAndDisplaySuggestions() {
        progressBar.visibility = View.GONE
        
        val friendIds = myFriends.mapNotNull { it.id }.toSet()
        val currentUserId = currentUser?.id

        // On exclut l'utilisateur courant et ses amis déjà existants
        val suggestions = allUsers.filter { user ->
            user.id != null && user.id != currentUserId && !friendIds.contains(user.id)
        }

        if (suggestions.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvSuggestions.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvSuggestions.visibility = View.VISIBLE
            adapter.updateUsers(suggestions)
        }
    }

    private fun addFriend(user: UserResponse) {
        val token = sessionManager.fetchAuthToken() ?: return
        val currentUserId = currentUser?.id ?: return
        
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val req = FriendActionRequest(
                    friendId = user.id,
                    requesterId = currentUserId,
                    status = "pending"
                )
                val response = RetrofitClient.instance.addFriend("Bearer $token", req)
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    Toast.makeText(this@FriendSuggestionsActivity, "Demande envoyée à ${user.username}", Toast.LENGTH_SHORT).show()
                    
                    // On retire l'utilisateur de la liste après l'ajout réussi
                    val updatedList = adapter.getCurrentUsers().filter { it.id != user.id }
                    adapter.updateUsers(updatedList)
                    
                    if (updatedList.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        rvSuggestions.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(this@FriendSuggestionsActivity, "Erreur lors de l'envoi", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@FriendSuggestionsActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
