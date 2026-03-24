package com.example.unity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private var currentUser: UserResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_profile)

        sessionManager = SessionManager(this)

        val etFullName = findViewById<TextInputEditText>(R.id.etFullName)
        val etBio = findViewById<TextInputEditText>(R.id.etBio)
        val rgStatus = findViewById<RadioGroup>(R.id.rgStatus)
        val btnSave = findViewById<MaterialButton>(R.id.btnSave)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)

        toolbar.setNavigationOnClickListener { finish() }

        // 1. Charger les infos actuelles
        loadCurrentProfile(etFullName, etBio, rgStatus)

        // 2. Enregistrer les modifications
        btnSave.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val bio = etBio.text.toString().trim()
            
            val selectedId = rgStatus.checkedRadioButtonId
            val status = if (selectedId != -1) {
                findViewById<RadioButton>(selectedId).text.toString()
            } else {
                "Étudiant"
            }

            updateProfile(fullName, bio, status)
        }
    }

    private fun loadCurrentProfile(etName: TextInputEditText, etBio: TextInputEditText, rgStatus: RadioGroup) {
        val token = sessionManager.fetchAuthToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getMe("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    currentUser = response.body()
                    val user = currentUser!!
                    
                    etName.setText("${user.firstName ?: ""} ${user.lastName ?: ""}".trim())
                    etBio.setText(user.bio ?: "")
                    
                    // Sélectionner le bon radio button selon le rôle
                    when (user.role) {
                        "Étudiant" -> findViewById<RadioButton>(R.id.rbStudent).isChecked = true
                        "Enseignant" -> findViewById<RadioButton>(R.id.rbTeacher).isChecked = true
                        "Modérateur" -> findViewById<RadioButton>(R.id.rbModerator).isChecked = true
                    }
                }
            } catch (e: Exception) {
                Log.e("EDIT_PROFILE", "Erreur chargement", e)
            }
        }
    }

    private fun updateProfile(fullName: String, bio: String, status: String) {
        val token = sessionManager.fetchAuthToken() ?: return
        
        // On sépare le nom complet en prénom/nom pour l'API si nécessaire
        val nameParts = fullName.split(" ", limit = 2)
        val firstName = nameParts.getOrNull(0) ?: ""
        val lastName = nameParts.getOrNull(1) ?: ""

        val updatedUser = UserResponse(
            email = currentUser?.email ?: "",
            username = currentUser?.username,
            firstName = firstName,
            lastName = lastName,
            bio = bio,
            role = status
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.updateMe("Bearer $token", updatedUser)
                if (response.isSuccessful) {
                    Toast.makeText(this@EditProfileActivity, "Profil mis à jour !", Toast.LENGTH_SHORT).show()
                    finish() // Retour au profil
                } else {
                    Toast.makeText(this@EditProfileActivity, "Erreur lors de la sauvegarde", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditProfileActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
