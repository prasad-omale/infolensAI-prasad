package com.example.infolensai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ReportBugBottomSheet : BottomSheetDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_report_bug, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etDescription = view.findViewById<EditText>(R.id.etBugDescription)
        val btnSend = view.findViewById<AppCompatButton>(R.id.btnSendBug)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancelBug)

        btnSend.setOnClickListener {
            val description = etDescription.text.toString().trim()

            if (description.isEmpty()) {
                etDescription.error = "Please describe the bug"
                return@setOnClickListener
            }

            // In a real app, you would send this to a server or email
            Toast.makeText(requireContext(), "Thank you for your report!", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        const val TAG = "ReportBugBottomSheet"
    }
}