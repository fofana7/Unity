package com.example.unity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.launch

class EventsActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: EventListAdapter
    private var eventsList = mutableListOf<EventResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_events)

        sessionManager = SessionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvEvents)
        adapter = EventListAdapter(eventsList)
        rv.adapter = adapter

        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener { loadEvents() }

        loadEvents()
    }

    private fun loadEvents() {
        val token = sessionManager.fetchAuthToken() ?: return
        val classe = sessionManager.fetchUserClasse() ?: "" 
        
        // On récupère aussi la classe depuis la session si possible, sinon on attend que l'app soit prête
        if (classe.isEmpty()) {
             // On pourrait appeler getMe() ici si besoin
        }

        lifecycleScope.launch {
            try {
                // Pour simplifier on récupère le profil d'abord pour avoir la classe à jour
                val meResp = RetrofitClient.instance.getMe("Bearer $token")
                val currentClasse = meResp.body()?.user?.classe ?: ""
                
                if (currentClasse.isNotEmpty()) {
                    Log.d("EVENTS_PAGE", "Fetching for class: $currentClasse")
                    val response = RetrofitClient.instance.getEventsByClasse("Bearer $token", currentClasse)
                    if (response.isSuccessful && response.body() != null) {
                        eventsList.clear()
                        eventsList.addAll(response.body()!!)
                        adapter.notifyDataSetChanged()
                        
                        findViewById<View>(R.id.tvEmpty).visibility = 
                            if (eventsList.isEmpty()) View.VISIBLE else View.GONE
                        
                        if (eventsList.isEmpty()) {
                            Toast.makeText(this@EventsActivity, "Aucun évènement trouvé pour $currentClasse", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@EventsActivity, "Erreur serveur : ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@EventsActivity, "Classe non définie pour votre profil", Toast.LENGTH_LONG).show()
                    findViewById<View>(R.id.tvEmpty).visibility = View.VISIBLE
                    // Si c'est un LinearLayout, on ne peut pas mettre de texte directement, 
                    // mais l'icône et le texte par défaut du layout feront l'affaire.
                }
            } catch (e: Exception) {
                findViewById<View>(R.id.tvEmpty).visibility = View.VISIBLE
            } finally {
                findViewById<SwipeRefreshLayout>(R.id.swipeRefresh).isRefreshing = false
            }
        }
    }
}
