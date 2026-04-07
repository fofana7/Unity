package com.example.unity

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var adapter: MessageAdapter
    private val messagesList = mutableListOf<Message>()
    private lateinit var sessionManager: SessionManager
    private var otherUserId: Int = -1
    private var groupId: Int = -1
    private var isGroup: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)

        sessionManager = SessionManager(this)
        
        // Récupérer les infos de la conversation
        otherUserId = intent.getIntExtra("OTHER_USER_ID", -1)
        groupId = intent.getIntExtra("GROUP_ID", -1)
        val chatName = intent.getStringExtra("CHAT_NAME") ?: "Chat"
        isGroup = groupId != -1

        findViewById<TextView>(R.id.tvChatName).text = chatName
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val rvMessages = findViewById<RecyclerView>(R.id.rvMessages)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)

        adapter = MessageAdapter(messagesList)
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true // Pour que les messages commencent par le bas
        }
        rvMessages.adapter = adapter

        // Charger les messages
        loadMessages()

        btnSend.setOnClickListener {
            val content = etMessage.text.toString().trim()
            if (content.isNotEmpty()) {
                sendMessage(content)
                etMessage.text.clear()
            }
        }
    }

    private fun loadMessages() {
        val token = sessionManager.fetchAuthToken() ?: return
        val myId = sessionManager.fetchUserId()
        
        lifecycleScope.launch {
            try {
                val response = if (isGroup) {
                    RetrofitClient.instance.getGroupMessages("Bearer $token", groupId)
                } else {
                    RetrofitClient.instance.getPrivateMessages("Bearer $token", otherUserId)
                }

                if (response.isSuccessful && response.body() != null) {
                    messagesList.clear()
                    response.body()!!.forEach { msg ->
                        msg.isMe = msg.senderId == myId
                        messagesList.add(msg)
                    }
                    adapter.notifyDataSetChanged()
                    findViewById<RecyclerView>(R.id.rvMessages).scrollToPosition(messagesList.size - 1)
                } else {
                    addDummyMessages()
                }
            } catch (e: Exception) {
                Log.e("CHAT", "Erreur réseau", e)
                addDummyMessages()
            }
        }
    }

    private fun sendMessage(content: String) {
        val token = sessionManager.fetchAuthToken() ?: return
        val myId = sessionManager.fetchUserId()
        
        val newMessage = Message(
            senderId = myId,
            receiverId = if (!isGroup) otherUserId else null,
            groupId = if (isGroup) groupId else null,
            content = content,
            isMe = true,
            timestamp = "Maintenant"
        )

        // Ajout local immédiat pour la fluidité
        messagesList.add(newMessage)
        adapter.notifyItemInserted(messagesList.size - 1)
        findViewById<RecyclerView>(R.id.rvMessages).scrollToPosition(messagesList.size - 1)

        lifecycleScope.launch {
            try {
                if (isGroup) {
                    RetrofitClient.instance.sendGroupMessage("Bearer $token", newMessage)
                } else {
                    RetrofitClient.instance.sendPrivateMessage("Bearer $token", newMessage)
                }
            } catch (e: Exception) {
                Log.e("CHAT", "Erreur envoi", e)
            }
        }
    }

    private fun addDummyMessages() {
        messagesList.add(Message(senderId = 0, content = "Salut !", timestamp = "10:00", isMe = false))
        messagesList.add(Message(senderId = 1, content = "Coucou, comment ça va ?", timestamp = "10:01", isMe = true))
        adapter.notifyDataSetChanged()
    }
}
