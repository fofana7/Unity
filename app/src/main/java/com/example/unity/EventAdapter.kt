package com.example.unity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EventAdapter(private var events: List<EventResponse>) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event_card, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size

    fun updateData(newEvents: List<EventResponse>) {
        this.events = newEvents
        notifyDataSetChanged()
    }

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvType: TextView = view.findViewById(R.id.tvEventType)
        private val tvTitle: TextView = view.findViewById(R.id.tvEventTitle)
        private val tvDate: TextView = view.findViewById(R.id.tvEventDate)

        fun bind(event: EventResponse) {
            tvTitle.text = event.title
            tvType.text = event.type
            
            // Format simple de la date (yyyy-mm-dd -> dd MMM)
            val dateStr = event.date.split("T")[0] // Au cas où c'est un ISO string
            tvDate.text = dateStr 
            
            // On peut changer la couleur du badge selon le type
            when (event.type.lowercase()) {
                "examen", "contrôle" -> tvType.setBackgroundResource(R.drawable.rounded_accent_purple)
                "cours annulé" -> tvType.setBackgroundResource(R.drawable.sidebar_card_bg)
                else -> tvType.setBackgroundResource(R.drawable.rounded_accent_blue)
            }
        }
    }
}
