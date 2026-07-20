package com.example.infolensai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import com.google.android.material.textfield.TextInputEditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)

        NotificationHelper.createNotificationChannel(this)

        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.etSignUpEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etSignUpPassword)
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)

        btnSignUp.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty()) {
                etEmail.error = "Email is required"
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Please enter a valid email"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                etPassword.error = "Password is required"
                return@setOnClickListener
            }

            if (password.length < 6) {
                etPassword.error = "Password should be at least 6 characters"
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userEmail = auth.currentUser?.email ?: email
                        NotificationHelper.showNotification(
                            this,
                            "Sign Up Successful",
                            "Welcome to Infolens! Registered as $userEmail",
                            1002
                        )
                        Toast.makeText(this, "Sign up successful! Welcome $userEmail", Toast.LENGTH_LONG).show()
                        
                        // Save login state and email
                        val sharedPref = getSharedPreferences("InfolensPrefs", Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putBoolean("isLoggedIn", true)
                            putString("userEmail", userEmail)
                            apply()
                        }

                        val intent = Intent(this, ChatActivity::class.java)
                        startActivity(intent)
                        finishAffinity()
                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        tvLogin.setOnClickListener {
            finish()
        }
    }
}