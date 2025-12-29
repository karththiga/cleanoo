package com.example.rewardrecycleapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView

class ProfileFragment : Fragment() {

    private val PICK_IMAGE = 100
    private var imageUri: Uri? = null

    private lateinit var imgProfile: CircleImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // Menu buttons
    private lateinit var btnEditProfile: LinearLayout
    private lateinit var btnPhone: LinearLayout
    private lateinit var btnChangePassword: LinearLayout
    private lateinit var btnInbox: LinearLayout
    private lateinit var btnLogout: LinearLayout

    // Sections
    private lateinit var sectionEditProfile: LinearLayout
    private lateinit var sectionPhone: LinearLayout
    private lateinit var sectionPassword: LinearLayout
    private lateinit var sectionInbox: LinearLayout

    // Fields
    private lateinit var edtName: EditText
    private lateinit var btnSaveProfile: Button

    private lateinit var edtPhone: EditText
    private lateinit var btnSavePhone: Button

    private lateinit var edtPassword: EditText
    private lateinit var btnSavePassword: Button

    // Header Info
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        assignViews(view)
        loadUserDetails()
        setupClickListeners()

        return view
    }


    private fun assignViews(view: View) {
        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)

        imgProfile = view.findViewById(R.id.imgProfile)
        imgProfile.setOnClickListener { chooseImageFromGallery() }

        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnPhone = view.findViewById(R.id.btnPhone)
        btnChangePassword = view.findViewById(R.id.btnChangePassword)
        btnInbox = view.findViewById(R.id.btnInbox)
        btnLogout = view.findViewById(R.id.btnLogout)

        sectionEditProfile = view.findViewById(R.id.sectionEditProfile)
        sectionPhone = view.findViewById(R.id.sectionPhone)
        sectionPassword = view.findViewById(R.id.sectionPassword)
        sectionInbox = view.findViewById(R.id.sectionInbox)

        edtName = view.findViewById(R.id.edtName)
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile)

        edtPhone = view.findViewById(R.id.edtPhone)
        btnSavePhone = view.findViewById(R.id.btnSavePhone)

        edtPassword = view.findViewById(R.id.edtPassword)
        btnSavePassword = view.findViewById(R.id.btnSavePassword)
    }


    // Load user info + profile picture
    private fun loadUserDetails() {
        val user = auth.currentUser ?: return

        tvEmail.text = user.email

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {

                    val name = doc.getString("name")
                    val phone = doc.getString("phone")
                    val imageUrl = doc.getString("profileImage")

                    tvName.text = name ?: "User"
                    edtName.setText(name ?: "")
                    edtPhone.setText(phone ?: "")

                    // Load profile picture
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(requireContext())
                            .load(imageUrl)
                            .placeholder(R.drawable.default_profile)
                            .into(imgProfile)
                    }
                }
            }
    }


    private fun setupClickListeners() {
        btnEditProfile.setOnClickListener { toggleSection(sectionEditProfile) }
        btnPhone.setOnClickListener { toggleSection(sectionPhone) }
        btnChangePassword.setOnClickListener { toggleSection(sectionPassword) }
        btnInbox.setOnClickListener { toggleSection(sectionInbox) }

        btnLogout.setOnClickListener { logoutUser() }

        btnSaveProfile.setOnClickListener { saveProfile() }
        btnSavePhone.setOnClickListener { savePhone() }
        btnSavePassword.setOnClickListener { updatePassword() }
    }

    private fun toggleSection(selected: LinearLayout) {
        val all = listOf(sectionEditProfile, sectionPhone, sectionPassword, sectionInbox)

        all.forEach { section ->
            section.visibility =
                if (section == selected && section.visibility == View.GONE)
                    View.VISIBLE else View.GONE
        }
    }


    private fun saveProfile() {
        val name = edtName.text.toString().trim()
        val user = auth.currentUser ?: return

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(user.uid)
            .update("name", name)
            .addOnSuccessListener {
                tvName.text = name
                Toast.makeText(requireContext(), "Profile Updated", Toast.LENGTH_SHORT).show()
            }
    }


    private fun savePhone() {
        val phone = edtPhone.text.toString().trim()
        val user = auth.currentUser ?: return

        if (phone.isEmpty()) {
            Toast.makeText(requireContext(), "Phone cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(user.uid)
            .update("phone", phone)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Phone Updated", Toast.LENGTH_SHORT).show()
            }
    }


    private fun updatePassword() {
        val newPass = edtPassword.text.toString().trim()
        val user = auth.currentUser ?: return

        if (newPass.length < 6) {
            Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        user.updatePassword(newPass)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Password Updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun logoutUser() {
        auth.signOut()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    // ========= IMAGE PICKER ==============
    private fun chooseImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data

            if (imageUri != null) {
                imgProfile.setImageURI(imageUri)
                uploadProfileImage(imageUri!!)   // FIXED
            }
        }
    }

    // ========== UPLOAD IMAGE TO FIREBASE STORAGE ==========
    private fun uploadProfileImage(uri: Uri) {
        val user = auth.currentUser ?: return

        val ref = storage.reference.child("profileImages/${user.uid}.jpg")

        ref.putFile(uri)
            .addOnSuccessListener {

                ref.downloadUrl.addOnSuccessListener { downloadUri: Uri ->   // FIXED TYPE
                    val url = downloadUri.toString()

                    db.collection("users").document(user.uid)
                        .update("profileImage", url)
                        .addOnSuccessListener {

                            Glide.with(requireContext())
                                .load(url)
                                .placeholder(R.drawable.default_profile)
                                .into(imgProfile)

                            Toast.makeText(requireContext(), "Profile photo updated", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
