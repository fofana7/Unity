package com.example.unity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class CreateGroupActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: SelectableFriendAdapter
    private var selectedFriendIds: MutableList<Int> = mutableListOf()
    private var allAvailableFriends: List<UserResponse> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_group)

        sessionManager = SessionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val rvFriends = findViewById<RecyclerView>(R.id.rvFriends)
        rvFriends.layoutManager = LinearLayoutManager(this)
        
        adapter = SelectableFriendAdapter(emptyList()) { ids ->
            selectedFriendIds = ids.toMutableList()
            updateButtonText()
        }
        rvFriends.adapter = adapter

        val role = sessionManager.fetchUserRole()?.lowercase()?.trim()
        val layoutTeacher = findViewById<View>(R.id.layoutTeacherOptions)
        
        if (role == "enseignant" || role == "professeur") {
            layoutTeacher.visibility = View.VISIBLE
            findViewById<MaterialButton>(R.id.btnSelectByClass).setOnClickListener {
                if (allAvailableFriends.isEmpty()) {
                    Toast.makeText(this, "Chargement des membres...", Toast.LENGTH_SHORT).show()
                } else {
                    showClassSelectionDialog()
                }
            }
        }

        findViewById<MaterialButton>(R.id.btnCreateGroup).setOnClickListener { createGroup() }

        loadFriends()
    }

    private fun updateButtonText() {
        val btnCreate = findViewById<MaterialButton>(R.id.btnCreateGroup)
        btnCreate.text = "Créer le groupe (${selectedFriendIds.size} membre${if (selectedFriendIds.size > 1) "s" else ""})"
    }

    private fun loadFriends() {
        val token = sessionManager.fetchAuthToken() ?: return
        val pbLoading = findViewById<ProgressBar>(R.id.pbLoading)
        val tvNoFriends = findViewById<TextView>(R.id.tvNoFriends)
        
        pbLoading.visibility = View.VISIBLE
        tvNoFriends.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Pour un enseignant, on tente d'abord de charger tous les utilisateurs via /users
                val role = sessionManager.fetchUserRole()?.lowercase()?.trim()
                val response = if (role == "enseignant" || role == "professeur" || role == "personnel") {
                    RetrofitClient.instance.getAllUsers("Bearer $token")
                } else {
                    RetrofitClient.instance.getMyFriends("Bearer $token")
                }
                pbLoading.visibility = View.GONE
                
                if (response.isSuccessful && response.body() != null) {
                    allAvailableFriends = response.body()!!
                    if (allAvailableFriends.isEmpty()) {
                        tvNoFriends.visibility = View.VISIBLE
                        tvNoFriends.text = if (role == "enseignant" || role == "professeur" || role == "personnel") {
                            "Aucun utilisateur trouvé."
                        } else {
                            "Aucun membre trouvé. Ajoutez des amis pour les voir ici."
                        }
                    } else {
                        adapter.updateData(allAvailableFriends)
                        updateButtonText()
                    }
                } else {
                    tvNoFriends.visibility = View.VISIBLE
                    tvNoFriends.text = "Erreur : ${response.code()}"
                }
            } catch (e: Exception) {
                pbLoading.visibility = View.GONE
                tvNoFriends.visibility = View.VISIBLE
                tvNoFriends.text = "Erreur réseau."
            }
        }
    }

    private fun showClassSelectionDialog() {
        // Liste des classes réelles d'après votre SQL (A1MSI, A2MSI, A3MSI etc.)
        val classes = allAvailableFriends.mapNotNull { it.classe?.trim() }
            .filter { it.isNotEmpty() }
            .distinct().sorted()

        if (classes.isEmpty()) {
            Toast.makeText(this, "Aucune classe trouvée. Assurez-vous que vos élèves ont une classe définie.", Toast.LENGTH_LONG).show()
            return
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Sélectionner une classe")
            .setItems(classes.toTypedArray()) { _, which ->
                selectMembersByClass(classes[which])
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun selectMembersByClass(className: String) {
        val idsOfClass = allAvailableFriends.filter { it.classe?.trim() == className }.mapNotNull { it.id }
        if (idsOfClass.isEmpty()) return

        selectedFriendIds = idsOfClass.toMutableList()
        adapter.setSelectedIds(selectedFriendIds)
        updateButtonText()
        
        val etName = findViewById<EditText>(R.id.etGroupName)
        if (etName.text.isEmpty() || etName.text.toString().startsWith("Classe ")) {
            etName.setText("Classe $className")
        }
        Toast.makeText(this, "${idsOfClass.size} membres de $className sélectionnés", Toast.LENGTH_SHORT).show()
    }

    private fun createGroup() {
        val name = findViewById<EditText>(R.id.etGroupName).text.toString().trim()
        val desc = findViewById<EditText>(R.id.etGroupDesc).text.toString().trim()
        
        if (name.isEmpty() || selectedFriendIds.isEmpty()) {
            Toast.makeText(this, "Nom et membres obligatoires", Toast.LENGTH_SHORT).show()
            return
        }

        val btnCreate = findViewById<MaterialButton>(R.id.btnCreateGroup)
        btnCreate.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val request = CreateGroupRequest(name, desc.ifEmpty { null }, selectedFriendIds)
                val response = RetrofitClient.instance.createGroup("Bearer ${sessionManager.fetchAuthToken()}", request)
                if (response.isSuccessful) {
                    Toast.makeText(this@CreateGroupActivity, "Groupe créé !", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@CreateGroupActivity, "Erreur serveur", Toast.LENGTH_SHORT).show()
                    btnCreate.isEnabled = true
                }
            } catch (e: Exception) {
                Toast.makeText(this@CreateGroupActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
                btnCreate.isEnabled = true
            }
        }
    }
}
