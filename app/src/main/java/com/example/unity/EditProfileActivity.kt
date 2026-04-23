package com.example.unity

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class EditProfileActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private var currentUser: UserResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_profile)

        sessionManager = SessionManager(this)

        val etFirstName = findViewById<TextInputEditText>(R.id.etFirstName)
        val etLastName = findViewById<TextInputEditText>(R.id.etLastName)
        val etBio = findViewById<TextInputEditText>(R.id.etBio)
        val etClasse = findViewById<TextInputEditText>(R.id.etClasse)
        val btnSave = findViewById<MaterialButton>(R.id.btnSave)
        val tvInitials = findViewById<TextView>(R.id.tvEditInitials)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<android.view.View>(R.id.btnChangeAvatar).setOnClickListener {
            openImagePicker(REQUEST_CODE_AVATAR)
        }

        findViewById<android.view.View>(R.id.btnChangeBanner).setOnClickListener {
            openImagePicker(REQUEST_CODE_BANNER)
        }

        loadCurrentProfile(etFirstName, etLastName, etBio, etClasse, tvInitials)

        btnSave.setOnClickListener {
            val fName = etFirstName.text.toString().trim()
            val lName = etLastName.text.toString().trim()
            val bio = etBio.text.toString().trim()
            val classe = etClasse.text.toString().trim()

            updateProfile(fName, lName, bio, classe)
        }
    }

    private val REQUEST_CODE_AVATAR = 101
    private val REQUEST_CODE_BANNER = 102

    private fun openImagePicker(requestCode: Int) {
        val intent = android.content.Intent(android.content.Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            val imageUri = data.data
            if (imageUri != null) {
                if (requestCode == REQUEST_CODE_AVATAR) {
                    uploadAvatarToServer(imageUri)
                } else {
                    Toast.makeText(this, "Upload de bannière bientôt disponible", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun uploadAvatarToServer(uri: android.net.Uri) {
        val token = sessionManager.fetchAuthToken() ?: return
        
        lifecycleScope.launch {
            try {
                // Créer le Part depuis l'URI
                val inputStream = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: return@launch
                
                val mimeType = contentResolver.getType(uri) ?: "image/*"
                val mediaType = mimeType.toMediaTypeOrNull()
                val requestFile = bytes.toRequestBody(mediaType)
                
                val body = MultipartBody.Part.createFormData("avatar", "avatar.jpg", requestFile)

                val response = RetrofitClient.instance.uploadAvatar("Bearer $token", body)
                if (response.isSuccessful) {
                    Toast.makeText(this@EditProfileActivity, "Photo de profil mise à jour !", Toast.LENGTH_SHORT).show()
                    findViewById<TextView>(R.id.tvEditInitials).text = "📸"
                    // Optionnel: Recharger les infos pour avoir l'URL
                } else {
                    Log.e("EDIT_PROFILE", "Upload error: ${response.code()}")
                    Toast.makeText(this@EditProfileActivity, "Erreur lors de l'upload", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("EDIT_PROFILE", "Upload exception", e)
                Toast.makeText(this@EditProfileActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadCurrentProfile(
        etFName: TextInputEditText,
        etLName: TextInputEditText,
        etBio: TextInputEditText,
        etClasse: TextInputEditText,
        tvInit: TextView
    ) {
        val token = sessionManager.fetchAuthToken() ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getMe("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!.user
                    currentUser = user

                    etFName.setText(user.firstName ?: "")
                    etLName.setText(user.lastName ?: "")
                    etBio.setText(user.bio ?: "")
                    etClasse.setText(user.classe ?: "")
                    
                    val initialsSource = user.firstName?.ifEmpty { user.username } ?: "U"
                    tvInit.text = initialsSource.take(1).uppercase()
                }
            } catch (e: Exception) {
                Log.e("EDIT_PROFILE", "Load error", e)
            }
        }
    }

    private fun updateProfile(fName: String, lName: String, bio: String, classe: String) {
        val token = sessionManager.fetchAuthToken() ?: return
        val user = currentUser ?: return

        val updatedUser = UserResponse(
            id = user.id,
            email = user.email,
            username = user.username,
            firstName = fName,
            lastName = lName,
            bio = bio,
            role = user.role, // On garde le rôle actuel, on ne permet pas de le changer ici
            avatarUrl = user.avatarUrl,
            classe = classe
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.updateMe("Bearer $token", updatedUser)
                if (response.isSuccessful) {
                    Toast.makeText(this@EditProfileActivity, "Profil mis à jour !", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@EditProfileActivity, "Erreur lors de la sauvegarde", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditProfileActivity, "Erreur réseau", Toast.LENGTH_SHORT).show()
            }
        }
    }
}