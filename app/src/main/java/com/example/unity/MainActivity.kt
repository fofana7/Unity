package com.example.unity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // --- VÉRIFICATION DES DONNÉES AU DÉMARRAGE ---
        val db = AppDatabase(this)
        db.debugLogDatabase()
        // ----------------------------------------------

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sessionManager = SessionManager(this)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("LOGIN_TEST", "Tentative de connexion pour : $email")
            Toast.makeText(this, "Connexion en cours...", Toast.LENGTH_SHORT).show()

            lifecycleScope.launch {
                try {
                    val loginRequest = LoginRequest(email, password)
                    val response = RetrofitClient.instance.login(loginRequest)

                    if (response.isSuccessful && response.body() != null) {
                        val loginResponse = response.body()!!
                        sessionManager.saveAuthToken(loginResponse.token)
                        // Sauvegarde de l'ID utilisateur pour les messages
                        loginResponse.user.id?.let { id -> sessionManager.saveUserId(id) }
                        
                        val intent = Intent(this@MainActivity, DashboardActivity::class.java)
                        intent.putExtra("USER_EMAIL", email)
                        startActivity(intent)
                        finish()
                    } else {
                        val errorMsg = response.errorBody()?.string() ?: "Identifiants incorrects"
                        Toast.makeText(this@MainActivity, "Erreur : $errorMsg", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Erreur de connexion au serveur", Toast.LENGTH_LONG).show()
                }
            }
        }

        tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }
}
