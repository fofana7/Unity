package com.example.unity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class EventListAdapter(private var events: List<EventResponse>) : RecyclerView.Adapter<EventListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]
        holder.tvTitle.text = event.title
        holder.tvType.text = event.type.uppercase()
        holder.tvDescription.text = event.description ?: "Aucune description"
        holder.tvDate.text = event.date.split("T")[0]
        
        val teacherName = event.teacherName ?: "Enseignant"
        holder.tvTeacher.text = "Par $teacherName"
        holder.tvTeacherInitial.text = teacherName.take(1).uppercase()

        // Couleurs dynamiques selon le type
        val context = holder.itemView.context
        when (event.type.lowercase()) {
            "examen" -> {
                holder.tvType.setBackgroundResource(R.drawable.rounded_accent_purple)
                holder.viewAccent.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_purple))
                holder.tvTeacherInitial.setBackgroundResource(R.drawable.rounded_accent_purple)
                holder.tvTitle.setTextColor(ContextCompat.getColor(context, android.R.color.holo_purple))
            }
            "contrôle", "controle" -> {
                holder.tvType.setBackgroundResource(R.drawable.rounded_accent_blue)
                holder.viewAccent.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark))
                holder.tvTeacherInitial.setBackgroundResource(R.drawable.rounded_accent_blue)
                holder.tvTitle.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark))
            }
            "cours annulé", "annulé" -> {
                holder.tvType.setBackgroundResource(R.drawable.sidebar_card_bg)
                holder.viewAccent.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                holder.tvTeacherInitial.setBackgroundResource(R.drawable.sidebar_card_bg)
                holder.tvTitle.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            }
            else -> {
                holder.tvType.setBackgroundResource(R.drawable.rounded_accent_blue)
                holder.viewAccent.setBackgroundColor(ContextCompat.getColor(context, R.color.unity_accent))
                holder.tvTeacherInitial.setBackgroundResource(R.drawable.rounded_accent_blue)
                holder.tvTitle.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            }
        }
    }

    override fun getItemCount(): Int = events.size

    fun updateData(newEvents: List<EventResponse>) {
        this.events = newEvents
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvType: TextView = view.findViewById(R.id.tvEventType)
        val tvTitle: TextView = view.findViewById(R.id.tvEventTitle)
        val tvDate: TextView = view.findViewById(R.id.tvEventDate)
        val tvDescription: TextView = view.findViewById(R.id.tvEventDescription)
        val tvTeacher: TextView = view.findViewById(R.id.tvTeacherName)
        val tvTeacherInitial: TextView = view.findViewById(R.id.tvTeacherInitial)
        val viewAccent: View = view.findViewById(R.id.viewTypeAccent)
    }
}
