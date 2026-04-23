package com.example.unity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.launch

class ManageEventsActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: TeacherEventAdapter
    private var eventsList = mutableListOf<EventResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manage_events)

        sessionManager = SessionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvEvents)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = TeacherEventAdapter(
            eventsList,
            onEditClick = { event -> openEditDialog(event) },
            onDeleteClick = { event -> confirmDelete(event) }
        )
        rv.adapter = adapter

        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener { loadTeacherEvents() }

        loadTeacherEvents()
    }

    private fun openEditDialog(event: EventResponse) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_event, null)
        val etTitle = view.findViewById<android.widget.EditText>(R.id.etEditTitle)
        val etType = view.findViewById<android.widget.EditText>(R.id.etEditType)
        val etClasse = view.findViewById<android.widget.EditText>(R.id.etEditClasse)
        val etDate = view.findViewById<android.widget.EditText>(R.id.etEditDate)
        val etDesc = view.findViewById<android.widget.EditText>(R.id.etEditDescription)

        etTitle.setText(event.title)
        etType.setText(event.type)
        etClasse.setText(event.classe)
        etDate.setText(event.date.split("T")[0])
        etDesc.setText(event.description ?: "")

        AlertDialog.Builder(this)
            .setTitle("Modifier l'évènement")
            .setView(view)
            .setPositiveButton("Enregistrer") { _, _ ->
                val newTitle = etTitle.text.toString().trim()
                val newType = etType.text.toString().trim()
                val newClasse = etClasse.text.toString().trim()
                val newDate = etDate.text.toString().trim()
                val newDesc = etDesc.text.toString().trim()
                
                if (newTitle.isNotEmpty() && newType.isNotEmpty() && newClasse.isNotEmpty() && newDate.isNotEmpty()) {
                    updateEvent(event.id, newTitle, newType, newClasse, newDesc, newDate)
                } else {
                    Toast.makeText(this, "Veuillez remplir tous les champs obligatoires", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun updateEvent(id: Int, title: String, type: String, classe: String, desc: String, date: String) {
        val token = sessionManager.fetchAuthToken() ?: return
        val request = CreateEventRequest(title, desc, type, date, classe)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.updateEvent("Bearer $token", id, request)
                if (response.isSuccessful) {
                    Toast.makeText(this@ManageEventsActivity, "Mis à jour !", Toast.LENGTH_SHORT).show()
                    loadTeacherEvents()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Code ${response.code()}"
                    Toast.makeText(this@ManageEventsActivity, "Erreur serveur: $errorMsg", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ManageEventsActivity, "Erreur réseau: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadTeacherEvents() {
        val token = sessionManager.fetchAuthToken() ?: return
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        val tvEmptyLayout = findViewById<View>(R.id.tvEmpty)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getMyEvents("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    eventsList.clear()
                    eventsList.addAll(response.body()!!)
                    adapter.notifyDataSetChanged()
                    
                    tvEmptyLayout.visibility = if (eventsList.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Erreur ${response.code()}"
                    Log.e("MANAGE_EVENTS", "Load error: $errorMsg")
                    tvEmptyLayout.visibility = View.VISIBLE
                    // On peut aussi mettre à jour un TextView à l'intérieur du layout si besoin
                }
            } catch (e: Exception) {
                Log.e("MANAGE_EVENTS", "Network exception", e)
                tvEmptyLayout.visibility = View.VISIBLE
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun confirmDelete(event: EventResponse) {
        AlertDialog.Builder(this)
            .setTitle("Supprimer l'évènement")
            .setMessage("Voulez-vous vraiment supprimer '${event.title}' ? Cette action est irréversible.")
            .setPositiveButton("Supprimer") { _, _ -> deleteEvent(event.id) }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun deleteEvent(eventId: Int) {
        val token = sessionManager.fetchAuthToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.deleteEvent("Bearer $token", eventId)
                if (response.isSuccessful) {
                    Toast.makeText(this@ManageEventsActivity, "Évènement supprimé", Toast.LENGTH_SHORT).show()
                    loadTeacherEvents()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Erreur ${response.code()}"
                    Toast.makeText(this@ManageEventsActivity, "Erreur suppression : $errorMsg", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ManageEventsActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
