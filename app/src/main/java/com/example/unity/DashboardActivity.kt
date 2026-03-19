package com.example.unity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private var currentUser: UserResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        sessionManager = SessionManager(this)
        
        // Appliquer les marges pour le design immersif
        val topBar = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.topBar)
        ViewCompat.setOnApplyWindowInsetsListener(topBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val tvTopUsername = findViewById<TextView>(R.id.tvTopUsername)
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val btnLogout = findViewById<TextView>(R.id.btnLogout)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // 1. Récupérer les infos du profil depuis le backend
        fetchUserProfile(tvTopUsername, tvWelcome)

        // 2. Gestion de la déconnexion
        btnLogout.setOnClickListener {
            sessionManager.clearSession() // Effacer le token
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // 3. Navigation
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.nav_messages -> {
                    startActivity(Intent(this, MessagesActivity::class.java))
                    true
                }
                else -> true
            }
        }
    }

    private fun fetchUserProfile(tvUsername: TextView, tvWelcome: TextView) {
        val token = sessionManager.fetchAuthToken()
        if (token == null) {
            redirectToLogin()
            return
        }

        lifecycleScope.launch {
            try {
                // On ajoute "Bearer " devant le token comme attendu par la plupart des API
                val response = RetrofitClient.instance.getMe("Bearer $token")
                
                if (response.isSuccessful && response.body() != null) {
                    currentUser = response.body()
                    val name = currentUser?.firstName ?: currentUser?.username ?: "Utilisateur"
                    
                    tvUsername.text = name
                    tvWelcome.text = "Ravi de vous revoir, $name !"
                    
                    Log.d("DASHBOARD", "Profil récupéré : ${currentUser?.email}")
                } else {
                    Log.e("DASHBOARD", "Erreur lors de la récupération du profil")
                    if (response.code() == 401) redirectToLogin()
                }
            } catch (e: Exception) {
                Log.e("DASHBOARD", "Erreur réseau", e)
                Toast.makeText(this@DashboardActivity, "Erreur de synchronisation", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
