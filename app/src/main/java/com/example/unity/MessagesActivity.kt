package com.example.unity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout

class MessagesActivity : AppCompatActivity() {

    private lateinit var adapter: ConversationAdapter
    private lateinit var allConversations: List<Conversation>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_messages)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        // 1. Données d'exemple avec types
        allConversations = listOf(
            Conversation(1, "Rana Fawaz", "Salut ! Tu as fini le projet ?", "10:30", ConversationType.PRIVATE),
            Conversation(2, "Talida Font", "On se voit quand ?", "Hier", ConversationType.PRIVATE),
            Conversation(3, "Groupe Android Unity", "Rana: J'ai poussé le code", "11:15", ConversationType.GROUP),
            Conversation(4, "Projet Fin d'Études", "Prof: N'oubliez pas le rapport", "Lun.", ConversationType.GROUP),
            Conversation(5, "Support", "Votre compte est actif", "01/10", ConversationType.PRIVATE)
        )

        // 2. Configuration du RecyclerView
        val rvConversations = findViewById<RecyclerView>(R.id.rvConversations)
        rvConversations.layoutManager = LinearLayoutManager(this)
        adapter = ConversationAdapter(allConversations.filter { it.type == ConversationType.PRIVATE })
        rvConversations.adapter = adapter

        // 3. Gestion des Onglets (Tabs)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> filterConversations(ConversationType.PRIVATE)
                    1 -> filterConversations(ConversationType.GROUP)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // 4. Navigation du bas
        setupBottomNavigation()
    }

    private fun filterConversations(type: ConversationType) {
        val filteredList = allConversations.filter { it.type == type }
        adapter.updateData(filteredList)
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav?.selectedItemId = R.id.nav_messages
        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_messages -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }
                else -> true
            }
        }
    }
}
