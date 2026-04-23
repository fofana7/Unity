package com.example.unity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TeacherEventAdapter(
    private var events: List<EventResponse>,
    private val onEditClick: (EventResponse) -> Unit,
    private val onDeleteClick: (EventResponse) -> Unit
) : RecyclerView.Adapter<TeacherEventAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_teacher_event, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]
        val context = holder.itemView.context
        
        holder.tvTitle.text = event.title
        holder.tvType.text = event.type.uppercase()
        holder.tvDate.text = event.date.split("T")[0]
        holder.tvClasse.text = event.classe

        // Couleurs dynamiques
        when (event.type.lowercase()) {
            "examen" -> {
                holder.tvType.setBackgroundResource(R.drawable.rounded_accent_purple)
                holder.viewAccent.setBackgroundColor(context.getColor(android.R.color.holo_purple))
                holder.tvTitle.setTextColor(context.getColor(android.R.color.holo_purple))
            }
            "contrôle", "controle" -> {
                holder.tvType.setBackgroundResource(R.drawable.rounded_accent_blue)
                holder.viewAccent.setBackgroundColor(context.getColor(android.R.color.holo_orange_dark))
                holder.tvTitle.setTextColor(context.getColor(android.R.color.holo_orange_dark))
            }
            "cours annulé", "annulé" -> {
                holder.tvType.setBackgroundResource(R.drawable.sidebar_card_bg)
                holder.viewAccent.setBackgroundColor(context.getColor(android.R.color.holo_red_dark))
                holder.tvTitle.setTextColor(context.getColor(android.R.color.holo_red_dark))
            }
            else -> {
                holder.tvType.setBackgroundResource(R.drawable.rounded_accent_blue)
                holder.viewAccent.setBackgroundColor(context.getColor(R.color.unity_accent))
                holder.tvTitle.setTextColor(context.getColor(android.R.color.white))
            }
        }

        holder.btnEdit.setOnClickListener { onEditClick(event) }
        holder.btnDelete.setOnClickListener { onDeleteClick(event) }
    }

    override fun getItemCount(): Int = events.size

    fun updateData(newEvents: List<EventResponse>) {
        this.events = newEvents
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val viewAccent: View = view.findViewById(R.id.viewAccent)
        val tvType: TextView = view.findViewById(R.id.tvEventType)
        val tvClasse: TextView = view.findViewById(R.id.tvEventClasse)
        val tvTitle: TextView = view.findViewById(R.id.tvEventTitle)
        val tvDate: TextView = view.findViewById(R.id.tvEventDate)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }
}
