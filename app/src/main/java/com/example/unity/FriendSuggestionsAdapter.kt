package com.example.unity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FriendSuggestionsAdapter(
    private var users: List<UserResponse>,
    private val onAddFriendClick: (UserResponse) -> Unit
) : RecyclerView.Adapter<FriendSuggestionsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInitials: TextView = view.findViewById(R.id.tvInitials)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val btnAdd: Button = view.findViewById(R.id.btnAdd)
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

        holder.btnAdd.setOnClickListener {
            onAddFriendClick(user)
        }
    }

    override fun getItemCount() = users.size

    fun updateUsers(newUsers: List<UserResponse>) {
        users = newUsers
        notifyDataSetChanged()
    }
    
    fun getCurrentUsers(): List<UserResponse> = users
}
