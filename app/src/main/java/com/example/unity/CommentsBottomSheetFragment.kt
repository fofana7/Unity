package com.example.unity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommentsBottomSheetFragment(private val postId: Int) : BottomSheetDialogFragment() {

    var onCommentAdded: (() -> Unit)? = null
    
    private lateinit var rvComments: RecyclerView
    private lateinit var etNewComment: EditText
    private lateinit var btnSendComment: ImageButton
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: CommentAdapter
    private var commentsList = mutableListOf<CommentResponse>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_comments, container, false)

        rvComments = view.findViewById(R.id.rvComments)
        etNewComment = view.findViewById(R.id.etNewComment)
        btnSendComment = view.findViewById(R.id.btnSendComment)
        pbLoading = view.findViewById(R.id.pbCommentsLoading)
        tvEmpty = view.findViewById(R.id.tvEmptyComments)

        adapter = CommentAdapter(commentsList)
        rvComments.layoutManager = LinearLayoutManager(context)
        rvComments.adapter = adapter

        fetchComments()

        btnSendComment.setOnClickListener {
            val text = etNewComment.text.toString().trim()
            if (text.isNotEmpty()) {
                postComment(text)
            }
        }

        return view
    }

    private fun fetchComments() {
        val sessionManager = SessionManager(requireContext())
        val tokenStr = sessionManager.fetchAuthToken() ?: return
        val token = "Bearer $tokenStr"

        pbLoading.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        rvComments.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getComments(token, postId)
                withContext(Dispatchers.Main) {
                    pbLoading.visibility = View.GONE
                    if (response.isSuccessful) {
                        val body = response.body() ?: emptyList()
                        commentsList.clear()
                        commentsList.addAll(body)
                        adapter.updateComments(commentsList)
                        
                        if (commentsList.isEmpty()) {
                            tvEmpty.visibility = View.VISIBLE
                        } else {
                            rvComments.visibility = View.VISIBLE
                        }
                    } else {
                        Toast.makeText(context, "Erreur de chargement", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pbLoading.visibility = View.GONE
                    Toast.makeText(context, "Erreur de connexion", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun postComment(content: String) {
        val sessionManager = SessionManager(requireContext())
        val tokenStr = sessionManager.fetchAuthToken() ?: return
        val token = "Bearer $tokenStr"
        
        btnSendComment.isEnabled = false

        // Optimistic UI : on crée un commentaire factice avant la confirmation
        val currentUserId = sessionManager.fetchUserId()
        val currentUsername = "Moi" // On peut la faire avancer par la suite
        
        val optimisticComment = CommentResponse(
            id = -1,
            content = content,
            userId = currentUserId,
            username = currentUsername,
            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()).format(java.util.Date())
        )
        commentsList.add(optimisticComment)
        adapter.updateComments(commentsList)
        rvComments.scrollToPosition(commentsList.size - 1)
        rvComments.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        etNewComment.text.clear()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val body = mapOf("content" to content)
                val response = RetrofitClient.instance.addComment(token, postId, body)
                
                withContext(Dispatchers.Main) {
                    btnSendComment.isEnabled = true
                    if (response.isSuccessful) {
                        fetchComments()
                        onCommentAdded?.invoke()
                    } else {
                        // Rollback si échec
                        commentsList.remove(optimisticComment)
                        adapter.updateComments(commentsList)
                        Toast.makeText(context, "Erreur d'envoi", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnSendComment.isEnabled = true
                    commentsList.remove(optimisticComment)
                    adapter.updateComments(commentsList)
                    Toast.makeText(context, "Erreur réseau", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
