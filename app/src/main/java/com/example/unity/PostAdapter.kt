package com.example.unity

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PostAdapter(
    private var posts: List<PostResponse>,
    var currentUserId: Int, // Rendu public et mutable
    private val onLikeClick: (PostResponse, ImageView, TextView) -> Unit,
    private val onCommentClick: (PostResponse) -> Unit,
    private val onOptionsClick: (PostResponse, View) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPostAuthor: TextView = view.findViewById(R.id.tvPostAuthor)
        val tvPostHandle: TextView = view.findViewById(R.id.tvPostHandle)
        val tvPostContent: TextView = view.findViewById(R.id.tvPostContent)
        val tvPostInitials: TextView = view.findViewById(R.id.tvPostInitials)
        val tvPostTime: TextView = view.findViewById(R.id.tvPostTime)
        val tvLikeCount: TextView = view.findViewById(R.id.tvLikeCount)
        val ivLikeIcon: ImageView = view.findViewById(R.id.ivLikeIcon)
        val btnLike: View = view.findViewById(R.id.btnLike)
        val btnComment: View = view.findViewById(R.id.btnComment)
        val tvCommentCount: TextView = view.findViewById(R.id.tvCommentCount)
        val ivOptions: ImageView = view.findViewById(R.id.ivOptions)
        val cvPostImage: View = view.findViewById(R.id.cvPostImage)
        val ivPostImage: ImageView = view.findViewById(R.id.ivPostImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        val authorName = post.displayName()
        holder.tvPostAuthor.text = authorName
        holder.tvPostHandle.text = "@${authorName.lowercase().replace(" ", "")}"
        holder.tvPostInitials.text = authorName.take(1).uppercase()
        holder.tvPostContent.text = post.content
        holder.tvPostTime.text = formatTime(post.createdAt)

        // Likes
        holder.tvLikeCount.text = post.likesCount.toString()
        if (post.isLiked) {
            holder.ivLikeIcon.setImageResource(android.R.drawable.btn_star_big_on)
            holder.ivLikeIcon.setColorFilter(holder.itemView.context.getColor(R.color.unity_accent))
        } else {
            holder.ivLikeIcon.setImageResource(android.R.drawable.btn_star_big_off)
            holder.ivLikeIcon.clearColorFilter()
        }
        holder.btnLike.setOnClickListener { onLikeClick(post, holder.ivLikeIcon, holder.tvLikeCount) }

        // Commentaires
        holder.tvCommentCount.text = post.commentsCount.toString()
        holder.btnComment.setOnClickListener { onCommentClick(post) }

        // Options (Modifier / Supprimer) — Uniquement si c'est notre post
        if (post.authorId == currentUserId) {
            holder.ivOptions.visibility = View.VISIBLE
            holder.ivOptions.setOnClickListener { onOptionsClick(post, holder.ivOptions) }
        } else {
            holder.ivOptions.visibility = View.GONE
        }

        // Image en Base64
        val imgSrc = post.displayImage()
        if (!imgSrc.isNullOrEmpty()) {
            try {
                val base64String = imgSrc.substringAfter("base64,")
                val bytes = Base64.decode(base64String, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.ivPostImage.setImageBitmap(bmp)
                holder.cvPostImage.visibility = View.VISIBLE
            } catch (e: Exception) {
                holder.cvPostImage.visibility = View.GONE
            }
        } else {
            holder.cvPostImage.visibility = View.GONE
        }
    }

    override fun getItemCount() = posts.size

    fun updateData(newPosts: List<PostResponse>) {
        posts = newPosts
        notifyDataSetChanged()
    }

    private fun formatTime(dateStr: String): String {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = sdf.parse(dateStr) ?: return ""
            val diff = (System.currentTimeMillis() - date.time) / 1000
            when {
                diff < 60 -> "à l'instant"
                diff < 3600 -> "${diff / 60}m"
                diff < 86400 -> "${diff / 3600}h"
                else -> "${diff / 86400}j"
            }
        } catch (e: Exception) { "" }
    }
}
