package com.example.unity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class AddGroupMembersActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: SelectableFriendAdapter
    private var selectedIds: MutableList<Int> = mutableListOf()
    private var groupId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_members)

        sessionManager = SessionManager(this)
        groupId = intent.getIntExtra("GROUP_ID", -1)

        if (groupId == -1) {
            Toast.makeText(this, "Erreur : Groupe introuvable", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val rvFriends = findViewById<RecyclerView>(R.id.rvFriends)
        rvFriends.layoutManager = LinearLayoutManager(this)
        
        adapter = SelectableFriendAdapter(emptyList()) { ids ->
            selectedIds = ids.toMutableList()
            updateButtonText()
        }
        rvFriends.adapter = adapter

        findViewById<MaterialButton>(R.id.btnAddMembers).setOnClickListener { addMembers() }

        loadAvailableUsers()
    }

    private fun updateButtonText() {
        val btn = findViewById<MaterialButton>(R.id.btnAddMembers)
        btn.text = "Ajouter (${selectedIds.size} membre${if (selectedIds.size > 1) "s" else ""})"
        btn.isEnabled = selectedIds.isNotEmpty()
    }

    private fun loadAvailableUsers() {
        val token = sessionManager.fetchAuthToken() ?: return
        val pbLoading = findViewById<ProgressBar>(R.id.pbLoading)
        val tvEmpty = findViewById<TextView>(R.id.tvNoFriends)

        pbLoading.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                // On récupère tous les amis/utilisateurs
                val role = sessionManager.fetchUserRole()?.lowercase()?.trim()
                val response = if (role == "enseignant" || role == "professeur") {
                    RetrofitClient.instance.getAllUsers("Bearer $token")
                } else {
                    RetrofitClient.instance.getMyFriends("Bearer $token")
                }

                // On récupère aussi les membres actuels du groupe pour les filtrer
                val groupResp = RetrofitClient.instance.getGroupDetails("Bearer $token", groupId)
                
                pbLoading.visibility = View.GONE

                if (response.isSuccessful && groupResp.isSuccessful) {
                    val allUsers = response.body() ?: emptyList()
                    val currentMembers = groupResp.body()?.members?.mapNotNull { it.id } ?: emptyList()
                    
                    // Filtrer pour ne garder que ceux qui ne sont pas déjà dans le groupe
                    val available = allUsers.filter { it.id !in currentMembers }
                    
                    if (available.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        tvEmpty.text = "Aucun nouveau membre à ajouter."
                    } else {
                        adapter.updateData(available)
                    }
                } else {
                    Toast.makeText(this@AddGroupMembersActivity, "Erreur serveur", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                pbLoading.visibility = View.GONE
                Toast.makeText(this@AddGroupMembersActivity, "Erreur de chargement", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addMembers() {
        val token = sessionManager.fetchAuthToken() ?: return
        val btn = findViewById<MaterialButton>(R.id.btnAddMembers)
        btn.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.addMembersToGroup(
                    "Bearer $token",
                    groupId,
                    AddMembersRequest(selectedIds)
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@AddGroupMembersActivity, "Membres ajoutés !", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@AddGroupMembersActivity, "Erreur lors de l'ajout", Toast.LENGTH_SHORT).show()
                    btn.isEnabled = true
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddGroupMembersActivity, "Erreur réseau : ${e.message}", Toast.LENGTH_LONG).show()
                btn.isEnabled = true
                Log.e("ADD_MEMBERS", "Network Error", e)
            }
        }
    }
}
