package com.example.unity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MessageAdapter(private val messages: List<Message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isMe) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_right, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_left, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        
        val tvMsg: TextView
        val tvTime: TextView
        val ivImg: ImageView?
        val layoutPdf: View?
        val tvFileName: TextView?
        val tvSender: TextView?

        if (holder is SentMessageViewHolder) {
            tvMsg = holder.tvMessage
            tvTime = holder.tvTime
            ivImg = holder.ivImage
            layoutPdf = holder.layoutPdf
            tvFileName = holder.tvFileName
            tvSender = null
        } else {
            val h = holder as ReceivedMessageViewHolder
            tvMsg = h.tvMessage
            tvTime = h.tvTime
            ivImg = h.ivImage
            layoutPdf = h.layoutPdf
            tvFileName = h.tvFileName
            tvSender = h.tvSenderName
        }

        // --- BINDING ---
        tvMsg.text = message.content
        tvMsg.visibility = if (message.content.isEmpty()) View.GONE else View.VISIBLE
        tvTime.text = formatTimestamp(message.timestamp)

        if (tvSender != null) {
            if (message.senderName != null) {
                tvSender.text = message.senderName
                tvSender.visibility = View.VISIBLE
            } else {
                tvSender.visibility = View.GONE
            }
        }

        // --- ATTACHMENTS ---
        ivImg?.visibility = View.GONE
        layoutPdf?.visibility = View.GONE

        if (!message.fileUrl.isNullOrEmpty()) {
            if (message.fileType == "image") {
                ivImg?.let {
                    it.visibility = View.VISIBLE
                    Glide.with(it.context).load(message.fileUrl).into(it)
                    it.setOnClickListener { view ->
                        // Optionnel: Ouvrir l'image en plein écran
                    }
                }
            } else if (message.fileType == "pdf") {
                layoutPdf?.let {
                    it.visibility = View.VISIBLE
                    tvFileName?.text = message.fileUrl.substringAfterLast("/")
                    it.setOnClickListener { view ->
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                        intent.data = android.net.Uri.parse(message.fileUrl)
                        view.context.startActivity(intent)
                    }
                }
            }
        }
    }

    private fun formatTimestamp(ts: String?): String {
        if (ts == null) return ""
        return try {
            val parts = ts.split("T")
            if (parts.size > 1) {
                parts[1].substring(0, 5)
            } else ts
        } catch (e: Exception) { ts }
    }

    override fun getItemCount() = messages.size

    class SentMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val ivImage: ImageView = view.findViewById(R.id.ivImage)
        val layoutPdf: View = view.findViewById(R.id.layoutPdf)
        val tvFileName: TextView = view.findViewById(R.id.tvFileName)
    }

    class ReceivedMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSenderName: TextView = view.findViewById(R.id.tvSenderName)
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val ivImage: ImageView = view.findViewById(R.id.ivImage)
        val layoutPdf: View = view.findViewById(R.id.layoutPdf)
        val tvFileName: TextView = view.findViewById(R.id.tvFileName)
    }
}
