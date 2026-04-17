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
        selectedIds.clear()
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

            tvName.text = fullName
            tvInitials.text = fullName.take(1).uppercase()
            
            // Détacher le listener pour éviter des boucles lors du recyclage
            cbSelect.setOnCheckedChangeListener(null)
            cbSelect.isChecked = isSelected

            cbSelect.setOnCheckedChangeListener { _, isChecked ->
                user.id?.let { id ->
                    if (isChecked) selectedIds.add(id) else selectedIds.remove(id)
                    onSelectionChange(selectedIds.toList())
                }
            }

            itemView.setOnClickListener {
                cbSelect.isChecked = !cbSelect.isChecked
            }
        }
    }
}
