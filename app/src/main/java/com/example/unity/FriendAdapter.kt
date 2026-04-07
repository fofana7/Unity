package com.example.unity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

enum class FriendActionType {
    FRIEND, SUGGESTION, REQUEST, SENT_REQUEST
}

data class FriendItem(
    val user: UserResponse,
    val actionType: FriendActionType
)

class FriendAdapter(
    private var items: List<FriendItem>,
    private val onActionClick: (UserResponse, FriendActionType) -> Unit
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
        val item = items[position]
        val friend = item.user
        val actionType = item.actionType
        
        val fullName = "${friend.firstName ?: ""} ${friend.lastName ?: ""}".trim()
        holder.tvUserName.text = if (fullName.isNotEmpty()) fullName else friend.username
        holder.tvUserHandle.text = "@${friend.username}"
        
        val initials = (friend.firstName?.take(1) ?: friend.username?.take(1) ?: "U").uppercase()
        holder.tvInitials.text = initials

        when (actionType) {
            FriendActionType.FRIEND -> {
                holder.btnAction.text = "Message"
                holder.btnAction.setIconResource(android.R.drawable.stat_notify_chat)
            }
            FriendActionType.SUGGESTION -> {
                holder.btnAction.text = "Ajouter"
                holder.btnAction.setIconResource(android.R.drawable.ic_input_add)
            }
            FriendActionType.REQUEST -> {
                holder.btnAction.text = "Accepter"
                holder.btnAction.setIconResource(android.R.drawable.checkbox_on_background)
            }
            FriendActionType.SENT_REQUEST -> {
                holder.btnAction.text = "Annuler"
                holder.btnAction.setIconResource(android.R.drawable.ic_menu_close_clear_cancel)
                holder.btnAction.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY)
            }
        }

        holder.btnAction.setOnClickListener {
            onActionClick(friend, actionType)
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<FriendItem>) {
        items = newItems
        notifyDataSetChanged()
    }
    
    // Reste pour la compatibilité si besoin ou à supprimer
    fun updateData(newList: List<UserResponse>, type: FriendActionType) {
        items = newList.map { FriendItem(it, type) }
        notifyDataSetChanged()
    }
}
