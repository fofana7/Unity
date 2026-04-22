package com.example.unity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var postAdapter: PostAdapter
    private var currentUser: UserResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        sessionManager = SessionManager(this)

        val appBar = findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appBar)
        if (appBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(appBar) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(0, systemBars.top, 0, 0)
                insets
            }
        }

        setupRecyclerView()
        fetchProfileData()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, AtelierGUIavance::class.java))
        }

        findViewById<MaterialButton>(R.id.btnEditProfile).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnShareProfile).setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            val username = currentUser?.username ?: ""
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Rejoins-moi sur Unity ! Mon profil : @$username")
            startActivity(Intent.createChooser(shareIntent, "Partager le profil"))
        }

        setupBottomNavigation()
    }

    private fun setupRecyclerView() {
        val rvUserPosts = findViewById<RecyclerView>(R.id.rvUserPosts)
        postAdapter = PostAdapter(
            emptyList(),
            0,
            onLikeClick = { post, icon, text -> handleLike(post, icon, text) },
            onCommentClick = { _ -> },
            onOptionsClick = { _, _ -> }
        )
        rvUserPosts.layoutManager = LinearLayoutManager(this)
        rvUserPosts.adapter = postAdapter
    }

    private fun handleLike(post: PostResponse, icon: android.widget.ImageView, text: TextView) {
        val token = sessionManager.fetchAuthToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.likePost("Bearer $token", post.id)
                if (response.isSuccessful) {
                    post.isLiked = !post.isLiked
                    if (post.isLiked) {
                        post.likesCount++
                        icon.setImageResource(android.R.drawable.btn_star_big_on)
                        icon.setColorFilter(getColor(R.color.unity_accent))
                    } else {
                        post.likesCount--
                        icon.setImageResource(android.R.drawable.btn_star_big_off)
                        icon.clearColorFilter()
                    }
                    text.text = post.likesCount.toString()
                }
            } catch (e: Exception) {
                Log.e("LIKE_ERROR", "Erreur lors du like", e)
            }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav?.selectedItemId = R.id.nav_profile
        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_friends -> {
                    startActivity(Intent(this, FriendsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_messages -> {
                    startActivity(Intent(this, MessagesActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> true
                else -> true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        fetchProfileData()
    }

    private fun fetchProfileData() {
        val token = sessionManager.fetchAuthToken() ?: return redirectToLogin()

        val tvName = findViewById<TextView>(R.id.tvFullName)
        val tvRole = findViewById<TextView>(R.id.tvUserRole)
        val tvClass = findViewById<TextView>(R.id.tvUserClass)
        val tvInit = findViewById<TextView>(R.id.tvInitials)
        val tvBio = findViewById<TextView>(R.id.tvBio)
        val tvPostCount = findViewById<TextView>(R.id.tvPostCount)
        val tvFriendsCount = findViewById<TextView>(R.id.tvFriendsCount)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getMe("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!.user
                    currentUser = user

                    val firstName = user.firstName ?: ""
                    val lastName = user.lastName ?: ""
                    val fullName = "$firstName $lastName".trim()

                    val displayRole = user.role?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: "Étudiant"

                    tvName.text = fullName.ifEmpty { user.username }
                    tvRole.text = "@${user.username} • $displayRole"
                    tvClass.text = if (user.classe.isNullOrEmpty()) "" else "Classe : ${user.classe}"

                    val initialsSource = firstName.ifEmpty { user.username ?: "U" }
                    tvInit.text = initialsSource.take(1).uppercase()

                    tvBio.text = if (user.bio.isNullOrEmpty()) "Aucune biographie" else user.bio

                    val userId = user.id ?: 0
                    postAdapter.currentUserId = userId

                    fetchUserPosts(userId, tvPostCount)
                    fetchFriendsCount(tvFriendsCount)
                }
            } catch (e: Exception) {
                Log.e("PROFILE_ERROR", "Erreur réseau", e)
            }
        }
    }

    private fun fetchUserPosts(userId: Int, tvCount: TextView) {
        val token = sessionManager.fetchAuthToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getPosts("Bearer $token")
                if (response.isSuccessful) {
                    val allPosts: List<PostResponse> = response.body() ?: emptyList()
                    val myPosts = allPosts.filter { post -> post.authorId == userId }
                    postAdapter.updateData(myPosts)
                    tvCount.text = myPosts.size.toString()
                }
            } catch (e: Exception) {
                Log.e("PROFILE_POSTS", "Erreur posts", e)
            }
        }
    }

    private fun fetchFriendsCount(tvCount: TextView) {
        val token = sessionManager.fetchAuthToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getMyFriends("Bearer $token")
                if (response.isSuccessful) {
                    val friendsList: List<UserResponse> = response.body() ?: emptyList()
                    tvCount.text = friendsList.size.toString()
                }
            } catch (e: Exception) {
                Log.e("PROFILE_FRIENDS", "Erreur amis", e)
            }
        }
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
