package com.example.unity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter(private var comments: List<CommentResponse>) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.ivCommentAvatar)
        val tvAuthor: TextView = view.findViewById(R.id.tvCommentAuthor)
        val tvTime: TextView = view.findViewById(R.id.tvCommentTime)
        val tvContent: TextView = view.findViewById(R.id.tvCommentContent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        
        holder.tvAuthor.text = comment.displayName()
        holder.tvContent.text = comment.content
        holder.tvTime.text = formatTime(comment.createdAt)

        // On affiche un placeholder basique en attendant Glide ou les vrais avatars
        holder.ivAvatar.setImageResource(android.R.drawable.ic_menu_camera)
    }

    override fun getItemCount() = comments.size

    fun updateComments(newComments: List<CommentResponse>) {
        comments = newComments
        notifyDataSetChanged()
    }

    private fun formatTime(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(dateStr) ?: return ""
            
            val diff = (System.currentTimeMillis() - date.time) / 1000
            when {
                diff < 60 -> "À l'instant"
                diff < 3600 -> "${diff / 60} min"
                diff < 86400 -> "${diff / 3600} h"
                else -> "${diff / 86400} j"
            }
        } catch (e: Exception) {
            ""
        }
    }
}
