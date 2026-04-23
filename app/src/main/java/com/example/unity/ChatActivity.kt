package com.example.unity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

class ChatActivity : AppCompatActivity() {

    private lateinit var adapter: MessageAdapter
    private val messagesList = mutableListOf<Message>()
    private lateinit var sessionManager: SessionManager

    private var otherUserId: Int = -1
    private var groupId: Int = -1
    private var isPolling = true
    private var groupMembersMap = mutableMapOf<Int, UserResponse>()

    private val addMembersLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            fetchInitialData() // Recharger les membres
        }
    }

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadAndSendMessage(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        isPolling = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sessionManager = SessionManager(this)

        otherUserId = intent.getIntExtra("OTHER_USER_ID", -1)
        groupId     = intent.getIntExtra("GROUP_ID", -1)
        val chatName = intent.getStringExtra("CHAT_NAME") ?: "Discussion"

        findViewById<TextView>(R.id.tvChatName).text = chatName
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val rvMessages = findViewById<RecyclerView>(R.id.rvMessages)
        val etMessage  = findViewById<EditText>(R.id.etMessage)
        val btnSend    = findViewById<ImageButton>(R.id.btnSend)
        val btnInfo    = findViewById<ImageButton>(R.id.btnInfo)
        val llHeader   = findViewById<View>(R.id.llChatHeader)

        val myId = sessionManager.fetchUserId()
        adapter = MessageAdapter(messagesList)
        rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvMessages.adapter = adapter

        if (groupId != -1) {
            btnInfo.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvChatStatus).visibility = View.VISIBLE
            fetchInitialData()

            val infoClick = View.OnClickListener { showGroupMembersDialog() }
            btnInfo.setOnClickListener(infoClick)
            llHeader.setOnClickListener(infoClick)
        } else {
            btnInfo.visibility = View.GONE
        }

        startPolling(myId, rvMessages, chatName)

        btnSend.setOnClickListener {
            val content = etMessage.text.toString().trim()
            if (content.isNotEmpty()) {
                sendMessage(content, myId, rvMessages)
                etMessage.text.clear()
            }
        }

        findViewById<ImageButton>(R.id.btnAttach).setOnClickListener {
            pickFileLauncher.launch("*/*") // Accepte tout, on filtrera ou le système gérera
        }
    }

    private fun fetchInitialData() {
        val token = sessionManager.fetchAuthToken() ?: return
        val myId  = sessionManager.fetchUserId()

        lifecycleScope.launch {
            try {
                val groupDeferred = async { RetrofitClient.instance.getGroupDetails("Bearer $token", groupId) }
                val groupResp = groupDeferred.await()

                if (groupResp.isSuccessful) {
                    val members = groupResp.body()?.members ?: emptyList()
                    groupMembersMap.clear()
                    members.forEach { user ->
                        val id = user.id ?: return@forEach
                        groupMembersMap[id] = user
                    }
                    if (!groupMembersMap.containsKey(myId)) {
                        // On peut ajouter le current user si non présent
                        groupMembersMap[myId] = UserResponse(id = myId, firstName = "Moi")
                    }
                } else {
                    groupMembersMap.clear()
                    if (myId != null) {
                        groupMembersMap[myId] = UserResponse(id = myId, firstName = "Moi")
                    }
                }
                updateMemberCountUI()
            } catch (e: Exception) {
                Log.e("CHAT", "Erreur fetchInitialData", e)
            }
        }
    }

    private fun updateMemberCountUI() {
        val tvStatus = findViewById<TextView>(R.id.tvChatStatus)
        val count = groupMembersMap.size
        tvStatus.text = if (count > 0) "$count membre${if (count > 1) "s" else ""}" else "Chargement..."
    }

    private fun showGroupMembersDialog() {
        val membersList = groupMembersMap.values.toList()
            .sortedBy { it.id != sessionManager.fetchUserId() }

        val bottomSheet = GroupMembersBottomSheet(
            members = membersList,
            currentUserId = sessionManager.fetchUserId(),
            onAddMember = { openAddMembers() },
            onLeaveGroup = { confirmLeaveGroup() }
        )
        bottomSheet.show(supportFragmentManager, "GroupMembersBottomSheet")
    }

    private fun openAddMembers() {
        val intent = Intent(this, AddGroupMembersActivity::class.java)
        intent.putExtra("GROUP_ID", groupId)
        addMembersLauncher.launch(intent)
    }

    private fun confirmLeaveGroup() {
        AlertDialog.Builder(this)
            .setTitle("Quitter le groupe")
            .setMessage("Êtes-vous sûr de vouloir quitter ce groupe ?")
            .setPositiveButton("Oui, quitter") { _, _ ->
                leaveGroup()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun leaveGroup() {
        val token = sessionManager.fetchAuthToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.leaveGroup("Bearer $token", groupId)
                if (response.isSuccessful) {
                    Toast.makeText(this@ChatActivity, "Vous avez quitté le groupe", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@ChatActivity, "Erreur pour quitter le groupe", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startPolling(myId: Int, rv: RecyclerView, chatName: String) {
        val token = sessionManager.fetchAuthToken() ?: return
        lifecycleScope.launch {
            while (isPolling) {
                try {
                    if (groupId != -1) {
                        val resp = RetrofitClient.instance.getGroupMessages("Bearer $token", groupId)
                        if (resp.isSuccessful) {
                            val serverMessages = resp.body()?.map { gm ->
                                if (!groupMembersMap.containsKey(gm.senderId)) {
                                    groupMembersMap[gm.senderId] = UserResponse(id = gm.senderId, username = gm.username, firstName = gm.firstName, lastName = gm.lastName)
                                    updateMemberCountUI()
                                }
                                val senderName = groupMembersMap[gm.senderId]?.displayName() ?: gm.displayName()
                                Message(
                                    id = gm.id,
                                    senderId = gm.senderId,
                                    content = gm.content,
                                    timestamp = gm.createdAt,
                                    senderName = if (gm.senderId == myId) "$senderName (vous)" else senderName,
                                    fileUrl = gm.fileUrl,
                                    fileType = gm.fileType
                                ).also { it.isMe = gm.senderId == myId }
                            } ?: emptyList()

                            if (serverMessages.size != messagesList.size) {
                                messagesList.clear()
                                messagesList.addAll(serverMessages)
                                adapter.notifyDataSetChanged()
                                rv.scrollToPosition(messagesList.size - 1)
                            }
                        }
                    } else {
                        val resp = RetrofitClient.instance.getPrivateMessages("Bearer $token", otherUserId)
                        if (resp.isSuccessful) {
                            val serverMessages = resp.body()?.map { m ->
                                m.copy(
                                    isMe = m.senderId == myId,
                                    senderName = if (m.senderId == myId) "Moi" else chatName
                                )
                            } ?: emptyList()

                            if (serverMessages.size != messagesList.size) {
                                messagesList.clear()
                                messagesList.addAll(serverMessages)
                                adapter.notifyDataSetChanged()
                                rv.scrollToPosition(messagesList.size - 1)
                            }
                        }
                    }
                } catch (e: Exception) { Log.e("CHAT", "Polling error", e) }
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    private fun sendMessage(content: String, myId: Int, rv: RecyclerView, fileUrl: String? = null, fileType: String? = null) {
        val token = sessionManager.fetchAuthToken() ?: return
        val optimistic = Message(-1, myId, content = content, fileUrl = fileUrl, fileType = fileType).also { it.isMe = true }
        messagesList.add(optimistic)
        adapter.notifyItemInserted(messagesList.size - 1)
        rv.scrollToPosition(messagesList.size - 1)

        lifecycleScope.launch {
            try {
                val response = if (groupId != -1) {
                    RetrofitClient.instance.sendGroupMessage(
                        "Bearer $token",
                        SendMessageRequest(groupId = groupId, content = content, fileUrl = fileUrl, fileType = fileType)
                    )
                } else {
                    RetrofitClient.instance.sendPrivateMessage(
                        "Bearer $token",
                        SendMessageRequest(receiverId = otherUserId, content = content, fileUrl = fileUrl, fileType = fileType)
                    )
                }

                if (!response.isSuccessful) {
                    messagesList.remove(optimistic)
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this@ChatActivity, "Échec de l'envoi", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                messagesList.remove(optimistic)
                adapter.notifyDataSetChanged()
                Toast.makeText(this@ChatActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadAndSendMessage(uri: android.net.Uri) {
        val token = sessionManager.fetchAuthToken() ?: return
        val myId = sessionManager.fetchUserId()
        val rvMessages = findViewById<RecyclerView>(R.id.rvMessages)
        
        lifecycleScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: return@launch
                inputStream.close()

                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                val fileName = "upload_${System.currentTimeMillis()}"
                
                val requestFile = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", fileName, requestFile)

                val response = RetrofitClient.instance.uploadChatFile("Bearer $token", body)
                if (response.isSuccessful && response.body() != null) {
                    val uploadData = response.body()!!
                    sendMessage("", myId, rvMessages, uploadData.fileUrl, uploadData.fileType)
                } else {
                    Toast.makeText(this@ChatActivity, "Erreur upload", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CHAT", "Upload failed", e)
                Toast.makeText(this@ChatActivity, "Erreur lors de l'upload", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
