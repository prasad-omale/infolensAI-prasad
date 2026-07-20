package com.example.infolensai

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splash_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val logo = findViewById<ImageView>(R.id.splash_logo)
        val text = findViewById<TextView>(R.id.splash_text)

        val logoAnim = AnimationUtils.loadAnimation(this, R.anim.splash_logo_cinematic)
        val textAnim = AnimationUtils.loadAnimation(this, R.anim.splash_text_cinematic)
        
        logo.alpha = 1f
        logo.startAnimation(logoAnim)
        
        text.alpha = 1f
        text.startAnimation(textAnim)

        // Check login status
        val sharedPref = getSharedPreferences("InfolensPrefs", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)

        // Navigate after cinematic sequence
        logo.postDelayed({
            val intent = if (isLoggedIn) {
                Intent(this, ChatActivity::class.java)
            } else {
                Intent(this, MainActivity::class.java)
            }
            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 1200)
    }
}