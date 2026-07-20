package com.example.infolensai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import com.google.android.material.textfield.TextInputEditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        
        NotificationHelper.createNotificationChannel(this)
        
        auth = FirebaseAuth.getInstance()

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("178456145754-qdn4ir8jlhchdvr906nv3jqs8ekn8p8v.apps.googleusercontent.com")
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnSignIn = findViewById<Button>(R.id.btnSignIn)
        val tvForgetPassword = findViewById<TextView>(R.id.tvForgetPassword)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)
        val btnGoogle = findViewById<View>(R.id.btnContinueWithGoogle)

        tvForgetPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        tvSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        btnGoogle.setOnClickListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        btnSignIn.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty()) {
                etEmail.error = "Please enter your email"
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "write the e-mail correct"
            } else if (password.isEmpty()) {
                etPassword.error = "Please enter your password"
            } else {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            onLoginSuccess()
                        } else {
                            Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    onLoginSuccess()
                } else {
                    Toast.makeText(this, "Firebase auth with Google failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun onLoginSuccess() {
        val userEmail = auth.currentUser?.email ?: "User"
        
        // Show notification
        NotificationHelper.showNotification(
            this,
            "Login Successful",
            "Logged in successfully with $userEmail",
            1001
        )
        
        // Show Toast as requested "send the e-mail successfully I log in"
        Toast.makeText(this, "Logged in successfully with $userEmail", Toast.LENGTH_LONG).show()

        val sharedPref = getSharedPreferences("InfolensPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("isLoggedIn", true)
            putString("userEmail", userEmail) // Storing email for profile/consistency
            apply()
        }
        val intent = Intent(this, ChatActivity::class.java)
        startActivity(intent)
        finish()
    }
}