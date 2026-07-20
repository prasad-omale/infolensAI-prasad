package com.example.infolensai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class ChatSession(
    val id: String, 
    val title: String, 
    val timestamp: Long,
    val isFavorite: Boolean = false
)

class RecentChatAdapter(
    private val recentChats: MutableList<ChatSession>,
    private val onChatClick: (ChatSession) -> Unit,
    private val onChatLongClick: (ChatSession) -> Unit
) : RecyclerView.Adapter<RecentChatAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvRecentChatTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recent_chat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = recentChats[position]
        holder.tvTitle.text = session.title
        holder.itemView.setOnClickListener { onChatClick(session) }
        holder.itemView.setOnLongClickListener {
            onChatLongClick(session)
            true
        }
    }

    override fun getItemCount() = recentChats.size

    fun addRecent(session: ChatSession) {
        recentChats.add(0, session)
        notifyItemInserted(0)
    }

    fun setChats(chats: List<ChatSession>) {
        recentChats.clear()
        recentChats.addAll(chats)
        notifyDataSetChanged()
    }
}