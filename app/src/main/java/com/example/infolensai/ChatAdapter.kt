package com.example.infolensai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

data class ChatMessage(
    val text: String, 
    val isUser: Boolean, 
    val image: Bitmap? = null,
    val isLoading: Boolean = false
)

class ChatAdapter(private val messages: MutableList<ChatMessage>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
        private const val VIEW_TYPE_LOADING = 3
    }

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val ivChatMessage: ImageView = view.findViewById(R.id.ivChatMessage)
    }

    class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.isLoading -> VIEW_TYPE_LOADING
            message.isUser -> VIEW_TYPE_USER
            else -> VIEW_TYPE_AI
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_LOADING) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_loading, parent, false)
            LoadingViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
            ChatViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        if (holder is LoadingViewHolder) {
            // ProgressBar handles its own indeterminate animation
            return
        }

        holder as ChatViewHolder
        // Handle Text
        if (message.text.isEmpty()) {
            holder.tvMessage.visibility = View.GONE
        } else {
            holder.tvMessage.visibility = View.VISIBLE
            holder.tvMessage.text = message.text
        }

        // Handle Image
        if (message.image != null) {
            holder.ivChatMessage.visibility = View.VISIBLE
            holder.ivChatMessage.setImageBitmap(message.image)
        } else {
            holder.ivChatMessage.visibility = View.GONE
        }
        
        val textParams = holder.tvMessage.layoutParams as LinearLayout.LayoutParams
        val imageParams = holder.ivChatMessage.layoutParams as LinearLayout.LayoutParams
        
        if (message.isUser) {
            textParams.gravity = Gravity.END
            imageParams.gravity = Gravity.END
            holder.tvMessage.setBackgroundResource(R.drawable.glass_effect_bg)
        } else {
            textParams.gravity = Gravity.START
            imageParams.gravity = Gravity.START
            holder.tvMessage.setBackgroundResource(R.drawable.chat_input_pill_bg)
        }
        
        holder.tvMessage.layoutParams = textParams
        holder.ivChatMessage.layoutParams = imageParams

        // Copy text on long click
        holder.tvMessage.setOnLongClickListener {
            val context = holder.itemView.context
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Infolens Message", message.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
            true
        }
    }

    override fun getItemCount() = messages.size

    fun addMessage(text: String, isUser: Boolean, image: Bitmap? = null) {
        messages.add(ChatMessage(text, isUser, image))
        notifyItemInserted(messages.size - 1)
    }

    fun showLoading() {
        messages.add(ChatMessage("", false, null, true))
        notifyItemInserted(messages.size - 1)
    }

    fun hideLoading() {
        val lastIndex = messages.indexOfLast { it.isLoading }
        if (lastIndex != -1) {
            messages.removeAt(lastIndex)
            notifyItemRemoved(lastIndex)
        }
    }
}