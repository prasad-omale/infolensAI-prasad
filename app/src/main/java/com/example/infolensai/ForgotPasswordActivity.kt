package com.example.infolensai

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import com.google.android.material.textfield.TextInputEditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ActionCodeResult

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        NotificationHelper.createNotificationChannel(this)

        auth = FirebaseAuth.getInstance()

        val etForgotEmail = findViewById<EditText>(R.id.etForgotEmail)
        val btnResetPassword = findViewById<Button>(R.id.btnResetPassword)
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        btnResetPassword.setOnClickListener {
            val email = etForgotEmail.text.toString().trim()

            if (email.isEmpty()) {
                etForgotEmail.error = "Email is required"
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etForgotEmail.error = "Please enter a valid email"
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        NotificationHelper.showNotification(
                            this,
                            "Reset Link Sent",
                            "Successfully sent password reset email to $email",
                            1003
                        )
                        Toast.makeText(this, "Password reset email sent!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        tvBackToLogin.setOnClickListener {
            finish()
        }

        // Handle the incoming reset link
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data
        if (data != null && data.getQueryParameter("mode") == "resetPassword") {
            val oobCode = data.getQueryParameter("oobCode")
            if (oobCode != null) {
                verifyResetCode(oobCode)
            }
        }
    }

    private fun verifyResetCode(oobCode: String) {
        auth.verifyPasswordResetCode(oobCode)
            .addOnSuccessListener { email ->
                showNewPasswordDialog(oobCode, email)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Invalid or expired link: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showNewPasswordDialog(oobCode: String, email: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reset_password, null)
        val etNewPassword = dialogView.findViewById<TextInputEditText>(R.id.etNewPassword)
        val etConfirmNewPassword = dialogView.findViewById<TextInputEditText>(R.id.etConfirmNewPassword)
        val tvResetInfo = dialogView.findViewById<TextView>(R.id.tvResetInfo)
        
        tvResetInfo.text = "Set new password for $email"

        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<Button>(R.id.btnConfirmReset).setOnClickListener {
            val newPassword = etNewPassword.text.toString()
            val confirmPassword = etConfirmNewPassword.text.toString()

            if (newPassword.length < 6) {
                etNewPassword.error = "Minimum 6 characters"
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                etConfirmNewPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            auth.confirmPasswordReset(oobCode, newPassword)
                .addOnSuccessListener {
                    Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to reset password: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        dialogView.findViewById<TextView>(R.id.btnCancelReset).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}