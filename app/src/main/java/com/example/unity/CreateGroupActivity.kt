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
    private var selectedFriendIds: List<Int> = emptyList()

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

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val rvFriends = findViewById<RecyclerView>(R.id.rvFriends)
        rvFriends.layoutManager = LinearLayoutManager(this)
        
        adapter = SelectableFriendAdapter(emptyList()) { ids ->
            selectedFriendIds = ids
            val btnCreate = findViewById<MaterialButton>(R.id.btnCreateGroup)
            btnCreate.text = "Créer le groupe (${ids.size} ami${if (ids.size > 1) "s" else ""})"
        }
        rvFriends.adapter = adapter

        findViewById<MaterialButton>(R.id.btnCreateGroup).setOnClickListener {
            createGroup()
        }

        loadFriends()
    }

    private fun loadFriends() {
        val token = sessionManager.fetchAuthToken() ?: return
        val pbLoading = findViewById<ProgressBar>(R.id.pbLoading)
        val tvNoFriends = findViewById<TextView>(R.id.tvNoFriends)
        
        pbLoading.visibility = View.VISIBLE
        tvNoFriends.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getMyFriends("Bearer $token")
                pbLoading.visibility = View.GONE
                
                if (response.isSuccessful && response.body() != null) {
                    val friends = response.body()!!
                    if (friends.isEmpty()) {
                        tvNoFriends.visibility = View.VISIBLE
                    } else {
                        adapter.updateData(friends)
                    }
                } else {
                    tvNoFriends.visibility = View.VISIBLE
                    tvNoFriends.text = "Erreur de chargement des amis."
                }
            } catch (e: Exception) {
                pbLoading.visibility = View.GONE
                tvNoFriends.visibility = View.VISIBLE
                tvNoFriends.text = "Erreur réseau."
            }
        }
    }

    private fun createGroup() {
        val name = findViewById<EditText>(R.id.etGroupName).text.toString().trim()
        val desc = findViewById<EditText>(R.id.etGroupDesc).text.toString().trim()
        
        if (name.isEmpty()) {
            Toast.makeText(this, "Le nom du groupe est obligatoire.", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (selectedFriendIds.isEmpty()) {
            Toast.makeText(this, "Sélectionnez au moins un ami.", Toast.LENGTH_SHORT).show()
            return
        }

        val btnCreate = findViewById<MaterialButton>(R.id.btnCreateGroup)
        btnCreate.isEnabled = false
        btnCreate.text = "Création..."

        val request = CreateGroupRequest(
            name = name,
            description = desc.ifEmpty { null },
            memberIds = selectedFriendIds
        )
        
        val token = sessionManager.fetchAuthToken() ?: return
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.createGroup("Bearer $token", request)
                if (response.isSuccessful) {
                    Toast.makeText(this@CreateGroupActivity, "Groupe créé !", Toast.LENGTH_SHORT).show()
                    finish() // Retour à MessagesActivity où les groupes seront rechargés
                } else {
                    Log.e("CREATE_GROUP", "Erreur : ${response.code()}")
                    Toast.makeText(this@CreateGroupActivity, "Erreur serveur.", Toast.LENGTH_SHORT).show()
                    btnCreate.isEnabled = true
                    btnCreate.text = "Créer le groupe (${selectedFriendIds.size} amis)"
                }
            } catch (e: Exception) {
                Log.e("CREATE_GROUP", "Erreur", e)
                Toast.makeText(this@CreateGroupActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
                btnCreate.isEnabled = true
                btnCreate.text = "Créer le groupe (${selectedFriendIds.size} amis)"
            }
        }
    }
}
