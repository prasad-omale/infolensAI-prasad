package com.example.infolensai

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SearchActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var pbSearchLoading: ProgressBar
    private lateinit var tvNoResults: TextView
    private lateinit var adapter: RecentChatAdapter
    
    private var allSessions = mutableListOf<ChatSession>()
    private var filteredSessions = mutableListOf<ChatSession>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_search)
        
        etSearch = findViewById(R.id.etSearch)
        rvSearchResults = findViewById(R.id.rvSearchResults)
        pbSearchLoading = findViewById(R.id.pbSearchLoading)
        tvNoResults = findViewById(R.id.tvNoResults)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        setupRecyclerView()
        loadChats()
        setupSearch()

        val mainLayout = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            
            v.setPadding(
                systemBars.left, 
                systemBars.top, 
                systemBars.right, 
                if (imeInsets.bottom > 0) imeInsets.bottom else systemBars.bottom
            )
            insets
        }
    }

    private fun setupRecyclerView() {
        adapter = RecentChatAdapter(filteredSessions, { session ->
            // On click: Go to ChatActivity and load this session
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("SESSION_ID", session.id)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }, { session ->
            // Handle long click (options dialog)
            showChatOptionsDialog(session)
        })
        rvSearchResults.layoutManager = LinearLayoutManager(this)
        rvSearchResults.adapter = adapter
    }

    private fun showChatOptionsDialog(session: ChatSession) {
        val options = if (session.isFavorite) {
            arrayOf("Unfavorite", "Delete", "Cancel")
        } else {
            arrayOf("Favorite", "Delete", "Cancel")
        }

        AlertDialog.Builder(this)
            .setTitle("Chat Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { // Favorite/Unfavorite
                        toggleFavorite(session)
                    }
                    1 -> { // Delete
                        showDeleteConfirmationDialog(session)
                    }
                }
            }
            .show()
    }

    private fun toggleFavorite(session: ChatSession) {
        val newStatus = !session.isFavorite
        pbSearchLoading.visibility = View.VISIBLE
        ChatHistoryManager.toggleFavorite(session.id, newStatus) { success ->
            runOnUiThread {
                pbSearchLoading.visibility = View.GONE
                if (success) {
                    loadChats() // Refresh
                    val msg = if (newStatus) "Added to Favorites" else "Removed from Favorites"
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(session: ChatSession) {
        AlertDialog.Builder(this)
            .setTitle("Delete Chat")
            .setMessage("Are you sure you want to delete this chat session?")
            .setPositiveButton("Delete") { _, _ ->
                pbSearchLoading.visibility = View.VISIBLE
                ChatHistoryManager.deleteSession(session.id) { success ->
                    runOnUiThread {
                        pbSearchLoading.visibility = View.GONE
                        if (success) {
                            loadChats()
                            Toast.makeText(this, "Chat deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadChats() {
        pbSearchLoading.visibility = View.VISIBLE
        ChatHistoryManager.getRecentSessions { sessions ->
            runOnUiThread {
                pbSearchLoading.visibility = View.GONE
                allSessions.clear()
                allSessions.addAll(sessions)
                filterChats("") // Initial show all
            }
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterChats(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterChats(query: String) {
        filteredSessions.clear()
        if (query.isEmpty()) {
            filteredSessions.addAll(allSessions)
        } else {
            val lowerQuery = query.lowercase()
            allSessions.filterTo(filteredSessions) { 
                it.title.lowercase().contains(lowerQuery)
            }
        }
        adapter.notifyDataSetChanged()
        
        tvNoResults.visibility = if (filteredSessions.isEmpty() && allSessions.isNotEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
}