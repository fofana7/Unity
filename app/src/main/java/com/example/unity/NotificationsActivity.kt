package com.example.unity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.launch

class NotificationsActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: NotificationAdapter
    private val notificationList = mutableListOf<NotificationItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifications)

        sessionManager = SessionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val rv = findViewById<RecyclerView>(R.id.rvNotifications)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = NotificationAdapter(notificationList)
        rv.adapter = adapter

        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            loadNotifications(swipeRefresh)
        }

        loadNotifications()
    }

    private fun loadNotifications(swipeRefresh: SwipeRefreshLayout? = null) {
        val token = sessionManager.fetchAuthToken()
        if (token == null) {
            swipeRefresh?.isRefreshing = false
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getNotifications("Bearer $token")
                val tvEmpty = findViewById<TextView>(R.id.tvEmpty)

                if (response.isSuccessful && response.body() != null) {
                    notificationList.clear()
                    notificationList.addAll(response.body()!!)
                    adapter.notifyDataSetChanged()

                    if (notificationList.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                    } else {
                        tvEmpty.visibility = View.GONE
                    }
                } else {
                    Log.e("NOTIFICATIONS", "Erreur serveur : ${response.code()}")
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "Erreur de chargement des notifications"
                }
            } catch (e: Exception) {
                Log.e("NOTIFICATIONS", "Erreur réseau", e)
                findViewById<TextView>(R.id.tvEmpty).visibility = View.VISIBLE
                findViewById<TextView>(R.id.tvEmpty).text = "Veuillez vérifier votre connexion"
            } finally {
                swipeRefresh?.isRefreshing = false
            }
        }
    }
}
