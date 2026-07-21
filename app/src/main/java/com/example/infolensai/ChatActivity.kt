package com.example.infolensai

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnMenu: ImageView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageView
    private lateinit var btnPlus: ImageView
    private lateinit var rvChat: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var profileIcon: ImageView
    private lateinit var previewContainer: View
    private lateinit var ivPreview: ImageView
    private lateinit var pbLoading: ProgressBar
    private lateinit var mainProgressBar: ProgressBar
    private lateinit var btnRemovePreview: ImageView
    
    private lateinit var rvRecents: RecyclerView
    private lateinit var recentChatAdapter: RecentChatAdapter
    private val recentChatsList = mutableListOf<ChatSession>()
    
    private var selectedImageBitmap: Bitmap? = null
    private val messages = mutableListOf<ChatMessage>()
    
    private var currentSessionId: String? = null
    private lateinit var geminiClient: GeminiClient

    // Constant for Gemini API Key
    private companion object {
        const val GEMINI_API_KEY = ""
    }

    private val captureImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                processAndShowImage(bitmap)
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                try {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(contentResolver, uri)
                        ImageDecoder.decodeBitmap(source)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    }
                    processAndShowImage(bitmap)
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val storageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        }

        if (cameraGranted && storageGranted) {
            showImageSourceDialog()
        } else {
            Toast.makeText(this, "Permissions required to share photos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)
        
        val chatLayout = findViewById<View>(R.id.chat_main)
        ViewCompat.setOnApplyWindowInsetsListener(chatLayout) { v, insets ->
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

        val centerLogo = findViewById<ImageView>(R.id.centerLogo)
        val tvTitle = findViewById<View>(R.id.tvChatTitle)
        val tvSubtitle = findViewById<View>(R.id.tvChatSubtitle)
        val rings = listOf(R.id.ring_outer, R.id.ring_inner, R.id.circle_solid)

        rvChat = findViewById(R.id.rvChat)
        chatAdapter = ChatAdapter(messages)
        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = chatAdapter

        rvRecents = findViewById(R.id.rvRecents)

        val onSessionClick: (ChatSession) -> Unit = { session ->
            drawerLayout.closeDrawer(GravityCompat.START)
            loadSession(session.id)
        }

        recentChatAdapter = RecentChatAdapter(recentChatsList, onSessionClick, { session ->
            showChatOptionsDialog(session)
        })
        rvRecents.layoutManager = LinearLayoutManager(this)
        rvRecents.adapter = recentChatAdapter

        ChatHistoryManager.getRecentSessions { sessions ->
            runOnUiThread {
                recentChatAdapter.setChats(sessions)
            }
        }

        currentSessionId = null

        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnPlus = findViewById(R.id.btnPlus)
        btnMenu = findViewById(R.id.btnMenu)
        profileIcon = findViewById(R.id.profileIcon)
        drawerLayout = findViewById(R.id.drawerLayout)
        
        previewContainer = findViewById(R.id.previewContainer)
        ivPreview = findViewById(R.id.ivPreview)
        pbLoading = findViewById(R.id.pbLoading)
        mainProgressBar = findViewById(R.id.mainProgressBar)
        btnRemovePreview = findViewById(R.id.btnRemovePreview)

        // Initialize Gemini Client
        geminiClient = GeminiClient(GEMINI_API_KEY)

        loadProfileImage()

        btnRemovePreview.setOnClickListener {
            removeImagePreview()
        }

        rvChat.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom) {
                rvChat.postDelayed({
                    if (messages.isNotEmpty()) {
                        rvChat.scrollToPosition(messages.size - 1)
                    }
                }, 100)
            }
        }

        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
            // Refresh recents whenever drawer is opened to exit "Favorites" view if active
            ChatHistoryManager.getRecentSessions { sessions ->
                runOnUiThread { recentChatAdapter.setChats(sessions) }
            }
        }

        findViewById<View>(R.id.navSearchChat).setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        findViewById<View>(R.id.navFavorites).setOnClickListener {
            val intent = Intent(this, FavoritesActivity::class.java)
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        profileIcon.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.navNewChat).setOnClickListener {
            currentSessionId = null
            messages.clear()
            chatAdapter.notifyDataSetChanged()
            
            centerLogo.visibility = View.VISIBLE
            tvTitle.visibility = View.VISIBLE
            tvSubtitle.visibility = View.VISIBLE
            rings.forEach { id -> findViewById<View>(id).visibility = View.VISIBLE }
            rvChat.visibility = View.GONE
            
            drawerLayout.closeDrawer(GravityCompat.START)
            etMessage.text.clear()
            removeImagePreview()
            Toast.makeText(this, "New Chat Ready", Toast.LENGTH_SHORT).show()
        }

        etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSendButtonVisibility()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnSend.setOnClickListener {
            val message = etMessage.text.toString()
            if (message.isNotEmpty() || selectedImageBitmap != null) {
                centerLogo.visibility = View.GONE
                tvTitle.visibility = View.GONE
                tvSubtitle.visibility = View.GONE
                rings.forEach { id -> findViewById<View>(id).visibility = View.GONE }
                rvChat.visibility = View.VISIBLE
                
                val imageToUpload = selectedImageBitmap
                val userMsg = ChatMessage(message, true, imageToUpload)
                messages.add(userMsg)
                chatAdapter.notifyItemInserted(messages.size - 1)

                if (currentSessionId == null) {
                    ChatHistoryManager.createNewSession { sessionId ->
                        runOnUiThread {
                            currentSessionId = sessionId
                            saveAndProcessMessage(sessionId, userMsg, message, imageToUpload)
                        }
                    }
                } else {
                    saveAndProcessMessage(currentSessionId!!, userMsg, message, imageToUpload)
                }

                etMessage.text.clear()
                removeImagePreview()
                rvChat.scrollToPosition(messages.size - 1)
            }
        }

        btnPlus.setOnClickListener {
            checkPermissionsAndShowDialog()
        }
    }

    private fun saveAndProcessMessage(sid: String, userMsg: ChatMessage, text: String, image: Bitmap?) {
        ChatHistoryManager.saveMessage(sid, userMsg)
        
        if (messages.size == 1) {
            val title = if (text.length > 25) text.take(25) + "..." else if (text.isEmpty() && image != null) "Image Chat" else text
            ChatHistoryManager.updateSessionTitle(sid, title)
        }

        getGeminiResponse(text, image)
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val outputStream = java.io.ByteArrayOutputStream()
        val scaledBitmap = if (bitmap.width > 1024 || bitmap.height > 1024) {
            val ratio = Math.min(1024f / bitmap.width, 1024f / bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else {
            bitmap
        }
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun getGeminiResponse(userMessage: String, image: Bitmap?) {
        chatAdapter.showLoading()
        rvChat.scrollToPosition(messages.size - 1)
        
        val base64Image = image?.let { encodeImageToBase64(it) }
        
        val systemPrompt = "You are InfoLens AI. Carefully analyze the uploaded image. Identify the primary object, place, product, animal, plant, or document visible. Explain what it is in clear, natural English. Mention important details, uses, features, history, or interesting facts when applicable. If text is visible, use it to improve the identification. Never invent facts. If the object cannot be identified confidently, clearly state that. Please provide the response in a natural conversational style, avoiding markdown, raw JSON, or raw labels."
        
        val prompt = if (image != null) {
            if (userMessage.isNotEmpty()) {
                "You are InfoLens AI. Answer the user's question about the image provided: $userMessage. Please respond in clear, natural English without markdown."
            } else {
                systemPrompt
            }
        } else {
            userMessage
        }

        geminiClient.generateResponse(prompt, base64Image) { info, error ->
            runOnUiThread {
                chatAdapter.hideLoading()
                if (error != null) {
                    val friendlyError = if (error == "QUOTA_EXCEEDED") {
                        "InfoLens is currently busy (limit reached). Please try again in 60 seconds."
                    } else {
                        "Error: $error"
                    }
                    val aiMsg = ChatMessage(friendlyError, false)
                    messages.add(aiMsg)
                    chatAdapter.notifyItemInserted(messages.size - 1)
                } else {
                    val aiMsg = ChatMessage(info ?: "No response", false)
                    messages.add(aiMsg)
                    chatAdapter.notifyItemInserted(messages.size - 1)
                    currentSessionId?.let { sid -> ChatHistoryManager.saveMessage(sid, aiMsg) }
                }
                rvChat.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun updateSendButtonVisibility() {
        btnSend.visibility = if (etMessage.text.isNullOrEmpty() && selectedImageBitmap == null) View.GONE else View.VISIBLE
    }

    private fun checkPermissionsAndShowDialog() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, storagePermission) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(storagePermission)
        }

        if (permissionsToRequest.isEmpty()) {
            showImageSourceDialog()
        } else {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Camera", "Gallery")
        AlertDialog.Builder(this)
            .setTitle("Select Image Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            captureImageLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
                        } else {
                            checkPermissionsAndShowDialog()
                        }
                    }
                    1 -> pickImageLauncher.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI))
                }
            }
            .show()
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
                    2 -> { // Cancel
                        // Do nothing
                    }
                }
            }
            .show()
    }

    private fun toggleFavorite(session: ChatSession) {
        val newStatus = !session.isFavorite
        mainProgressBar.visibility = View.VISIBLE
        ChatHistoryManager.toggleFavorite(session.id, newStatus) { success ->
            runOnUiThread {
                mainProgressBar.visibility = View.GONE
                if (success) {
                    val msg = if (newStatus) "Added to Favorites" else "Removed from Favorites"
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    // Refresh the list
                    ChatHistoryManager.getRecentSessions { sessions ->
                        runOnUiThread { recentChatAdapter.setChats(sessions) }
                    }
                } else {
                    Toast.makeText(this, "Failed to update favorite status", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(session: ChatSession) {
        AlertDialog.Builder(this)
            .setTitle("Delete Chat")
            .setMessage("Are you sure you want to delete this chat session?")
            .setPositiveButton("Delete") { _, _ ->
                mainProgressBar.visibility = View.VISIBLE
                ChatHistoryManager.deleteSession(session.id) { success ->
                    runOnUiThread {
                        mainProgressBar.visibility = View.GONE
                        if (success) {
                            Toast.makeText(this, "Chat deleted", Toast.LENGTH_SHORT).show()
                            ChatHistoryManager.getRecentSessions { sessions ->
                                runOnUiThread { recentChatAdapter.setChats(sessions) }
                            }
                            if (currentSessionId == session.id) {
                                messages.clear()
                                chatAdapter.notifyDataSetChanged()
                                currentSessionId = null
                                findViewById<ImageView>(R.id.centerLogo).visibility = View.VISIBLE
                                findViewById<View>(R.id.tvChatTitle).visibility = View.VISIBLE
                                findViewById<View>(R.id.tvChatSubtitle).visibility = View.VISIBLE
                                val rings = listOf(R.id.ring_outer, R.id.ring_inner, R.id.circle_solid)
                                rings.forEach { id -> findViewById<View>(id).visibility = View.VISIBLE }
                                rvChat.visibility = View.GONE
                            }
                        } else {
                            Toast.makeText(this, "Failed to delete chat", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun processAndShowImage(bitmap: Bitmap) {
        selectedImageBitmap = bitmap
        previewContainer.visibility = View.VISIBLE
        ivPreview.setImageBitmap(bitmap)
        updateSendButtonVisibility()
    }

    private fun removeImagePreview() {
        selectedImageBitmap = null
        previewContainer.visibility = View.GONE
        updateSendButtonVisibility()
    }

    private fun loadProfileImage() {
        val sharedPref = getSharedPreferences("InfolensPrefs", Context.MODE_PRIVATE)
        val imageBase64 = sharedPref.getString("userImage", null)
        if (imageBase64 != null) {
            try {
                val decodedByte = Base64.decode(imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
                profileIcon.setImageBitmap(bitmap)
            } catch (e: Exception) {
                profileIcon.setImageResource(R.drawable.icon_logo)
            }
        } else {
            profileIcon.setImageResource(R.drawable.icon_logo)
        }
    }

    override fun onResume() {
        super.onResume()
        loadProfileImage()
        
        // Handle session ID if passed via intent (e.g., from SearchActivity)
        intent.getStringExtra("SESSION_ID")?.let { sessionId ->
            loadSession(sessionId)
            intent.removeExtra("SESSION_ID")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun loadSession(sessionId: String) {
        currentSessionId = sessionId
        mainProgressBar.visibility = View.VISIBLE
        ChatHistoryManager.getMessages(sessionId) { history ->
            runOnUiThread {
                mainProgressBar.visibility = View.GONE
                messages.clear()
                messages.addAll(history)
                chatAdapter.notifyDataSetChanged()

                findViewById<ImageView>(R.id.centerLogo).visibility = View.GONE
                findViewById<View>(R.id.tvChatTitle).visibility = View.GONE
                findViewById<View>(R.id.tvChatSubtitle).visibility = View.GONE
                val rings = listOf(R.id.ring_outer, R.id.ring_inner, R.id.circle_solid)
                rings.forEach { id -> findViewById<View>(id).visibility = View.GONE }
                rvChat.visibility = View.VISIBLE
                if (messages.isNotEmpty()) {
                    rvChat.scrollToPosition(messages.size - 1)
                }
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        }
    }
}
