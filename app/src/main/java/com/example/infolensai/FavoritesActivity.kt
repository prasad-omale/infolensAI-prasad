package com.example.infolensai

import android.content.Intent
import android.os.Bundle
import android.view.View
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

class FavoritesActivity : AppCompatActivity() {

    private lateinit var rvFavorites: RecyclerView
    private lateinit var pbFavLoading: ProgressBar
    private lateinit var tvNoFavs: TextView
    private lateinit var adapter: RecentChatAdapter
    private val favoriteSessions = mutableListOf<ChatSession>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_favorites)

        rvFavorites = findViewById(R.id.rvFavorites)
        pbFavLoading = findViewById(R.id.pbFavLoading)
        tvNoFavs = findViewById(R.id.tvNoFavs)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        setupRecyclerView()
        loadFavorites()

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
        adapter = RecentChatAdapter(favoriteSessions, { session ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("SESSION_ID", session.id)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }, { session ->
            showChatOptionsDialog(session)
        })
        rvFavorites.layoutManager = LinearLayoutManager(this)
        rvFavorites.adapter = adapter
    }

    private fun loadFavorites() {
        pbFavLoading.visibility = View.VISIBLE
        ChatHistoryManager.getFavoriteSessions { sessions ->
            runOnUiThread {
                pbFavLoading.visibility = View.GONE
                favoriteSessions.clear()
                favoriteSessions.addAll(sessions)
                adapter.notifyDataSetChanged()
                tvNoFavs.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showChatOptionsDialog(session: ChatSession) {
        val options = arrayOf("Unfavorite", "Delete", "Cancel")

        AlertDialog.Builder(this)
            .setTitle("Chat Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> toggleFavorite(session)
                    1 -> deleteSession(session)
                }
            }
            .show()
    }

    private fun toggleFavorite(session: ChatSession) {
        pbFavLoading.visibility = View.VISIBLE
        ChatHistoryManager.toggleFavorite(session.id, false) { success ->
            runOnUiThread {
                pbFavLoading.visibility = View.GONE
                if (success) {
                    loadFavorites()
                    Toast.makeText(this, "Removed from Favorites", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteSession(session: ChatSession) {
        AlertDialog.Builder(this)
            .setTitle("Delete Chat")
            .setMessage("Are you sure you want to delete this chat session?")
            .setPositiveButton("Delete") { _, _ ->
                pbFavLoading.visibility = View.VISIBLE
                ChatHistoryManager.deleteSession(session.id) { success ->
                    runOnUiThread {
                        pbFavLoading.visibility = View.GONE
                        if (success) {
                            loadFavorites()
                            Toast.makeText(this, "Chat deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}