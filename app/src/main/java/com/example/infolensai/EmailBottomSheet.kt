package com.example.infolensai

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth

class EmailBottomSheet : BottomSheetDialogFragment() {

    interface EmailUpdateListener {
        fun onEmailUpdated()
    }

    private var listener: EmailUpdateListener? = null

    fun setEmailUpdateListener(listener: EmailUpdateListener) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_email, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val btnSave = view.findViewById<AppCompatButton>(R.id.btnSaveEmail)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancelEmail)

        val sharedPref = requireContext().getSharedPreferences("InfolensPrefs", Context.MODE_PRIVATE)
        val user = FirebaseAuth.getInstance().currentUser
        val currentEmail = user?.email ?: sharedPref.getString("userEmail", "xyz1528@gmail.com")
        etEmail.setText(currentEmail)

        btnSave.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (email.isEmpty()) {
                etEmail.error = "Email cannot be empty"
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Invalid email format"
                return@setOnClickListener
            }

            // Update local profile email in SharedPreferences
            with(sharedPref.edit()) {
                putString("userEmail", email)
                apply()
            }

            NotificationHelper.showNotification(
                requireContext(),
                "Profile Updated",
                "Your profile email has been updated to $email",
                1004
            )

            Toast.makeText(requireContext(), "Email updated in profile", Toast.LENGTH_SHORT).show()
            listener?.onEmailUpdated()
            dismiss()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        const val TAG = "EmailBottomSheet"
    }
}