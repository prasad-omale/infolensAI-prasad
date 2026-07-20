package com.example.infolensai

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    
    private lateinit var ivArrowHandle: ImageView
    private lateinit var tvBtnText: TextView
    private lateinit var btnGetStarted: View
    private var initialX = 0f
    
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        testFirebase()

        btnGetStarted = findViewById(R.id.btnGetStarted)
        ivArrowHandle = findViewById(R.id.ivArrowHandle)
        tvBtnText = findViewById(R.id.tvBtnText)

        setupSwipeButton()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSwipeButton() {
        ivArrowHandle.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = event.rawX
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialX
                    val maxScroll = btnGetStarted.width - view.width - 20 // 20 for margin
                    
                    if (deltaX > 0 && deltaX < maxScroll) {
                        view.translationX = deltaX
                        // Fade text as we swipe
                        val alpha = 1 - (deltaX / maxScroll)
                        tvBtnText.alpha = alpha
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val deltaX = event.rawX - initialX
                    val maxScroll = btnGetStarted.width - view.width - 20
                    
                    if (deltaX > maxScroll * 0.7) {
                        // Success! Animate to end and launch
                        view.animate()
                            .translationX(maxScroll.toFloat())
                            .setDuration(100)
                            .withEndAction {
                                val intent = Intent(this, LoginActivity::class.java)
                                startActivity(intent)
                                // Reset for when user comes back
                                view.postDelayed({
                                    view.translationX = 0f
                                    tvBtnText.alpha = 1f
                                }, 500)
                            }
                            .start()
                    } else {
                        // Failed, animate back to start
                        view.animate()
                            .translationX(0f)
                            .setDuration(200)
                            .start()
                        tvBtnText.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .start()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun testFirebase() {
        // Test Firestore
        val db = FirebaseFirestore.getInstance()
        val user = hashMapOf(
            "name" to "Hero",
            "age" to 20
        )
        db.collection("Users")
            .add(user)
            .addOnSuccessListener { Log.d("FirebaseTest", "Firestore Success") }
            .addOnFailureListener { e -> Log.w("FirebaseTest", "Firestore Error", e) }

        // Test Auth
        FirebaseAuth.getInstance()
            .createUserWithEmailAndPassword("test${System.currentTimeMillis()}@gmail.com", "123456")
            .addOnSuccessListener { Log.d("FirebaseTest", "Auth Success") }
            .addOnFailureListener { e -> Log.w("FirebaseTest", "Auth Error", e) }
    }
}