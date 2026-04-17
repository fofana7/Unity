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

class MessagesActivity : AppCompatActivity() {

    private lateinit var adapter: ConversationAdapter
    private lateinit var sessionManager: SessionManager
    private var allConversations: MutableList<Conversation> = mutableListOf()
    private var currentType = ConversationType.PRIVATE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_messages)
        
        sessionManager = SessionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        val rvConversations = findViewById<RecyclerView>(R.id.rvConversations)
        rvConversations.layoutManager = LinearLayoutManager(this)

        adapter = ConversationAdapter(emptyList()) { conv ->
            val intent = Intent(this, ChatActivity::class.java)
            if (conv.type == ConversationType.GROUP) {
                intent.putExtra("GROUP_ID", conv.id)
            } else {
                intent.putExtra("OTHER_USER_ID", conv.id)
            }
            intent.putExtra("CHAT_NAME", conv.userName)
            startActivity(intent)
        }
        rvConversations.adapter = adapter

        // Charger les vraies conversations depuis l'API
        loadConversations()

        // Onglets Privé / Groupe
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentType = if (tab?.position == 1) ConversationType.GROUP else ConversationType.PRIVATE
                filterConversations(currentType)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Bouton "+" pour une nouvelle conversation
        findViewById<View>(R.id.fabNewMessage).setOnClickListener {
            if (currentType == ConversationType.GROUP) {
                startActivity(Intent(this, CreateGroupActivity::class.java))
            } else {
                startActivity(Intent(this, FriendsActivity::class.java))
            }
        }

        setupBottomNavigation()
    }

    private fun loadConversations() {
        val token = sessionManager.fetchAuthToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getConversations("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    allConversations = response.body()!!
                        .map { it.toConversation() }
                        .toMutableList()
                    
                    // DEBUG TOAST
                    Toast.makeText(this@MessagesActivity, "Load: ${allConversations.size} convs (${allConversations.count { it.type == ConversationType.GROUP }} groupes)", Toast.LENGTH_SHORT).show()
                    
                    filterConversations(currentType)
                } else {
                    Log.e("MESSAGES", "Erreur API : ${response.code()} ${response.errorBody()?.string()}")
                    showEmpty()
                }
            } catch (e: Exception) {
                Log.e("MESSAGES", "Erreur réseau", e)
                showEmpty()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadConversations()
    }

    private fun filterConversations(type: ConversationType) {
        val filtered = allConversations.filter { it.type == type }
        adapter.updateData(filtered)
        
        // Afficher un message si vide
        val tvEmpty = findViewById<TextView?>(R.id.tvEmptyConversations)
        if (filtered.isEmpty()) {
            tvEmpty?.visibility = View.VISIBLE
            tvEmpty?.text = if (type == ConversationType.PRIVATE) 
                "Aucune conversation\nCommencez à discuter avec vos amis !" 
            else 
                "Aucun groupe"
        } else {
            tvEmpty?.visibility = View.GONE
        }
    }

    private fun showEmpty() {
        filterConversations(currentType)
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav?.selectedItemId = R.id.nav_messages
        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, DashboardActivity::class.java)); finish(); true }
                R.id.nav_friends -> { startActivity(Intent(this, FriendsActivity::class.java)); finish(); true }
                R.id.nav_messages -> true
                R.id.nav_profile -> { startActivity(Intent(this, ProfileActivity::class.java)); finish(); true }
                else -> true
            }
        }
    }
}
