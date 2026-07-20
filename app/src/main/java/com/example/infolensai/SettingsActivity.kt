package com.example.infolensai

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity(), 
    ProfileEditBottomSheet.ProfileUpdateListener,
    EmailBottomSheet.EmailUpdateListener {

    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var avatarContainer: ImageView
    private lateinit var tvAvatarInitials: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        avatarContainer = findViewById(R.id.avatarContainer)
        tvAvatarInitials = findViewById(R.id.tvAvatarInitials)

        // Ensure email section is visible now
        tvUserEmail.visibility = View.VISIBLE

        loadProfileData()

        findViewById<ImageView>(R.id.btnClose).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btnProfile).setOnClickListener {
            val profileEditBottomSheet = ProfileEditBottomSheet()
            profileEditBottomSheet.setProfileUpdateListener(this)
            profileEditBottomSheet.show(supportFragmentManager, ProfileEditBottomSheet.TAG)
        }

        findViewById<View>(R.id.btnEmail).setOnClickListener {
            val emailBottomSheet = EmailBottomSheet()
            emailBottomSheet.setEmailUpdateListener(this)
            emailBottomSheet.show(supportFragmentManager, EmailBottomSheet.TAG)
        }

        findViewById<View>(R.id.btnLanguage).setOnClickListener {
            Toast.makeText(this, "Language Selected", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btnReportBug).setOnClickListener {
            val reportBugBottomSheet = ReportBugBottomSheet()
            reportBugBottomSheet.show(supportFragmentManager, ReportBugBottomSheet.TAG)
        }

        findViewById<View>(R.id.btnSignOut).setOnClickListener {
            // Firebase Sign out
            FirebaseAuth.getInstance().signOut()

            // Google Sign out
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("178456145754-qdn4ir8jlhchdvr906nv3jqs8ekn8p8v.apps.googleusercontent.com")
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(this, gso)
            
            googleSignInClient.signOut().addOnCompleteListener {
                val sharedPref = getSharedPreferences("InfolensPrefs", Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putBoolean("isLoggedIn", false)
                    apply()
                }

                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadProfileData() {
        val sharedPref = getSharedPreferences("InfolensPrefs", Context.MODE_PRIVATE)
        val name = sharedPref.getString("userName", "User")
        val email = sharedPref.getString("userEmail", "xyz1528@gmail.com")
        val imageBase64 = sharedPref.getString("userImage", null)
        
        tvUserName.text = name
        tvUserEmail.text = email

        if (imageBase64 != null) {
            try {
                val decodedByte = Base64.decode(imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
                avatarContainer.setImageBitmap(bitmap)
                tvAvatarInitials.visibility = View.GONE
            } catch (e: Exception) {
                avatarContainer.setImageResource(0)
                tvAvatarInitials.visibility = View.VISIBLE
                tvAvatarInitials.text = if (!name.isNullOrEmpty()) name.take(2).uppercase() else "RO"
            }
        } else {
            avatarContainer.setImageResource(0)
            tvAvatarInitials.visibility = View.VISIBLE
            tvAvatarInitials.text = if (!name.isNullOrEmpty()) name.take(2).uppercase() else "RO"
        }
    }

    override fun onProfileUpdated() {
        loadProfileData()
    }

    override fun onEmailUpdated() {
        loadProfileData()
    }
}