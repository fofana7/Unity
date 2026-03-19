package com.example.unity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.launch

class AtelierGUIavance : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var progressBar: ProgressBar
    private var currentUser: UserResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_atelier_guiavance)
        
        sessionManager = SessionManager(this)
        progressBar = findViewById(R.id.progressBar)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        fetchUserProfile()

        // Bouton Retour à l'accueil
        findViewById<TextView>(R.id.tvBackToHome).setOnClickListener {
            finish()
        }

        // Section Compte
        findViewById<TextView>(R.id.tvEditProfile).setOnClickListener {
            showEditProfileDialog()
        }

        findViewById<TextView>(R.id.tvChangePassword).setOnClickListener {
            showChangePasswordDialog()
        }

        findViewById<TextView>(R.id.tvDeleteAccount).setOnClickListener {
            showDeleteAccountDialog()
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
            showCustomDialog(R.layout.dialog_blocked_users, null)
        }

        // Section Aide & Déconnexion
        findViewById<TextView>(R.id.tvHelpCenter).setOnClickListener {
            showCustomDialog(R.layout.dialog_help_center, null)
        }

        findViewById<TextView>(R.id.tvLogout).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Déconnexion")
                .setMessage("Êtes-vous sûr de vouloir vous déconnecter ?")
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Se déconnecter") { _, _ ->
                    logout()
                }
                .show()
        }
    }

    private fun fetchUserProfile() {
        val token = sessionManager.fetchAuthToken() ?: return
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getMe("Bearer $token")
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    currentUser = response.body()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Log.e("SETTINGS", "Erreur fetch profil", e)
            }
        }
    }

    private fun showEditProfileDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null)
        val etUsername = view.findViewById<EditText>(R.id.etEditUsername)
        val etBio = view.findViewById<EditText>(R.id.etEditBio)

        etUsername.setText(currentUser?.username)
        etBio.setText(currentUser?.bio)

        MaterialAlertDialogBuilder(this)
            .setView(view)
            .setPositiveButton("Enregistrer") { _, _ ->
                val newUsername = etUsername.text.toString().trim()
                val newBio = etEditBioText(view).text.toString().trim() // correction ici
                
                if (newUsername.isEmpty()) {
                    Toast.makeText(this, "Le nom d'utilisateur ne peut pas être vide", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                updateProfileApi(newUsername, newBio)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun etEditBioText(view: View): EditText = view.findViewById(R.id.etEditBio)

    private fun updateProfileApi(username: String, bio: String) {
        val token = sessionManager.fetchAuthToken() ?: return
        val updatedUser = UserResponse(
            email = currentUser?.email ?: "",
            username = username,
            bio = bio,
            firstName = currentUser?.firstName,
            lastName = currentUser?.lastName,
            role = currentUser?.role,
            classe = currentUser?.classe
        )

        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.updateMe("Bearer $token", updatedUser)
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    currentUser = response.body()
                    Toast.makeText(this@AtelierGUIavance, "Profil mis à jour avec succès", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@AtelierGUIavance, "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@AtelierGUIavance, "Erreur réseau : ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showChangePasswordDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)
        val etOldPassword = view.findViewById<EditText>(R.id.etOldPassword)
        val etNewPassword = view.findViewById<EditText>(R.id.etNewPassword)

        MaterialAlertDialogBuilder(this)
            .setView(view)
            .setPositiveButton("Enregistrer") { _, _ ->
                val oldPwd = etOldPassword.text.toString().trim()
                val newPwd = etNewPassword.text.toString().trim()

                if (oldPwd.isEmpty() || newPwd.isEmpty()) {
                    Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                changePasswordApi(oldPwd, newPwd)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun changePasswordApi(oldPwd: String, newPwd: String) {
        val token = sessionManager.fetchAuthToken() ?: return
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val request = ChangePasswordRequest(oldPwd, newPwd)
                val response = RetrofitClient.instance.changePassword("Bearer $token", request)
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    Toast.makeText(this@AtelierGUIavance, "Mot de passe modifié avec succès", Toast.LENGTH_SHORT).show()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Erreur"
                    Toast.makeText(this@AtelierGUIavance, "Erreur : $errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@AtelierGUIavance, "Erreur réseau", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteAccountDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Supprimer le compte")
            .setMessage("Cette action est irréversible. Toutes vos données seront perdues. Continuer ?")
            .setNegativeButton("Annuler", null)
            .setPositiveButton("Supprimer") { _, _ ->
                deleteAccountApi()
            }
            .show()
    }

    private fun deleteAccountApi() {
        val token = sessionManager.fetchAuthToken() ?: return
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.deleteMe("Bearer $token")
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    Toast.makeText(this@AtelierGUIavance, "Compte supprimé", Toast.LENGTH_SHORT).show()
                    logout()
                } else {
                    Toast.makeText(this@AtelierGUIavance, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@AtelierGUIavance, "Erreur réseau", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logout() {
        sessionManager.clearSession()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showCustomDialog(layoutResId: Int, successMessage: String?) {
        val view = LayoutInflater.from(this).inflate(layoutResId, null)
        MaterialAlertDialogBuilder(this)
            .setView(view)
            .setPositiveButton(if (successMessage != null) "Fermer" else "OK", null)
            .show()
    }
}
