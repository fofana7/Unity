package com.example.unity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {

    private lateinit var adapter: FriendAdapter
    private lateinit var sessionManager: SessionManager
    private var allUsers: List<UserResponse> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_search)

        sessionManager = SessionManager(this)
        
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etSearchUser = findViewById<EditText>(R.id.etSearchUser)
        val rvSearchResults = findViewById<RecyclerView>(R.id.rvSearchResults)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvEmptySearch = findViewById<TextView>(R.id.tvEmptySearch)

        btnBack.setOnClickListener { finish() }

        adapter = FriendAdapter(emptyList()) { user, type ->
            // Action à définir pour la recherche (ex: envoyer demande d'ami)
            Toast.makeText(this, "Action sur ${user.username}", Toast.LENGTH_SHORT).show()
        }

        rvSearchResults.layoutManager = LinearLayoutManager(this)
        rvSearchResults.adapter = adapter

        // On charge tous les utilisateurs pour filtrer localement (plus simple pour ce projet)
        loadAllUsers(progressBar)

        etSearchUser.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterUsers(s.toString(), tvEmptySearch)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadAllUsers(progressBar: ProgressBar) {
        val token = sessionManager.fetchAuthToken() ?: return
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getAllUsers("Bearer $token")
                if (response.isSuccessful) {
                    allUsers = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // Erreur silencieuse
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun filterUsers(query: String, tvEmpty: TextView) {
        if (query.isEmpty()) {
            adapter.updateData(emptyList())
            tvEmpty.visibility = View.GONE
            return
        }

        val filtered = allUsers.filter {
            it.username?.contains(query, ignoreCase = true) == true ||
            it.firstName?.contains(query, ignoreCase = true) == true ||
            it.lastName?.contains(query, ignoreCase = true) == true
        }

        val items = filtered.map { FriendItem(it, FriendActionType.SUGGESTION) }
        adapter.updateData(items)

        tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }
}
