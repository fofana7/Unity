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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var db: AppDatabase
    private lateinit var llFeed: LinearLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var currentUser: UserResponse? = null
    private var allPosts: MutableList<PostResponse> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        sessionManager = SessionManager(this)
        db = AppDatabase(this)
        llFeed = findViewById(R.id.llFeed)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        
        val tvTopUsername = findViewById<TextView>(R.id.tvTopUsername)
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val btnLogout = findViewById<TextView>(R.id.btnLogout)
        val etNewPost = findViewById<EditText>(R.id.etNewPost)
        val btnPublish = findViewById<Button>(R.id.btnPublish)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val btnFriendSuggestions = findViewById<Button>(R.id.btnFriendSuggestions)

        fetchUserProfile(tvTopUsername, tvWelcome)
        loadPostsFromServer()

        // Redirection vers la Recherche
        findViewById<View>(R.id.btnSearchLink)?.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        swipeRefresh.setOnRefreshListener {
            loadPostsFromServer()
        }

        btnFriendSuggestions?.setOnClickListener {
            startActivity(Intent(this, FriendSuggestionsActivity::class.java))
        }

        // --- BOUTON PUBLIER ---
        btnPublish?.setOnClickListener {
            val content = etNewPost.text.toString().trim()
            if (content.isNotEmpty()) {
                publishPostToServer(content)
                etNewPost.text.clear()
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
                R.id.nav_home -> true
                R.id.nav_friends -> {
                    startActivity(Intent(this, FriendsActivity::class.java))
                    true
                }
                R.id.nav_messages -> {
                    startActivity(Intent(this, MessagesActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
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
                    currentUser = response.body()!!.user
                    val name = currentUser?.firstName ?: currentUser?.username ?: "Utilisateur"
                    tvUsername?.text = name
                    tvWelcome?.text = "Bienvenue, $name !"
                }
            } catch (e: Exception) {
                Log.e("DASHBOARD", "Erreur réseau", e)
            }
        }
    }

    private fun loadPostsFromServer() {
        val token = sessionManager.fetchAuthToken() ?: return
        swipeRefresh.isRefreshing = true
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getPosts("Bearer $token")
                if (response.isSuccessful) {
                    allPosts = (response.body() ?: emptyList()).toMutableList()
                    displayPosts()
                } else {
                    Toast.makeText(this@DashboardActivity, "Impossible de charger le fil", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("DASHBOARD", "Load posts failed", e)
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun displayPosts() {
        llFeed.removeAllViews()
        for (post in allPosts) {
            addPostView(post)
        }
    }

    private fun publishPostToServer(content: String) {
        val token = sessionManager.fetchAuthToken() ?: return
        val request = CreatePostRequest(content = content)
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.createPost("Bearer $token", request)
                if (response.isSuccessful) {
                    Toast.makeText(this@DashboardActivity, "Publié avec succès !", Toast.LENGTH_SHORT).show()
                    loadPostsFromServer()
                } else {
                    Toast.makeText(this@DashboardActivity, "Erreur de publication", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("DASHBOARD", "Publish failed", e)
            }
        }
    }

    private fun addPostView(post: PostResponse) {
        val inflater = LayoutInflater.from(this)
        val postView = inflater.inflate(R.layout.item_post, llFeed, false)

        val author = post.authorName ?: post.user?.username ?: "Utilisateur"
        val initials = author.take(1).uppercase()
        
        postView.findViewById<TextView>(R.id.tvPostAuthor).text = author
        postView.findViewById<TextView>(R.id.tvPostHandle).text = "@${author.lowercase().replace(" ", "")}"
        postView.findViewById<TextView>(R.id.tvPostContent).text = post.content
        postView.findViewById<TextView>(R.id.tvPostInitials).text = initials
        postView.findViewById<TextView>(R.id.tvPostTime).text = formatTime(post.createdAt)
        
        // Likes (Visual feedback)
        val tvLikeCount = postView.findViewById<TextView>(R.id.tvLikeCount)
        val ivLikeIcon = postView.findViewById<ImageView>(R.id.ivLikeIcon)
        tvLikeCount.text = post.likesCount.toString()
        
        postView.findViewById<View>(R.id.btnLike).setOnClickListener {
            handleLike(post, ivLikeIcon, tvLikeCount)
        }

        llFeed.addView(postView, 0)
    }

    private fun handleLike(post: PostResponse, icon: ImageView, text: TextView) {
        val token = sessionManager.fetchAuthToken() ?: return
        lifecycleScope.launch {
            try {
                // Optimistic UI update
                if (!post.isLiked) {
                    post.isLiked = true
                    post.likesCount++
                    icon.setImageResource(android.R.drawable.btn_star_big_on)
                    icon.setColorFilter(getColor(R.color.unity_accent))
                }
                text.text = post.likesCount.toString()
                
                RetrofitClient.instance.likePost("Bearer $token", post.id)
            } catch (e: Exception) {
                // Revert on error if needed
            }
        }
    }

    private fun formatTime(dateStr: String): String {
        return try {
            // Simplified for the demo
            "maintenant"
        } catch (e: Exception) {
            "..."
        }
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
