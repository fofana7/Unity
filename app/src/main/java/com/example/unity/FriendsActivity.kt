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
    
    private var allUsers = listOf<UserResponse>()
    private var myFriends = listOf<UserResponse>()
    private var friendRequests = listOf<FriendActionRequest>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_friends)

        sessionManager = SessionManager(this)
        progressBar = findViewById(R.id.progressBar)

        val topBar = findViewById<View>(R.id.topBar)
        if (topBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(topBar) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(0, systemBars.top, 0, 0)
                insets
            }
        }

        val rvFriends = findViewById<RecyclerView>(R.id.rvFriends)
        rvFriends.layoutManager = LinearLayoutManager(this)
        
        adapter = FriendAdapter(mutableListOf()) { user, actionType ->
            handleFriendAction(user, actionType)
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
        
        // Au démarrage, on charge les données de base pour tous les onglets
        fetchAllData(0)
    }

    private fun fetchAllData(initialTab: Int) {
        val token = sessionManager.fetchAuthToken() ?: return
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                // Chargement en parallèle/séquentiel pour préparer les 3 listes
                val friendsRes = RetrofitClient.instance.getMyFriends("Bearer $token")
                if (friendsRes.isSuccessful) myFriends = friendsRes.body() ?: emptyList()

                val allUsersRes = RetrofitClient.instance.getAllUsers("Bearer $token")
                if (allUsersRes.isSuccessful) allUsers = allUsersRes.body() ?: emptyList()

                val reqRes = RetrofitClient.instance.getFriendRequests("Bearer $token")
                if (reqRes.isSuccessful) {
                    friendRequests = reqRes.body() ?: emptyList()
                    val myId = sessionManager.fetchUserId()
                    Log.d("FRIENDS_DEBUG", "Mon ID: $myId, Requêtes reçues du serveur: ${friendRequests.size}")
                    Toast.makeText(this@FriendsActivity, "Diagnostic: ID=$myId, Total Requêtes=${friendRequests.size}", Toast.LENGTH_LONG).show()
                } else {
                    Log.e("FRIENDS_DEBUG", "Erreur API Requêtes: ${reqRes.code()} - ${reqRes.errorBody()?.string()}")
                }

                refreshCurrentTab(initialTab)
            } catch (e: Exception) {
                Log.e("FRIENDS", "Erreur lors du chargement initial", e)
                Toast.makeText(this@FriendsActivity, "Erreur tech: ${e.message}", Toast.LENGTH_LONG).show()
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
        if (myFriends.isEmpty()) {
            Toast.makeText(this, "Aucun ami pour le moment", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSuggestions() {
        val myId = sessionManager.fetchUserId()
        val friendIds = myFriends.mapNotNull { it.id }.toSet()
        
        // On exclut soi-même et ceux qui sont déjà amis
        val suggestions = allUsers.filter { user ->
            user.id != myId && !friendIds.contains(user.id)
        }
        adapter.updateData(suggestions, FriendActionType.SUGGESTION)
    }

    private fun showRequests() {
        val myId = sessionManager.fetchUserId()
        
        // --- Demandes Reçues (toId == monID) ---
        val incoming = friendRequests
            .filter { it.friendId == myId }
            .mapNotNull { req -> 
                allUsers.find { it.id == req.requesterId }?.let { FriendItem(it, FriendActionType.REQUEST) }
            }
        
        // --- Demandes Envoyées (fromId == monID) ---
        val outgoing = friendRequests
            .filter { it.requesterId == myId }
            .mapNotNull { req -> 
                allUsers.find { it.id == req.friendId }?.let { FriendItem(it, FriendActionType.SENT_REQUEST) }
            }
            
        // Fusion des deux listes
        val mergedList = incoming + outgoing
        
        adapter.updateData(mergedList)
        
        if (mergedList.isEmpty()) {
            Toast.makeText(this, "Aucune demande en attente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleFriendAction(user: UserResponse, actionType: FriendActionType) {
        val token = sessionManager.fetchAuthToken() ?: return
        
        lifecycleScope.launch {
            try {
                when (actionType) {
                    FriendActionType.FRIEND -> {
                        // Ouvrir message
                        val intent = Intent(this@FriendsActivity, ChatActivity::class.java)
                        intent.putExtra("OTHER_USER_ID", user.id)
                        intent.putExtra("OTHER_USER_NAME", user.username)
                        startActivity(intent)
                    }
                    FriendActionType.SUGGESTION -> {
                        val currentUserId = sessionManager.fetchUserId()
                        val req = FriendActionRequest(
                            friendId = user.id,
                            requesterId = currentUserId,
                            status = "pending"
                        )
                        val res = RetrofitClient.instance.addFriend("Bearer $token", req)
                        if (res.isSuccessful) {
                            Toast.makeText(this@FriendsActivity, "Demande envoyée à ${user.username} !", Toast.LENGTH_SHORT).show()
                            // On rafraîchit les données pour faire disparaître la suggestion
                            fetchAllData(1)
                        } else {
                            val errorBody = res.errorBody()?.string() ?: "Erreur inconnue"
                            Log.e("FRIENDS", "Add friend failed: $errorBody")
                            Toast.makeText(this@FriendsActivity, "Erreur: $errorBody", Toast.LENGTH_SHORT).show()
                        }
                    }
                    FriendActionType.REQUEST -> {
                        val currentUserId = sessionManager.fetchUserId()
                        val req = FriendActionRequest(
                            friendId = user.id,
                            requesterId = currentUserId,
                            status = "accepted"
                        )
                        val res = RetrofitClient.instance.acceptFriend("Bearer $token", req)
                        if (res.isSuccessful) {
                            Toast.makeText(this@FriendsActivity, "Ami ajouté !", Toast.LENGTH_SHORT).show()
                            fetchAllData(2) // On recharge tout pour mettre à jour les listes
                        } else {
                            Toast.makeText(this@FriendsActivity, "Impossible d'accepter", Toast.LENGTH_SHORT).show()
                        }
                    }
                    FriendActionType.SENT_REQUEST -> {
                        // Annuler la demande (souvent un DELETE sur le serveur)
                        Toast.makeText(this@FriendsActivity, "Demande annulée (en attente serveur)", Toast.LENGTH_SHORT).show()
                        // Pour l'instant, on retire juste de la vue locale si l'API ne supporte pas delete
                        fetchAllData(2)
                    }
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
