package com.example.unity

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
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
    private var isPolling = true

    override fun onDestroy() {
        super.onDestroy()
        isPolling = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)

        sessionManager = SessionManager(this)

        // Distinguer conversation privée VS groupe
        otherUserId = intent.getIntExtra("OTHER_USER_ID", -1)
        groupId = intent.getIntExtra("GROUP_ID", -1)
        val chatName = intent.getStringExtra("CHAT_NAME")
            ?: intent.getStringExtra("OTHER_USER_NAME")
            ?: "Discussion"

        findViewById<TextView>(R.id.tvChatName).text = chatName
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val rvMessages = findViewById<RecyclerView>(R.id.rvMessages)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)

        val myId = sessionManager.fetchUserId()
        adapter = MessageAdapter(messagesList)
        rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvMessages.adapter = adapter

        // Démarrer le polling selon le mode
        startPolling(myId, rvMessages)

        btnSend.setOnClickListener {
            val content = etMessage.text.toString().trim()
            if (content.isNotEmpty()) {
                sendMessage(content, myId, rvMessages)
                etMessage.text.clear()
            }
        }
    }

    // ─── Polling ──────────────────────────────────────────────────────────────
    private fun startPolling(myId: Int, rv: RecyclerView) {
        val token = sessionManager.fetchAuthToken() ?: return

        lifecycleScope.launch {
            while (isPolling) {
                try {
                    val serverMessages: List<Message> = if (groupId != -1) {
                        // Mode GROUPE
                        val resp = RetrofitClient.instance.getGroupMessages("Bearer $token", groupId)
                        if (resp.isSuccessful) resp.body()?.map { gm ->
                            Message(
                                id = gm.id,
                                senderId = gm.senderId,
                                content = gm.content,
                                timestamp = gm.createdAt
                            ).also { it.isMe = gm.senderId == myId }
                        } ?: emptyList()
                        else emptyList()
                    } else {
                        // Mode PRIVÉ
                        val resp = RetrofitClient.instance.getPrivateMessages("Bearer $token", otherUserId)
                        if (resp.isSuccessful) resp.body()?.map { pm ->
                            pm.isMe = pm.senderId == myId; pm
                        } ?: emptyList()
                        else emptyList()
                    }

                    if (serverMessages.size != messagesList.size) {
                        messagesList.clear()
                        messagesList.addAll(serverMessages)
                        adapter.notifyDataSetChanged()
                        if (messagesList.isNotEmpty()) {
                            rv.scrollToPosition(messagesList.size - 1)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CHAT", "Erreur polling", e)
                }
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    // ─── Envoi ────────────────────────────────────────────────────────────────
    private fun sendMessage(content: String, myId: Int, rv: RecyclerView) {
        val token = sessionManager.fetchAuthToken() ?: return

        // Optimistic UI : afficher immédiatement
        val optimistic = Message(
            id = -1,
            senderId = myId,
            content = content,
            timestamp = null
        ).also { it.isMe = true }
        messagesList.add(optimistic)
        adapter.notifyItemInserted(messagesList.size - 1)
        rv.scrollToPosition(messagesList.size - 1)

        lifecycleScope.launch {
            try {
                val success = if (groupId != -1) {
                    val resp = RetrofitClient.instance.sendGroupMessage(
                        "Bearer $token",
                        SendMessageRequest(groupId = groupId, content = content)
                    )
                    resp.isSuccessful
                } else {
                    val resp = RetrofitClient.instance.sendPrivateMessage(
                        "Bearer $token",
                        SendMessageRequest(receiverId = otherUserId, content = content)
                    )
                    resp.isSuccessful
                }

                if (!success) {
                    // Rollback
                    messagesList.remove(optimistic)
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this@ChatActivity, "Erreur d'envoi", Toast.LENGTH_SHORT).show()
                }
                // Le prochain cycle de polling synchronisera le vrai message
            } catch (e: Exception) {
                messagesList.remove(optimistic)
                adapter.notifyDataSetChanged()
                Toast.makeText(this@ChatActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
