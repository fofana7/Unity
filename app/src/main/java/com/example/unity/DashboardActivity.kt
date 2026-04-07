package com.example.unity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var db: AppDatabase
    private lateinit var llFeed: LinearLayout
    private var currentUser: UserResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        sessionManager = SessionManager(this)
        db = AppDatabase(this)
        llFeed = findViewById(R.id.llFeed)
        
        val topBar = findViewById<View>(R.id.topBar)
        if (topBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(topBar) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
                insets
            }
        }

        val tvTopUsername = findViewById<TextView>(R.id.tvTopUsername)
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val btnLogout = findViewById<TextView>(R.id.btnLogout)
        val etNewPost = findViewById<EditText>(R.id.etNewPost)
        val btnPublish = findViewById<Button>(R.id.btnPublish)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val btnFriendSuggestions = findViewById<Button>(R.id.btnFriendSuggestions)

        fetchUserProfile(tvTopUsername, tvWelcome)
        loadSavedPosts()

        btnFriendSuggestions?.setOnClickListener {
            startActivity(Intent(this, FriendSuggestionsActivity::class.java))
        }

        // --- BOUTON PUBLIER ---
        btnPublish?.setOnClickListener {
            val content = etNewPost.text.toString().trim()
            if (content.isNotEmpty()) {
                val author = currentUser?.username ?: currentUser?.firstName ?: "Anonyme"
                val time = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
                
                // Sauvegarde locale
                db.savePost(author, content, null, false, time)
                
                // Affichage immédiat
                addNewPostToView(author, content, time)
                
                etNewPost.text.clear()
                Toast.makeText(this, "Publié !", Toast.LENGTH_SHORT).show()
                
                // On affiche aussi les logs pour vérifier
                db.debugLogDatabase()
            }
        }

        btnLogout?.setOnClickListener {
            sessionManager.clearSession()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.nav_messages -> {
                    startActivity(Intent(this, MessagesActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, AtelierGUIavance::class.java))
                    true
                }
                else -> true
            }
        }
    }

    private fun fetchUserProfile(tvUsername: TextView?, tvWelcome: TextView?) {
        val token = sessionManager.fetchAuthToken() ?: return redirectToLogin()
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getMe("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    currentUser = response.body()
                    val name = currentUser?.username ?: "Utilisateur"
                    tvUsername?.text = name
                    tvWelcome?.text = "Bienvenue, $name !"
                }
            } catch (e: Exception) {
                Log.e("DASHBOARD", "Erreur réseau", e)
            }
        }
    }

    private fun loadSavedPosts() {
        llFeed.removeAllViews()
        val posts = db.getAllPosts()
        for (post in posts) {
            addNewPostToView(post.author, post.content, post.time)
        }
    }

    private fun addNewPostToView(author: String, content: String, time: String) {
        val inflater = LayoutInflater.from(this)
        val postView = inflater.inflate(R.layout.item_post, llFeed, false)

        postView.findViewById<TextView>(R.id.tvPostAuthor).text = author
        postView.findViewById<TextView>(R.id.tvPostHandle).text = "@${author.replace(" ", "").lowercase()} · $time"
        postView.findViewById<TextView>(R.id.tvPostContent).text = content
        
        llFeed.addView(postView, 0) // Ajoute en haut de la liste
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
