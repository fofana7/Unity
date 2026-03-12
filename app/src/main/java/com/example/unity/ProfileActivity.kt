package com.example.unity

import android.content.Context
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.headerBg)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        val tvFullName = findViewById<TextView>(R.id.tvFullName)
        val tvUserHandle = findViewById<TextView>(R.id.tvUserHandle)
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        val tvInitials = findViewById<TextView>(R.id.tvInitials)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // Récupération des données utilisateur
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val email = intent.getStringExtra("USER_EMAIL")

        if (email != null) {
            val username = sharedPref.getString("${email}_username", "Utilisateur")
            val firstName = sharedPref.getString("${email}_firstname", "")
            val lastName = sharedPref.getString("${email}_lastname", "")

            tvFullName.text = "$firstName $lastName"
            tvUserHandle.text = "@${username?.replace(" ", "")?.lowercase()}"
            tvEmail.text = email
            tvInitials.text = if (firstName!!.isNotEmpty()) firstName[0].uppercase() else "U"
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}