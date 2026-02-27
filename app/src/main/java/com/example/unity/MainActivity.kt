package com.example.unity

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show()
            } else {
                // Logique de connexion
                Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
            }
        }

        tvForgotPassword.setOnClickListener {
            // Action pour mot de passe oublié
            Toast.makeText(this, getString(R.string.forgot_password_clicked), Toast.LENGTH_SHORT).show()
        }

        tvSignUp.setOnClickListener {
            // Action pour l'inscription
            Toast.makeText(this, getString(R.string.signup_clicked), Toast.LENGTH_SHORT).show()
        }
    }
}