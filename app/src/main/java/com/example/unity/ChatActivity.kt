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
        
        otherUserId = intent.getIntExtra("OTHER_USER_ID", -1)
        val otherUserName = intent.getStringExtra("OTHER_USER_NAME") ?: "Discussion"

        findViewById<TextView>(R.id.tvChatName).text = otherUserName
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val rvMessages = findViewById<RecyclerView>(R.id.rvMessages)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)

        adapter = MessageAdapter(messagesList)
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvMessages.adapter = adapter

        startPollingMessages()

        btnSend.setOnClickListener {
            val content = etMessage.text.toString().trim()
            if (content.isNotEmpty()) {
                sendMessage(content)
                etMessage.text.clear()
            }
        }
    }

    private fun startPollingMessages() {
        val token = sessionManager.fetchAuthToken() ?: return
        val myId = sessionManager.fetchUserId()
        
        lifecycleScope.launch {
            while (isPolling) {
                try {
                    val response = RetrofitClient.instance.getPrivateMessages("Bearer $token", otherUserId)

                    if (response.isSuccessful && response.body() != null) {
                        val serverMessages = response.body()!!
                        
                        if (serverMessages.size != messagesList.size) {
                            messagesList.clear()
                            serverMessages.forEach { msg ->
                                msg.isMe = msg.senderId == myId
                                messagesList.add(msg)
                            }
                            adapter.notifyDataSetChanged()
                            if (messagesList.isNotEmpty()) {
                                findViewById<RecyclerView>(R.id.rvMessages).scrollToPosition(messagesList.size - 1)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CHAT", "Erreur polling", e)
                }
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    private fun sendMessage(content: String) {
        val token = sessionManager.fetchAuthToken() ?: return
        val myId = sessionManager.fetchUserId()
        
        if (myId == -1) {
            Toast.makeText(this, "Erreur d'identification. Reconnectez-vous.", Toast.LENGTH_SHORT).show()
            return
        }

        // Utilisation du modèle de requête simplifié pour le serveur
        val sendRequest = SendMessageRequest(
            receiverId = otherUserId,
            content = content
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.sendPrivateMessage("Bearer $token", sendRequest)
                if (!response.isSuccessful) {
                    val errorMsg = response.errorBody()?.string()
                    Log.e("CHAT_SEND", "Erreur ${response.code()}: $errorMsg")
                    Toast.makeText(this@ChatActivity, "Erreur d'envoi", Toast.LENGTH_SHORT).show()
                }
                // Le polling rafraîchira la liste automatiquement
            } catch (e: Exception) {
                Log.e("CHAT_SEND", "Exception", e)
                Toast.makeText(this@ChatActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
