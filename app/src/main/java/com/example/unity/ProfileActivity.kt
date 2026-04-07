package com.example.unity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
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
        fetchProfileData(tvFullName, tvUserRole, tvInitials)

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
    }

    override fun onResume() {
        super.onResume()
        // Rafraîchir les données au retour de la modification
        val tvFullName = findViewById<TextView>(R.id.tvFullName)
        val tvUserRole = findViewById<TextView>(R.id.tvUserRole)
        val tvInitials = findViewById<TextView>(R.id.tvInitials)
        fetchProfileData(tvFullName, tvUserRole, tvInitials)
    }

    private fun fetchProfileData(tvName: TextView, tvRole: TextView, tvInit: TextView) {
        val token = sessionManager.fetchAuthToken()
        if (token == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

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
                    
                } else {
                    Log.e("PROFILE_ERROR", "Erreur serveur : ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("PROFILE_ERROR", "Erreur réseau : ${e.message}")
            }
        }
    }
}
