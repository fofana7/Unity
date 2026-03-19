package com.example.unity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val currentUserEmail = intent.getStringExtra("USER_EMAIL")

        // Section Compte
        findViewById<TextView>(R.id.tvEditProfile).setOnClickListener {
            Toast.makeText(this, "Fonctionnalité bientôt disponible", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.tvChangePassword).setOnClickListener {
            Toast.makeText(this, "Fonctionnalité bientôt disponible", Toast.LENGTH_SHORT).show()
        }

        // Section Notifications
        findViewById<MaterialSwitch>(R.id.switchNotifications).setOnCheckedChangeListener { _, isChecked ->
            val message = if (isChecked) "Notifications push activées" else "Notifications push désactivées"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // Section Confidentialité
        findViewById<MaterialSwitch>(R.id.switchPrivacy).setOnCheckedChangeListener { _, isChecked ->
            val status = if (isChecked) "privé" else "public"
            MaterialAlertDialogBuilder(this)
                .setTitle("Changement de visibilité")
                .setMessage("Votre compte est désormais $status.")
                .setPositiveButton("OK", null)
                .show()
        }

        findViewById<TextView>(R.id.tvBlockUsers).setOnClickListener {
            Toast.makeText(this, "Fonctionnalité bientôt disponible", Toast.LENGTH_SHORT).show()
        }

        // Section Aide & Déconnexion
        findViewById<TextView>(R.id.tvHelpCenter).setOnClickListener {
            Toast.makeText(this, "Centre d'aide bientôt disponible", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.tvLogoutSettings).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Déconnexion")
                .setMessage("Êtes-vous sûr de vouloir vous déconnecter ?")
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Se déconnecter") { _, _ ->
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .show()
        }

        // Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_settings
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.putExtra("USER_EMAIL", currentUserEmail)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_messages -> {
                    val intent = Intent(this, MessagesActivity::class.java)
                    intent.putExtra("USER_EMAIL", currentUserEmail)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("USER_EMAIL", currentUserEmail)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_settings -> true
                else -> true
            }
        }
    }
}