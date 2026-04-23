package com.example.unity

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.unity.databinding.ActivityCreateEventBinding
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class CreateEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateEventBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupSpinners()
        setupDatePicker()

        binding.btnBack.setOnClickListener { finish() }
        binding.btnCreateEvent.setOnClickListener { attemptCreateEvent() }
    }

    private fun setupSpinners() {
        val eventTypes = arrayOf("Contrôle", "Devoir à rendre", "Examen", "Cours annulé", "Autre")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, eventTypes)
        binding.actEventType.setAdapter(typeAdapter)

        val classes = arrayOf("A1MSI", "A2MSI", "A3MSI")
        val classAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, classes)
        binding.actEventClasse.setAdapter(classAdapter)
    }

    private fun setupDatePicker() {
        binding.etEventDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    binding.etEventDate.setText(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }
    }

    private fun attemptCreateEvent() {
        val title = binding.etEventTitle.text.toString().trim()
        val description = binding.etEventDescription.text.toString().trim()
        val type = binding.actEventType.text.toString().trim()
        val classe = binding.actEventClasse.text.toString().trim()
        val date = binding.etEventDate.text.toString()

        if (title.isEmpty() || type.isEmpty() || classe.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs obligatoires", Toast.LENGTH_SHORT).show()
            return
        }

        val token = sessionManager.fetchAuthToken() ?: return
        val request = CreateEventRequest(
            title = title,
            description = if (description.isEmpty()) null else description,
            type = type,
            date = date,
            classe = classe
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.createEvent("Bearer $token", request)
                if (response.isSuccessful) {
                    Toast.makeText(this@CreateEventActivity, "Évènement créé !", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("CREATE_EVENT", "Error: ${response.code()} - $errorBody")
                    Toast.makeText(this@CreateEventActivity, "Erreur ${response.code()}: $errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("CREATE_EVENT", "Exception", e)
                Toast.makeText(this@CreateEventActivity, "Erreur réseau: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
