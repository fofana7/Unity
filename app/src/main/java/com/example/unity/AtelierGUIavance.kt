package com.example.unity

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch

class AtelierGUIavance : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_atelier_guiavance)
        
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

        // Section Compte
        findViewById<TextView>(R.id.tvEditProfile).setOnClickListener {
            showCustomDialog(R.layout.dialog_edit_profile, "Profil mis à jour")
        }

        findViewById<TextView>(R.id.tvChangePassword).setOnClickListener {
            showCustomDialog(R.layout.dialog_change_password, "Mot de passe modifié")
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
                    Toast.makeText(this, "Déconnexion réussie", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .show()
        }
    }

    private fun showCustomDialog(layoutResId: Int, successMessage: String?) {
        try {
            val view = LayoutInflater.from(this).inflate(layoutResId, null)
            MaterialAlertDialogBuilder(this)
                .setView(view)
                .setPositiveButton(if (successMessage != null) "Enregistrer" else "Fermer") { _, _ ->
                    if (successMessage != null) {
                        Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(if (successMessage != null) "Annuler" else null, null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur lors de l'ouverture du dialogue", Toast.LENGTH_SHORT).show()
        }
    }
}
