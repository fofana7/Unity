package com.example.unity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FriendSuggestionsAdapter(
    private var users: List<UserResponse>,
    private var myFriendIds: Set<Int> = emptySet(),
    private val currentUserId: Int = -1,
    private val onAddFriendClick: (UserResponse) -> Unit
) : RecyclerView.Adapter<FriendSuggestionsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInitials: TextView = view.findViewById(R.id.tvInitials)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val btnAdd: com.google.android.material.button.MaterialButton = view.findViewById(R.id.btnAdd)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_suggestion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]

        val firstName = user.firstName ?: ""
        val lastName = user.lastName ?: ""
        val fullName = if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
            "$firstName $lastName".trim()
        } else {
            user.username ?: "Utilisateur"
        }

        holder.tvName.text = fullName
        holder.tvUsername.text = "@${user.username?.replace(" ", "")?.lowercase()}"
        
        holder.tvInitials.text = if (fullName.isNotEmpty()) fullName[0].uppercase() else "U"

        // Vérifier si c'est déjà un ami ou soi-même
        val isFriend = myFriendIds.contains(user.id)
        val isMe = user.id == currentUserId

        if (isMe) {
            holder.btnAdd.visibility = View.GONE
        } else {
            holder.btnAdd.visibility = View.VISIBLE
            if (isFriend) {
                holder.btnAdd.text = "Ami"
                holder.btnAdd.isEnabled = false
                holder.btnAdd.alpha = 0.6f
            } else {
                holder.btnAdd.text = "Ajouter"
                holder.btnAdd.isEnabled = true
                holder.btnAdd.alpha = 1.0f
            }
        }

        holder.btnAdd.setOnClickListener {
            onAddFriendClick(user)
        }

        holder.itemView.setOnClickListener {
            val context = it.context
            android.util.Log.d("PROFILE_CLICK", "Clicked on suggestion: ${user.username} with ID: ${user.id}")
            val intent = android.content.Intent(context, ProfileActivity::class.java)
            intent.putExtra("userId", user.id ?: -1)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = users.size

    fun updateData(newUsers: List<UserResponse>, newFriendIds: Set<Int>) {
        users = newUsers
        myFriendIds = newFriendIds
        notifyDataSetChanged()
    }
    
    fun getCurrentUsers(): List<UserResponse> = users
}
