package com.example.unity

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import org.json.JSONObject

class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etFirstName = findViewById<TextInputEditText>(R.id.etFirstName)
        val etLastName = findViewById<TextInputEditText>(R.id.etLastName)
        val etUsername = findViewById<TextInputEditText>(R.id.etUsername)
        val actRole = findViewById<AutoCompleteTextView>(R.id.actRole)
        val tilClasse = findViewById<TextInputLayout>(R.id.tilClasse)
        val actClasse = findViewById<AutoCompleteTextView>(R.id.actClasse)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)

        // Configuration des rôles
        val rolesDisplay = arrayOf("Étudiant", "Professeur", "Administrateur")
        val rolesTechnical = mapOf("Étudiant" to "eleve", "Professeur" to "enseignant", "Administrateur" to "personnel")
        val adapterRoles = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, rolesDisplay)
        actRole.setAdapter(adapterRoles)

        // Configuration des classes
        val classes = arrayOf("A1MSI", "A2MSI", "A3MSI")
        val adapterClasses = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, classes)
        actClasse.setAdapter(adapterClasses)

        // Affichage dynamique du champ Classe
        actRole.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position).toString()
            if (selected == "Étudiant") {
                tilClasse.visibility = View.VISIBLE
            } else {
                tilClasse.visibility = View.GONE
                actClasse.text.clear()
            }
        }

        btnSignUp.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val selectedRoleDisplay = actRole.text.toString().trim()
            val selectedClasse = actClasse.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            val technicalRole = rolesTechnical[selectedRoleDisplay] ?: "eleve"

            if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty() || 
                selectedRoleDisplay.isEmpty() || email.isEmpty() || password.isEmpty() ||
                (technicalRole == "eleve" && selectedClasse.isEmpty())) {
                Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show()
            } else if (!email.endsWith("@esme.fr")) {
                Toast.makeText(this, getString(R.string.error_invalid_email_domain), Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, getString(R.string.error_password_mismatch), Toast.LENGTH_SHORT).show()
            } else if (!isPasswordComplex(password)) {
                Toast.makeText(this, "Votre mot de passe doit être complexe. Il doit comporter au minimum 12 caractères mélangeant les majuscules, les minuscules, des chiffres et des caractères spéciaux.", Toast.LENGTH_LONG).show()
            } else {
                lifecycleScope.launch {
                    try {
                        val registerRequest = RegisterRequest(
                            email = email,
                            password = password,
                            username = username,
                            firstName = firstName,
                            lastName = lastName,
                            role = technicalRole,
                            classe = if (technicalRole == "eleve") selectedClasse else null
                        )
                        
                        val response = RetrofitClient.instance.register(registerRequest)

                        if (response.isSuccessful) {
                            Toast.makeText(this@SignUpActivity, "Inscription réussie ! En attente de validation par l'administration.", Toast.LENGTH_LONG).show()
                            finish()
                        } else {
                            val errorBodyString = response.errorBody()?.string()
                            var displayMessage = "Erreur d'inscription"
                            
                            if (!errorBodyString.isNullOrEmpty()) {
                                try {
                                    val jsonObject = JSONObject(errorBodyString)
                                    if (jsonObject.has("error")) {
                                        displayMessage = jsonObject.getString("error")
                                    }
                                } catch (e: Exception) {
                                    if (errorBodyString.contains("email déjà utilisé", ignoreCase = true) || 
                                        errorBodyString.contains("username already exists", ignoreCase = true)) {
                                        displayMessage = "Cet email ou nom d'utilisateur est déjà utilisé."
                                    }
                                }
                            }
                            Toast.makeText(this@SignUpActivity, displayMessage, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@SignUpActivity, "Erreur réseau : ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        tvLogin.setOnClickListener { finish() }
    }

    private fun isPasswordComplex(password: String): Boolean {
        // Au moins 12 caractères, Majuscule, Minuscule, Chiffre et Caractère spécial
        val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._,?;:])(?=\\S+$).{12,}$".toRegex()
        return passwordPattern.matches(password)
    }
}