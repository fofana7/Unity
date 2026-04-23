package com.example.unity

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class FriendsBottomSheetFragment(
    private val friends: List<UserResponse>,
    private val title: String
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_friends, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        view.findViewById<TextView>(R.id.tvTitle).text = title
        
        val rv = view.findViewById<RecyclerView>(R.id.rvFriends)
        rv.layoutManager = LinearLayoutManager(context)
        
        val sessionManager = SessionManager(requireContext())
        val token = sessionManager.fetchAuthToken()
        val myId = sessionManager.fetchUserId()
        
        // Initialiser l'adapter
        val adapter = FriendSuggestionsAdapter(friends, emptySet(), myId) { user ->
            handleAddFriend(user, token, myId)
        }
        rv.adapter = adapter

        // Charger mes amis pour mettre à jour les statuts
        if (token != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.getMyFriends("Bearer $token")
                    if (response.isSuccessful) {
                        val myFriends = response.body() ?: emptyList()
                        val myFriendIds = myFriends.mapNotNull { it.id }.toSet()
                        adapter.updateData(friends, myFriendIds)
                    }
                } catch (e: Exception) {
                    Log.e("FRIENDS_BS", "Error fetching my friends", e)
                }
            }
        }
    }

    private fun handleAddFriend(user: UserResponse, token: String?, myId: Int) {
        if (token == null) return
        
        lifecycleScope.launch {
            try {
                val req = FriendActionRequest(friendId = user.id ?: -1, requesterId = myId, status = "pending")
                val res = RetrofitClient.instance.addFriend("Bearer $token", req)
                if (res.isSuccessful) {
                    Toast.makeText(requireContext(), "Demande envoyée à ${user.username}", Toast.LENGTH_SHORT).show()
                } else {
                    val errorBody = res.errorBody()?.string()
                    Log.e("FRIENDS_BS", "Error adding friend: $errorBody")
                    val msg = if (errorBody?.contains("Relation déjà existante") == true) {
                        "Déjà envoyé"
                    } else {
                        "Erreur serveur (${res.code()})"
                    }
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("FRIENDS_BS", "Error adding friend", e)
            }
        }
    }
}
