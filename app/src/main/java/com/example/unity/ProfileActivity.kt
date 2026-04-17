package com.example.unity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {
    
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        
        sessionManager = SessionManager(this)
        
        val appBar = findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appBar)
        if (appBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(appBar) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(0, systemBars.top, 0, 0)
                insets
            }
        }

        val tvFullName = findViewById<TextView>(R.id.tvFullName)
        val tvUserRole = findViewById<TextView>(R.id.tvUserRole)
        val tvInitials = findViewById<TextView>(R.id.tvInitials)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnSettings = findViewById<ImageButton>(R.id.btnSettings)
        val btnEditProfile = findViewById<MaterialButton>(R.id.btnEditProfile)

        // On appelle l'API pour récupérer les infos en temps réel
        fetchProfileData()

        btnBack.setOnClickListener {
            finish()
        }

        // Action du bouton Paramètres (Roue crantée)
        btnSettings.setOnClickListener {
            Log.d("PROFILE_NAV", "Clic sur Paramètres")
            val intent = Intent(this, AtelierGUIavance::class.java)
            startActivity(intent)
        }

        // Action du bouton Modifier
        btnEditProfile.setOnClickListener {
            Log.d("PROFILE_NAV", "Clic sur Modifier Profil")
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav?.selectedItemId = R.id.nav_profile
        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_friends -> {
                    startActivity(Intent(this, FriendsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_messages -> {
                    startActivity(Intent(this, MessagesActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> true
                else -> true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Rafraîchir les données au retour de la modification
        fetchProfileData()
    }

    private fun fetchProfileData() {
        val token = sessionManager.fetchAuthToken()
        if (token == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val tvName = findViewById<TextView>(R.id.tvFullName)
        val tvRole = findViewById<TextView>(R.id.tvUserRole)
        val tvInit = findViewById<TextView>(R.id.tvInitials)
        val tvBio = findViewById<TextView>(R.id.tvBio)
        val tvFriendsCount = findViewById<TextView>(R.id.tvFriendsCount)
        val tvPostCount = findViewById<TextView>(R.id.tvPostCount)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getMe("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!.user
                    
                    val username = user.username ?: "Utilisateur"
                    val firstName = user.firstName ?: ""
                    val lastName = user.lastName ?: ""
                    val role = user.role ?: "Membre"
                    
                    val fullName = "$firstName $lastName".trim()
                    if (fullName.isNotEmpty()) {
                        tvName.text = fullName
                        tvRole.text = "@$username • $role"
                    } else {
                        tvName.text = username
                        tvRole.text = role
                    }
                    
                    val displayForInitials = if (firstName.isNotEmpty()) firstName else username
                    tvInit.text = displayForInitials.take(1).uppercase()
                    
                    tvBio.text = user.bio ?: "Aucune biographie"
                    tvFriendsCount.text = user.friendsCount.toString()
                    tvPostCount.text = user.postsCount.toString()
                    
                    // DEBUG TOAST
                    Toast.makeText(this@ProfileActivity, "Data: Friends=${user.friendsCount}, Posts=${user.postsCount}", Toast.LENGTH_SHORT).show()
                    
                    
                } else {
                    Log.e("PROFILE_ERROR", "Erreur serveur : ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("PROFILE_ERROR", "Erreur réseau : ${e.message}")
            }
        }
    }
}
