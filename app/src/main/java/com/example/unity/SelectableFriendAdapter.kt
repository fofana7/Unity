package com.example.unity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SelectableFriendAdapter(
    private var friends: List<UserResponse>,
    private val onSelectionChange: (List<Int>) -> Unit
) : RecyclerView.Adapter<SelectableFriendAdapter.FriendViewHolder>() {

    private val selectedIds = mutableSetOf<Int>()

    fun updateData(newFriends: List<UserResponse>) {
        friends = newFriends
        // On ne clear pas forcément les sélections ici si on veut les garder, 
        // mais pour une mise à jour globale c'est plus propre.
        notifyDataSetChanged()
    }

    fun setSelectedIds(ids: List<Int>) {
        selectedIds.clear()
        selectedIds.addAll(ids)
        notifyDataSetChanged()
    }

    fun getSelectedIds(): List<Int> = selectedIds.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_selectable_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friends[position]
        holder.bind(friend, selectedIds.contains(friend.id))
    }

    override fun getItemCount(): Int = friends.size

    inner class FriendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvInitials: TextView = view.findViewById(R.id.tvInitialsGroup)
        private val tvName: TextView = view.findViewById(R.id.tvFriendName)
        private val cbSelect: CheckBox = view.findViewById(R.id.cbSelect)

        fun bind(user: UserResponse, isSelected: Boolean) {
            val username = user.username ?: "Utilisateur"
            val firstName = user.firstName ?: ""
            val lastName = user.lastName ?: ""
            val fullName = "$firstName $lastName".trim().ifEmpty { username }
            
            // Affichage du nom avec la classe pour aider l'enseignant
            val displayText = if (!user.classe.isNullOrEmpty()) "$fullName (${user.classe})" else fullName

            tvName.text = displayText
            tvInitials.text = fullName.take(1).uppercase()
            
            // Désactiver le clic direct sur la checkbox pour que itemView gère tout
            cbSelect.isClickable = false
            cbSelect.isFocusable = false
            cbSelect.isChecked = isSelected

            itemView.setOnClickListener {
                user.id?.let { id ->
                    val currentlySelected = selectedIds.contains(id)
                    val newState = !currentlySelected
                    
                    if (newState) {
                        selectedIds.add(id)
                    } else {
                        selectedIds.remove(id)
                    }
                    
                    cbSelect.isChecked = newState
                    onSelectionChange(selectedIds.toList())
                }
            }
        }
    }
}
