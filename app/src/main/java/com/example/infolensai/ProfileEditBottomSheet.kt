package com.example.infolensai

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.ByteArrayOutputStream

class ProfileEditBottomSheet : BottomSheetDialogFragment() {

    interface ProfileUpdateListener {
        fun onProfileUpdated()
    }

    private var listener: ProfileUpdateListener? = null
    private lateinit var profileAvatar: ImageView
    private lateinit var tvAvatarInitials: TextView
    private var selectedImageUri: Uri? = null
    private var capturedBitmap: Bitmap? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            capturedBitmap = null
            profileAvatar.setImageURI(selectedImageUri)
            tvAvatarInitials.visibility = View.GONE
        }
    }

    private val captureImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                capturedBitmap = bitmap
                selectedImageUri = null
                profileAvatar.setImageBitmap(capturedBitmap)
                tvAvatarInitials.visibility = View.GONE
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
            Toast.makeText(requireContext(), "Permissions required to change profile photo", Toast.LENGTH_SHORT).show()
        }
    }

    fun setProfileUpdateListener(listener: ProfileUpdateListener) {
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
        return inflater.inflate(R.layout.activity_profile_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etName = view.findViewById<EditText>(R.id.etName)
        val btnSave = view.findViewById<AppCompatButton>(R.id.btnSaveProfile)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancel)
        val btnCamera = view.findViewById<ImageView>(R.id.btnCamera)
        profileAvatar = view.findViewById(R.id.profileAvatar)
        tvAvatarInitials = view.findViewById(R.id.tvAvatarInitials)

        // Load existing data
        val sharedPref = requireContext().getSharedPreferences("InfolensPrefs", Context.MODE_PRIVATE)
        val savedName = sharedPref.getString("userName", "")
        val savedImageBase64 = sharedPref.getString("userImage", null)

        etName.setText(savedName)
        
        if (savedImageBase64 != null) {
            val decodedByte = Base64.decode(savedImageBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
            profileAvatar.setImageBitmap(bitmap)
            tvAvatarInitials.visibility = View.GONE
        } else {
            tvAvatarInitials.text = if (!savedName.isNullOrEmpty()) savedName.take(2).uppercase() else "RO"
        }

        btnCamera.setOnClickListener {
            checkPermissionsAndShowDialog()
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()

            if (name.isEmpty()) {
                etName.error = "Name cannot be empty"
                return@setOnClickListener
            }

            // Save to SharedPreferences
            with(sharedPref.edit()) {
                putString("userName", name)
                
                // Save image if selected/captured
                try {
                    if (capturedBitmap != null) {
                        putString("userImage", encodeImage(capturedBitmap!!))
                    } else if (selectedImageUri != null) {
                        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val source = ImageDecoder.createSource(requireContext().contentResolver, selectedImageUri!!)
                            ImageDecoder.decodeBitmap(source)
                        } else {
                            @Suppress("DEPRECATION")
                            MediaStore.Images.Media.getBitmap(requireContext().contentResolver, selectedImageUri)
                        }
                        putString("userImage", encodeImage(bitmap))
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show()
                }
                
                apply()
            }

            Toast.makeText(requireContext(), "Profile Saved", Toast.LENGTH_SHORT).show()
            listener?.onProfileUpdated()
            dismiss()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun checkPermissionsAndShowDialog() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(requireContext(), storagePermission) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(storagePermission)
        }

        if (permissionsToRequest.isEmpty()) {
            showImageSourceDialog()
        } else {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(requireContext())
            .setTitle("Profile Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
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

    private fun encodeImage(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val b = outputStream.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    companion object {
        const val TAG = "ProfileEditBottomSheet"
    }
}