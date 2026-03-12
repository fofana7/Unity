package com.example.unity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class MessagesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_messages)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        if (bottomNav != null) {
            bottomNav.selectedItemId = R.id.nav_messages
            bottomNav.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        finish() // Retour au dashboard
                        true
                    }
                    R.id.nav_messages -> true
                    R.id.nav_profile -> {
                        val intent = Intent(this, ProfileActivity::class.java)
                        intent.putExtra("USER_EMAIL", intent.getStringExtra("USER_EMAIL"))
                        startActivity(intent)
                        finish()
                        true
                    }
                    else -> {
                        Toast.makeText(this, "Bientôt disponible", Toast.LENGTH_SHORT).show()
                        true
                    }
                }
            }
        }
    }
}