package com.example.unity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class FriendAdapter(
    private var friends: List<UserResponse>,
    private val onActionClick: (UserResponse) -> Unit
) : RecyclerView.Adapter<FriendAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val tvUserHandle: TextView = view.findViewById(R.id.tvUserHandle)
        val tvInitials: TextView = view.findViewById(R.id.tvInitials)
        val btnAction: MaterialButton = view.findViewById(R.id.btnAction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val friend = friends[position]
        val fullName = "${friend.firstName ?: ""} ${friend.lastName ?: ""}".trim()
        holder.tvUserName.text = if (fullName.isNotEmpty()) fullName else friend.username
        holder.tvUserHandle.text = "@${friend.username}"
        
        val initials = (friend.firstName?.take(1) ?: friend.username?.take(1) ?: "U").uppercase()
        holder.tvInitials.text = initials

        // On pourra adapter le texte du bouton selon l'onglet
        // holder.btnAction.text = "Ajouter" / "Supprimer" / "Accepter"
    }

    override fun getItemCount() = friends.size

    fun updateData(newList: List<UserResponse>) {
        friends = newList
        notifyDataSetChanged()
    }
}
