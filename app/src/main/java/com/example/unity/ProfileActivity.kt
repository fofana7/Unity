package com.example.unity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
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

        findViewById<ImageButton>(R.id.btnShareProfile).setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            val username = currentUser?.username ?: ""
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Rejoins-moi sur Unity ! Mon profil : @$username")
            startActivity(Intent.createChooser(shareIntent, "Partager le profil"))
        }

        findViewById<android.view.View>(R.id.llFriendsCount).setOnClickListener {
            showFriendsDialog()
        }

        setupBottomNavigation()
    }



    private fun setupRecyclerView() {
        val rvUserPosts = findViewById<RecyclerView>(R.id.rvUserPosts)
        postAdapter = PostAdapter(
            emptyList(),
            sessionManager.fetchUserId(),
            onLikeClick = { post, icon, text -> handleLike(post, icon, text) },
            onCommentClick = { post -> showCommentDialog(post) },
            onOptionsClick = { post, view -> showPostOptions(post, view) }
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

    private fun showFriendsDialog() {
        val token = sessionManager.fetchAuthToken() ?: return
        val targetUserId = intent.getIntExtra("userId", -1)
        val myId = sessionManager.fetchUserId()
        val userId = if (targetUserId == -1) myId else targetUserId

        lifecycleScope.launch {
            try {
                val response = if (userId == myId) {
                    RetrofitClient.instance.getMyFriends("Bearer $token")
                } else {
                    RetrofitClient.instance.getFriendsById("Bearer $token", userId)
                }

                if (response.isSuccessful) {
                    val friends = response.body() ?: emptyList()
                    
                    if (friends.isEmpty()) {
                        Toast.makeText(this@ProfileActivity, "Aucun ami à afficher", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val title = "Amis de ${currentUser?.username ?: "l'utilisateur"}"
                    val bottomSheet = FriendsBottomSheetFragment(friends, title)
                    bottomSheet.show(supportFragmentManager, "FriendsBottomSheet")
                }
            } catch (e: Exception) {
                Log.e("FRIENDS_DIALOG", "Erreur", e)
            }
        }
    }

    private fun showCommentDialog(post: PostResponse) {
        val bottomSheet = CommentsBottomSheetFragment(post.id).apply {
            onCommentAdded = {
                post.commentsCount++
                postAdapter.notifyDataSetChanged()
            }
        }
        bottomSheet.show(supportFragmentManager, "CommentsBottomSheet")
    }

    private fun showPostOptions(post: PostResponse, view: View) {
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
        androidx.appcompat.app.AlertDialog.Builder(this)
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
                    Toast.makeText(this@ProfileActivity, "Modifié ✓", Toast.LENGTH_SHORT).show()
                    fetchProfileData() // Recharger tout
                }
            } catch (e: Exception) {
                Log.e("PROFILE", "Update failed", e)
            }
        }
    }

    private fun deletePost(post: PostResponse) {
        val token = sessionManager.fetchAuthToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.deletePost("Bearer $token", post.id)
                if (response.isSuccessful) {
                    Toast.makeText(this@ProfileActivity, "Supprimé", Toast.LENGTH_SHORT).show()
                    fetchProfileData() // Recharger tout
                }
            } catch (e: Exception) {
                Log.e("PROFILE", "Delete failed", e)
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
        val targetUserId = intent.getIntExtra("userId", -1)
        val myId = sessionManager.fetchUserId()
        val isMe = targetUserId == -1 || targetUserId == myId

        val tvName = findViewById<TextView>(R.id.tvProfileName)
        val tvHandle = findViewById<TextView>(R.id.tvProfileHandle)
        val tvRole = findViewById<TextView>(R.id.tvRoleName)
        val tvClass = findViewById<TextView>(R.id.tvProfileClass)
        val tvInit = findViewById<TextView>(R.id.tvProfileInitials)
        val tvBio = findViewById<TextView>(R.id.tvProfileBio)
        val tvPostCount = findViewById<TextView>(R.id.tvPostsCount)
        val tvFriendsCount = findViewById<TextView>(R.id.tvFriendsCount)
        val btnEdit = findViewById<View>(R.id.btnEditProfile)
        val btnAddFriend = findViewById<MaterialButton>(R.id.btnAddFriend)
        val btnSettings = findViewById<View>(R.id.btnSettings)
        val btnShare = findViewById<View>(R.id.btnShareProfile)

        btnEdit.visibility = if (isMe) View.VISIBLE else View.GONE
        btnAddFriend.visibility = if (!isMe) View.VISIBLE else View.GONE
        btnSettings.visibility = if (isMe) View.VISIBLE else View.GONE
        btnShare.visibility = if (isMe) View.VISIBLE else View.GONE

        lifecycleScope.launch {
            try {
                val user: UserResponse? = if (isMe) {
                    val resp = RetrofitClient.instance.getMe("Bearer $token")
                    if (resp.isSuccessful) resp.body()?.user else null
                } else {
                    val resp = RetrofitClient.instance.getUserById("Bearer $token", targetUserId)
                    if (resp.isSuccessful) resp.body() else null
                }

                if (user != null) {
                    currentUser = user
                    
                    val fName = user.firstName ?: ""
                    val lName = user.lastName ?: ""
                    val fullName = "$fName $lName".trim()

                    tvName.text = if (fullName.isNotEmpty()) fullName else user.username
                    tvHandle.text = "@${user.username}"
                    
                    val roleStr = user.role?.lowercase() ?: "eleve"
                    tvRole.text = when {
                        roleStr.contains("admin") -> "Administrateur"
                        roleStr.contains("prof") || roleStr.contains("enseignant") -> "Enseignant"
                        else -> "Étudiant"
                    }

                    tvClass.text = user.classe ?: "Non définie"
                    tvInit.text = (fName.ifEmpty { user.username } ?: "U").take(1).uppercase()
                    tvBio.text = if (user.bio.isNullOrEmpty()) "Aucune biographie disponible." else user.bio

                    if (!isMe) {
                        checkFriendshipStatus(user.id ?: 0, btnAddFriend)
                    }

                    fetchUserPosts(user.id ?: 0, tvPostCount)
                    fetchFriendsCount(user.id ?: 0, tvFriendsCount)
                } else {
                    Log.e("PROFILE", "Failed to load user data")
                    Toast.makeText(this@ProfileActivity, "Impossible de charger ce profil", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e("PROFILE_ERROR", "Network error: ${e.message}")
            }
        }
    }

    private fun fetchUserPosts(userId: Int, tvCount: TextView) {
        val token = sessionManager.fetchAuthToken() ?: return
        Log.d("PROFILE_POSTS", "Fetching posts for userId: $userId")
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getPosts("Bearer $token")
                if (response.isSuccessful) {
                    val allPosts: List<PostResponse> = response.body() ?: emptyList()
                    Log.d("PROFILE_POSTS", "Total posts received: ${allPosts.size}")
                    
                    val myPosts = allPosts.filter { post -> 
                        val match = post.authorId == userId
                        Log.d("PROFILE_POSTS", "Post ${post.id} authorId: ${post.authorId}, target: $userId, match: $match")
                        match
                    }
                    
                    tvCount.text = myPosts.size.toString()
                    Log.d("PROFILE_POSTS", "Filtered posts count: ${myPosts.size}")
                    
                    // Mettre à jour l'adapter avec les publications filtrées
                    postAdapter.updateData(myPosts)
                } else {
                    Log.e("PROFILE_POSTS", "Error: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("PROFILE_POSTS", "Erreur posts", e)
            }
        }
    }

    private fun fetchFriendsCount(userId: Int, tvCount: TextView) {
        val token = sessionManager.fetchAuthToken() ?: return
        val myId = sessionManager.fetchUserId()
        val isMe = userId == myId || userId == 0 || userId == -1

        lifecycleScope.launch {
            try {
                val response = if (isMe) {
                    RetrofitClient.instance.getMyFriends("Bearer $token")
                } else {
                    RetrofitClient.instance.getFriendsById("Bearer $token", userId)
                }

                if (response.isSuccessful) {
                    val friendsList: List<UserResponse> = response.body() ?: emptyList()
                    tvCount.text = friendsList.size.toString()
                }
            } catch (e: Exception) {
                Log.e("PROFILE_FRIENDS", "Erreur amis", e)
            }
        }
    }

    private fun checkFriendshipStatus(userId: Int, btn: MaterialButton) {
        val token = sessionManager.fetchAuthToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getMyFriends("Bearer $token")
                if (response.isSuccessful) {
                    val myFriends = response.body() ?: emptyList()
                    val isFriend = myFriends.any { it.id == userId }
                    
                    if (isFriend) {
                        btn.text = "Ami"
                        btn.setIconResource(android.R.drawable.checkbox_on_background)
                        btn.isEnabled = false
                        btn.alpha = 0.8f
                    } else {
                        btn.text = "Ajouter"
                        btn.setIconResource(android.R.drawable.ic_input_add)
                        btn.isEnabled = true
                        btn.alpha = 1.0f
                        btn.setOnClickListener {
                            handleAddFriendAction(userId, btn)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("FRIENDSHIP", "Error checking status", e)
            }
        }
    }

    private fun handleAddFriendAction(userId: Int, btn: MaterialButton) {
        val token = sessionManager.fetchAuthToken() ?: return
        val myId = sessionManager.fetchUserId()
        
        lifecycleScope.launch {
            try {
                val req = FriendActionRequest(friendId = userId, requesterId = myId, status = "pending")
                val response = RetrofitClient.instance.addFriend("Bearer $token", req)
                if (response.isSuccessful) {
                    Toast.makeText(this@ProfileActivity, "Demande envoyée !", Toast.LENGTH_SHORT).show()
                    btn.text = "En attente"
                    btn.isEnabled = false
                    btn.setIconResource(0)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("FRIENDSHIP", "Error adding friend: $errorBody")
                    val errorMsg = if (errorBody?.contains("Relation déjà existante") == true || response.code() == 400) {
                        "Déjà envoyé ou déjà amis"
                    } else {
                        "Erreur serveur (${response.code()})"
                    }
                    Toast.makeText(this@ProfileActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    if (errorBody?.contains("Relation déjà existante") == true) {
                        btn.text = "En attente"
                        btn.isEnabled = false
                        btn.setIconResource(0)
                    }
                }
            } catch (e: Exception) {
                Log.e("FRIENDSHIP", "Error adding friend", e)
                Toast.makeText(this@ProfileActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
