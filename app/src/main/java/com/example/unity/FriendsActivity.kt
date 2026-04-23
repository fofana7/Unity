package com.example.unity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class FriendsActivity : AppCompatActivity() {

    private lateinit var adapter: FriendAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var progressBar: ProgressBar

    private var allUsers = listOf<UserResponse>()
    private var myFriends = listOf<UserResponse>()
    private var incomingRequests = listOf<UserResponse>()
    private var outgoingRequests = listOf<UserResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_friends)

        sessionManager = SessionManager(this)
        progressBar = findViewById(R.id.progressBar)

        val topBar = findViewById<View>(R.id.topBar)
        topBar?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                v.setPadding(0, insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, 0, 0)
                insets
            }
        }

        val rvFriends = findViewById<RecyclerView>(R.id.rvFriends)
        rvFriends.layoutManager = LinearLayoutManager(this)

        adapter = FriendAdapter(mutableListOf()) { user, actionType, isAccept ->
            handleFriendAction(user, actionType, isAccept)
        }
        rvFriends.adapter = adapter

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                refreshCurrentTab(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        setupBottomNavigation()
        fetchAllData(0)
    }

    private fun fetchAllData(initialTab: Int) {
        val token = sessionManager.fetchAuthToken() ?: return
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val friendsRes = RetrofitClient.instance.getMyFriends("Bearer $token")
                if (friendsRes.isSuccessful) myFriends = friendsRes.body() ?: emptyList()

                val allRes = RetrofitClient.instance.getAllUsers("Bearer $token")
                if (allRes.isSuccessful) allUsers = allRes.body() ?: emptyList()

                val reqRes = RetrofitClient.instance.getFriendRequests("Bearer $token")
                if (reqRes.isSuccessful) incomingRequests = reqRes.body() ?: emptyList()

                val sentRes = RetrofitClient.instance.getSentRequests("Bearer $token")
                if (sentRes.isSuccessful) outgoingRequests = sentRes.body() ?: emptyList()

                refreshCurrentTab(initialTab)
            } catch (e: Exception) {
                Log.e("FRIENDS", "Load failed", e)
                Toast.makeText(this@FriendsActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun refreshCurrentTab(position: Int) {
        when (position) {
            0 -> showMyFriends()
            1 -> showSuggestions()
            2 -> showRequests()
        }
    }

    private fun showMyFriends() {
        adapter.updateData(myFriends, FriendActionType.FRIEND)
        updateEmptyState(myFriends.isEmpty(), "Vous n'avez pas encore d'amis")
    }

    private fun showSuggestions() {
        val myId = sessionManager.fetchUserId()
        val friendIds = myFriends.mapNotNull { it.id }.toSet()
        val incomingIds = incomingRequests.mapNotNull { it.id }.toSet()
        val outgoingIds = outgoingRequests.mapNotNull { it.id }.toSet()

        val suggestions = allUsers.filter { user ->
            user.id != myId && 
            !friendIds.contains(user.id) && 
            !incomingIds.contains(user.id) && 
            !outgoingIds.contains(user.id)
        }
        adapter.updateData(suggestions, FriendActionType.SUGGESTION)
        updateEmptyState(suggestions.isEmpty(), "Aucune suggestion pour l'instant")
    }

    private fun showRequests() {
        adapter.updateData(incomingRequests, FriendActionType.REQUEST)
        updateEmptyState(incomingRequests.isEmpty(), "Aucune demande en attente")
    }

    private fun updateEmptyState(isEmpty: Boolean, message: String) {
        val tvEmpty = findViewById<TextView?>(R.id.tvEmptyState)
        tvEmpty?.visibility = if (isEmpty) View.VISIBLE else View.GONE
        tvEmpty?.text = message
    }

    private fun handleFriendAction(user: UserResponse, actionType: FriendActionType, isAccept: Boolean) {
        val token = sessionManager.fetchAuthToken() ?: return
        val myId = sessionManager.fetchUserId()

        lifecycleScope.launch {
            try {
                when (actionType) {
                    FriendActionType.SUGGESTION -> {
                        val req = FriendActionRequest(friendId = user.id, requesterId = myId, status = "pending")
                        val res = RetrofitClient.instance.addFriend("Bearer $token", req)
                        if (res.isSuccessful) {
                            Toast.makeText(this@FriendsActivity, "Demande envoyée !", Toast.LENGTH_SHORT).show()
                            fetchAllData(1)
                        }
                    }
                    FriendActionType.REQUEST -> {
                        val req = FriendActionRequest(friendId = myId, requesterId = user.id)
                        val res = if (isAccept) {
                            RetrofitClient.instance.acceptFriend("Bearer $token", req)
                        } else {
                            RetrofitClient.instance.declineFriend("Bearer $token", req)
                        }
                        if (res.isSuccessful) {
                            Toast.makeText(this@FriendsActivity, if(isAccept) "Ami ajouté" else "Demande refusée", Toast.LENGTH_SHORT).show()
                            fetchAllData(2)
                        }
                    }
                    FriendActionType.FRIEND -> {
                        val intent = Intent(this@FriendsActivity, ChatActivity::class.java)
                        intent.putExtra("OTHER_USER_ID", user.id)
                        intent.putExtra("OTHER_USER_NAME", user.username ?: "Ami")
                        startActivity(intent)
                    }
                    FriendActionType.SENT_REQUEST -> {}
                }
            } catch (e: Exception) {
                Log.e("FRIENDS", "Action failed", e)
                Toast.makeText(this@FriendsActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
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
