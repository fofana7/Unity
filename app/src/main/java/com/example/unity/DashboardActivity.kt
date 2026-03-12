package com.example.unity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private lateinit var llFeed: LinearLayout
    private lateinit var db: AppDatabase
    private var currentUsername: String = "Utilisateur"
    private var currentUserEmail: String? = null
    private var selectedUri: Uri? = null
    private var isImageSelected: Boolean = false
    
    private lateinit var llAttachmentPreview: LinearLayout
    private lateinit var ivPreview: ImageView
    private lateinit var tvAttachmentName: TextView

    private val pickFile = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedUri = result.data?.data
            if (selectedUri != null) {
                showAttachmentPreview(selectedUri!!)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)
        
        db = AppDatabase(this)
        currentUserEmail = intent.getStringExtra("USER_EMAIL")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val tvTopUsername = findViewById<TextView>(R.id.tvTopUsername)
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val btnLogout = findViewById<TextView>(R.id.btnLogout)
        val etNewPost = findViewById<EditText>(R.id.etNewPost)
        val btnPublish = findViewById<TextView>(R.id.btnPublish)
        val btnChoosePhoto = findViewById<ImageView>(R.id.btnChoosePhoto)
        val btnChooseFile = findViewById<ImageView>(R.id.btnChooseFile)
        val ivRemoveAttachment = findViewById<ImageView>(R.id.ivRemoveAttachment)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        
        llAttachmentPreview = findViewById(R.id.llAttachmentPreview)
        ivPreview = findViewById(R.id.ivPreview)
        tvAttachmentName = findViewById(R.id.tvAttachmentName)
        llFeed = findViewById(R.id.llFeed)

        if (currentUserEmail != null) {
            currentUsername = db.getUserName(currentUserEmail!!)
            tvTopUsername.text = currentUsername
            tvWelcome.text = "Ravi de vous revoir, $currentUsername !"
            loadSavedPosts()
        }

        btnChoosePhoto.setOnClickListener {
            isImageSelected = true
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            pickFile.launch(intent)
        }

        btnChooseFile.setOnClickListener {
            isImageSelected = false
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            pickFile.launch(intent)
        }

        ivRemoveAttachment.setOnClickListener { resetAttachment() }

        btnPublish.setOnClickListener {
            val content = etNewPost.text.toString().trim()
            if (content.isNotEmpty() || selectedUri != null) {
                val time = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                
                db.savePost(currentUsername, content, selectedUri?.toString(), isImageSelected, time)
                addNewPostToView(currentUsername, content, selectedUri?.toString(), isImageSelected, time)
                
                etNewPost.text.clear()
                resetAttachment()
            }
        }

        btnLogout.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("USER_EMAIL", currentUserEmail)
                    startActivity(intent)
                    true
                }
                R.id.nav_messages -> {
                    val intent = Intent(this, MessagesActivity::class.java)
                    intent.putExtra("USER_EMAIL", currentUserEmail)
                    startActivity(intent)
                    true
                }
                else -> true
            }
        }
    }

    private fun loadSavedPosts() {
        llFeed.removeAllViews()
        val posts = db.getAllPosts()
        for (post in posts) {
            addNewPostToView(post.author, post.content, post.uri, post.isImage, post.time)
        }
    }

    private fun addNewPostToView(author: String, content: String, uriString: String?, isImage: Boolean, time: String) {
        val inflater = LayoutInflater.from(this)
        val postView = inflater.inflate(R.layout.item_post, llFeed, false)

        postView.findViewById<TextView>(R.id.tvPostAuthor).text = author
        postView.findViewById<TextView>(R.id.tvPostHandle).text = "@${author.replace(" ", "").lowercase()} · $time"
        
        val tvContent = postView.findViewById<TextView>(R.id.tvPostContent)
        if (content.isEmpty()) tvContent.visibility = View.GONE else tvContent.text = content

        val ivPostImage = postView.findViewById<ImageView>(R.id.ivPostImage)
        val llFileAttachment = postView.findViewById<LinearLayout>(R.id.llFileAttachment)
        val tvFileNamePost = postView.findViewById<TextView>(R.id.tvFileNamePost)

        if (uriString != null) {
            val uri = Uri.parse(uriString)
            if (isImage) {
                ivPostImage.visibility = View.VISIBLE
                ivPostImage.setImageURI(uri)
            } else {
                llFileAttachment.visibility = View.VISIBLE
                tvFileNamePost.text = getFileName(uri)
            }
        }
        llFeed.addView(postView, 0)
    }

    private fun showAttachmentPreview(uri: Uri) {
        llAttachmentPreview.visibility = View.VISIBLE
        tvAttachmentName.text = getFileName(uri)
        if (isImageSelected) {
            ivPreview.setImageURI(uri)
            ivPreview.scaleType = ImageView.ScaleType.CENTER_CROP
        } else {
            ivPreview.setImageResource(android.R.drawable.ic_menu_save)
            ivPreview.scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
    }

    private fun resetAttachment() {
        selectedUri = null
        llAttachmentPreview.visibility = View.GONE
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor.use { if (it != null && it.moveToFirst()) result = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) }
        }
        if (result == null) {
            result = uri.path?.substringAfterLast('/')
        }
        return result ?: "Fichier"
    }
}