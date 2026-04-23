package com.example.unity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ConversationAdapter(
    private var conversations: List<Conversation>,
    private val onConversationClick: (Conversation) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val tvLastMessage: TextView = view.findViewById(R.id.tvLastMessage)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val ivAvatar: View = view.findViewById(R.id.ivAvatar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conversation = conversations[position]
        holder.tvUserName.text = conversation.userName
        holder.tvLastMessage.text = conversation.lastMessage
        holder.tvTime.text = conversation.time
        
        // Couleur différente pour les groupes
        if (conversation.type == ConversationType.GROUP) {
            holder.ivAvatar.setBackgroundResource(R.drawable.rounded_accent_purple)
            holder.ivAvatar.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#0B84FF") // Bleu pour les groupes
            )
        } else {
            holder.ivAvatar.setBackgroundResource(R.drawable.rounded_accent_purple)
            holder.ivAvatar.backgroundTintList = null // Violet par défaut pour le privé
        }

        holder.itemView.setOnClickListener {
            onConversationClick(conversation)
        }
    }

    override fun getItemCount() = conversations.size

    fun updateData(newConversations: List<Conversation>) {
        this.conversations = newConversations
        notifyDataSetChanged()
    }
}
