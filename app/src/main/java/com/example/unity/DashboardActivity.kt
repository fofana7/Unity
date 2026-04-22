package com.example.unity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var db: AppDatabase
    private lateinit var rvFeed: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var postAdapter: PostAdapter
    private lateinit var ivSelectedImage: ImageView
    
    private var currentUser: UserResponse? = null
    private var allPosts: MutableList<PostResponse> = mutableListOf()
    private var selectedImageBase64: String? = null
    private var isPolling = true

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                ivSelectedImage.setImageURI(it)
                ivSelectedImage.visibility = View.VISIBLE
                val inputStream = contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val outputStream = ByteArrayOutputStream()
                bitmap?.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
                val byteArray = outputStream.toByteArray()
                selectedImageBase64 = "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
            } catch (e: Exception) {
                Log.e("DASHBOARD", "Erreur image", e)
                Toast.makeText(this, "Impossible de charger l'image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isPolling = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        sessionManager = SessionManager(this)
        db = AppDatabase(this)
        rvFeed = findViewById(R.id.rvFeed)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        ivSelectedImage = findViewById(R.id.ivSelectedImage)
        
        val tvTopUsername = findViewById<TextView>(R.id.tvTopUsername)
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val btnLogout = findViewById<TextView>(R.id.btnLogout)
        val btnNotifications = findViewById<View>(R.id.btnNotifications)
        val etNewPost = findViewById<EditText>(R.id.etNewPost)
        val btnPickImage = findViewById<View>(R.id.btnPickImage)
        val btnPublish = findViewById<Button>(R.id.btnPublish)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        fetchUserProfile(tvTopUsername, tvWelcome)

        postAdapter = PostAdapter(
            posts = emptyList(),
            currentUserId = sessionManager.fetchUserId(),
            onLikeClick = { post, icon, text -> handleLike(post, icon, text) },
            onCommentClick = { post -> showCommentDialog(post) },
            onOptionsClick = { post, view -> showPostOptions(post, view) }
        )
        rvFeed.layoutManager = LinearLayoutManager(this)
        rvFeed.adapter = postAdapter

        loadPostsFromServer()
        startFeedPolling()

        findViewById<View>(R.id.btnSearchLink)?.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        swipeRefresh.setOnRefreshListener {
            loadPostsFromServer()
        }

        btnPickImage?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnPublish?.setOnClickListener {
            val content = etNewPost.text.toString().trim()
            if (content.isNotEmpty() || selectedImageBase64 != null) {
                publishPostToServer(content)
                etNewPost.text.clear()
            } else {
                Toast.makeText(this, "Veuillez écrire un message", Toast.LENGTH_SHORT).show()
            }
        }

        btnLogout?.setOnClickListener {
            sessionManager.clearSession()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        btnNotifications?.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
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
                    
                    // Mise à jour de l'adaptateur avec le bon ID utilisateur pour les permissions
                    postAdapter.currentUserId = currentUser?.id ?: -1
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
                }
            } catch (e: Exception) {
                Log.e("DASHBOARD", "Load error", e)
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun startFeedPolling() {
        lifecycleScope.launch {
            while (isPolling) {
                kotlinx.coroutines.delay(10000)
                silentLoadPosts()
            }
        }
    }

    private suspend fun silentLoadPosts() {
        val token = sessionManager.fetchAuthToken() ?: return
        try {
            val response = RetrofitClient.instance.getPosts("Bearer $token")
            if (response.isSuccessful) {
                val newPosts = (response.body() ?: emptyList())
                if (newPosts.size != allPosts.size || newPosts.firstOrNull()?.id != allPosts.firstOrNull()?.id) {
                    allPosts = newPosts.toMutableList()
                    displayPosts()
                }
            }
        } catch (e: Exception) {}
    }

    private fun displayPosts() {
        postAdapter.updateData(allPosts)
    }

    private fun publishPostToServer(content: String) {
        val token = sessionManager.fetchAuthToken() ?: return
        val request = CreatePostRequest(content = content, imageUrl = selectedImageBase64)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.createPost("Bearer $token", request)
                if (response.isSuccessful) {
                    Toast.makeText(this@DashboardActivity, "Publié !", Toast.LENGTH_SHORT).show()
                    selectedImageBase64 = null
                    ivSelectedImage.visibility = View.GONE
                    loadPostsFromServer()
                }
            } catch (e: Exception) {
                Log.e("DASHBOARD", "Publish error", e)
            }
        }
    }

    private fun showPostOptions(post: PostResponse, view: View) {
        // On utilise l'ID de session (toujours dispo) plutôt que currentUser qui peut être null
        val myId = sessionManager.fetchUserId()
        if (myId <= 0 || post.authorId != myId) {
            Toast.makeText(this, "Vous ne pouvez modifier que vos propres posts", Toast.LENGTH_SHORT).show()
            return
        }

        val popup = PopupMenu(this, view)
        popup.menu.add(0, 1, 0, "✏️ Modifier le texte")
        popup.menu.add(0, 2, 1, "🗑️ Supprimer")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> editPost(post)
                2 -> deletePost(post)
            }
            true
        }
        popup.show()
    }

    private fun editPost(post: PostResponse) {
        val editText = EditText(this)
        editText.setText(post.content)
        android.app.AlertDialog.Builder(this)
            .setTitle("Modifier le post")
            .setView(editText)
            .setPositiveButton("Enregistrer") { _, _ ->
                val newContent = editText.text.toString().trim()
                if (newContent.isNotEmpty()) {
                    updatePostAction(post.id, newContent, post.imageUrl)
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun updatePostAction(postId: Int, newContent: String, imageUrl: String?) {
        val token = sessionManager.fetchAuthToken() ?: return
        val request = CreatePostRequest(content = newContent, imageUrl = imageUrl)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.updatePost("Bearer $token", postId, request)
                if (response.isSuccessful) {
                    Toast.makeText(this@DashboardActivity, "Modifié ✓", Toast.LENGTH_SHORT).show()
                    loadPostsFromServer()
                } else {
                    val code = response.code()
                    val errorMsg = when(code) {
                        403 -> "Action interdite : vous n'êtes pas l'auteur."
                        401 -> "Session expirée."
                        404 -> "Post introuvable."
                        else -> "Erreur $code : impossible de modifier."
                    }
                    Toast.makeText(this@DashboardActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("DASHBOARD", "Update failed", e)
            }
        }
    }

    private fun deletePost(post: PostResponse) {
        val token = sessionManager.fetchAuthToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.deletePost("Bearer $token", post.id)
                if (response.isSuccessful) {
                    Toast.makeText(this@DashboardActivity, "Supprimé", Toast.LENGTH_SHORT).show()
                    loadPostsFromServer()
                } else {
                    Toast.makeText(this@DashboardActivity, "Erreur suppression", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {}
        }
    }

    private fun handleLike(post: PostResponse, icon: ImageView, text: TextView) {
        val token = sessionManager.fetchAuthToken() ?: return
        
        // Optimistic UI Update (immédiat pour une sensation réaliste et rapide)
        val wasLiked = post.isLiked
        post.isLiked = !wasLiked
        post.likesCount += if (post.isLiked) 1 else -1

        text.text = post.likesCount.toString()
        if (post.isLiked) {
            icon.setImageResource(android.R.drawable.btn_star_big_on)
            icon.setColorFilter(getColor(R.color.unity_accent))
        } else {
            icon.setImageResource(android.R.drawable.btn_star_big_off)
            icon.clearColorFilter()
        }

        // Requête réseau en arrière-plan
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.likePost("Bearer $token", post.id)
                if (!response.isSuccessful) {
                    // Rollback en cas d'erreur serveur
                    post.isLiked = wasLiked
                    post.likesCount += if (post.isLiked) 1 else -1
                    text.text = post.likesCount.toString()
                    if (post.isLiked) {
                        icon.setImageResource(android.R.drawable.btn_star_big_on)
                        icon.setColorFilter(getColor(R.color.unity_accent))
                    } else {
                        icon.setImageResource(android.R.drawable.btn_star_big_off)
                        icon.clearColorFilter()
                    }
                }
            } catch (e: Exception) {
                // Rollback en cas d'absence de connexion
                post.isLiked = wasLiked
                post.likesCount += if (post.isLiked) 1 else -1
                text.text = post.likesCount.toString()
                if (post.isLiked) {
                    icon.setImageResource(android.R.drawable.btn_star_big_on)
                    icon.setColorFilter(getColor(R.color.unity_accent))
                } else {
                    icon.setImageResource(android.R.drawable.btn_star_big_off)
                    icon.clearColorFilter()
                }
            }
        }
    }

    private fun showCommentDialog(post: PostResponse) {
        val bottomSheet = CommentsBottomSheetFragment(post.id).apply {
            onCommentAdded = {
                // Optimistic UI pour la liste de posts : incrémenter et rafraîchir
                post.commentsCount++
                postAdapter.notifyDataSetChanged()
            }
        }
        bottomSheet.show(supportFragmentManager, "CommentsBottomSheet")
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
