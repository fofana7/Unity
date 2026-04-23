package com.example.unity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GroupMemberAdapter(
    private val members: List<UserResponse>,
    private val currentUserId: Int
) : RecyclerView.Adapter<GroupMemberAdapter.MemberViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = members[position]
        holder.bind(member, member.id == currentUserId)
    }

    override fun getItemCount(): Int = members.size

    class MemberViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.tvMemberName)
        private val tvRole: TextView = view.findViewById(R.id.tvMemberRole)
        private val tvInitials: TextView = view.findViewById(R.id.tvMemberInitials)

        fun bind(user: UserResponse, isMe: Boolean) {
            val fullName = user.displayName()
            tvName.text = if (isMe) "$fullName (vous)" else fullName
            tvRole.text = user.role?.capitalize() ?: "Étudiant"
            tvInitials.text = fullName.take(1).uppercase()
        }
    }
}
