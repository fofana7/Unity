package com.example.unity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class NotificationAdapter(
    private var notifications: List<NotificationItem>,
    private val onItemClick: (NotificationItem) -> Unit
) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    fun updateData(newNotifs: List<NotificationItem>) {
        notifications = newNotifs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notif = notifications[position]
        holder.bind(notif)
        holder.itemView.setOnClickListener { onItemClick(notif) }
    }

    override fun getItemCount(): Int = notifications.size

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvInitials: TextView = view.findViewById(R.id.tvInitials)
        private val tvContent: TextView = view.findViewById(R.id.tvContent)
        private val tvPreview: TextView = view.findViewById(R.id.tvPreview)
        private val tvDate: TextView = view.findViewById(R.id.tvDate)
        private val ivTypeIcon: ImageView = view.findViewById(R.id.ivTypeIcon)

        fun bind(notif: NotificationItem) {
            val name = notif.displayName()
            tvInitials.text = name.take(1).uppercase()

            when (notif.type) {
                "like_post" -> {
                    tvContent.text = "$name a aimé votre post."
                    ivTypeIcon.setImageResource(android.R.drawable.btn_star_big_on) // Star/Like icon
                }
                "comment_reply" -> {
                    tvContent.text = "$name a commenté votre post."
                    ivTypeIcon.setImageResource(android.R.drawable.sym_action_chat)
                }
                "friend_request" -> {
                    tvContent.text = "$name vous a envoyé une demande d'ami."
                    ivTypeIcon.setImageResource(android.R.drawable.ic_menu_add)
                }
                "friend_accepted" -> {
                    tvContent.text = "$name a accepté votre demande d'ami."
                    ivTypeIcon.setImageResource(android.R.drawable.checkbox_on_background)
                }
                "friend_rejected" -> {
                    tvContent.text = "$name a refusé votre demande d'ami."
                    ivTypeIcon.setImageResource(android.R.drawable.ic_delete)
                }
                "photo_post" -> {
                    tvContent.text = "$name a publié une nouvelle photo."
                    ivTypeIcon.setImageResource(android.R.drawable.ic_menu_camera)
                }
                "announcement" -> {
                    tvContent.text = "Nouvelle annonce dans ${notif.className ?: "votre classe"}."
                    ivTypeIcon.setImageResource(android.R.drawable.ic_dialog_info)
                }
                "event_created", "evenement" -> {
                    tvContent.text = notif.preview ?: "Nouvel évènement pour votre classe !"
                    ivTypeIcon.setImageResource(android.R.drawable.ic_menu_today)
                }
                else -> {
                    tvContent.text = "Nouvelle notification de $name"
                    ivTypeIcon.setImageResource(android.R.drawable.ic_popup_reminder)
                }
            }

            if (!notif.preview.isNullOrEmpty()) {
                tvPreview.visibility = View.VISIBLE
                tvPreview.text = "\"${notif.preview}\""
            } else {
                tvPreview.visibility = View.GONE
            }

            tvDate.text = formatTime(notif.createdAt)
        }

        private fun formatTime(dateStr: String?): String {
            if (dateStr.isNullOrEmpty()) return ""
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(dateStr) ?: return ""
                val diff = (System.currentTimeMillis() - date.time) / 1000
                when {
                    diff < 60 -> "À l'instant"
                    diff < 3600 -> "Il y a ${diff / 60} min"
                    diff < 86400 -> "Il y a ${diff / 3600} h"
                    else -> "Il y a ${diff / 86400} j"
                }
            } catch (e: Exception) { "" }
        }
    }
}
