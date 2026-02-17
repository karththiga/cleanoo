package com.example.rewardrecycleapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    private lateinit var imgProfile: CircleImageView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView

    private lateinit var edtName: EditText
    private lateinit var edtPhone: EditText
    private lateinit var edtAddress: EditText
    private lateinit var edtZone: EditText
    private lateinit var btnSaveProfile: Button

    private lateinit var edtPassword: EditText
    private lateinit var btnSavePassword: Button
    private lateinit var btnLogout: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        auth = FirebaseAuth.getInstance()

        bindViews(view)
        loadProfileFromPrefs()
        refreshProfileFromApi()
        bindActions()

        return view
    }

    private fun bindViews(view: View) {
        imgProfile = view.findViewById(R.id.imgProfile)
        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)

        edtName = view.findViewById(R.id.edtName)
        edtPhone = view.findViewById(R.id.edtPhone)
        edtAddress = view.findViewById(R.id.edtAddress)
        edtZone = view.findViewById(R.id.edtZone)
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile)

        edtPassword = view.findViewById(R.id.edtPassword)
        btnSavePassword = view.findViewById(R.id.btnSavePassword)
        btnLogout = view.findViewById(R.id.btnLogout)
    }

    private fun bindActions() {
        btnSaveProfile.setOnClickListener { updateProfile() }
        btnSavePassword.setOnClickListener { updatePassword() }
        btnLogout.setOnClickListener { logoutUser() }
    }

    private fun loadProfileFromPrefs() {
        val prefs = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val name = prefs.getString("HOUSEHOLD_NAME", "User") ?: "User"
        val email = prefs.getString("HOUSEHOLD_EMAIL", auth.currentUser?.email ?: "") ?: ""
        val phone = prefs.getString("HOUSEHOLD_PHONE", "") ?: ""
        val address = prefs.getString("HOUSEHOLD_ADDRESS", "") ?: ""
        val zone = prefs.getString("HOUSEHOLD_ZONE", "") ?: ""

        tvName.text = name
        tvEmail.text = email

        edtName.setText(name)
        edtPhone.setText(phone)
        edtAddress.setText(address)
        edtZone.setText(zone)
    }

    private fun refreshProfileFromApi() {
        val prefs = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("ID_TOKEN", null)

        if (token.isNullOrEmpty()) return

        MobileBackendApi.getMyHouseholdProfile(token) { success, profile, message ->
            activity?.runOnUiThread {
                if (success && profile != null) {
                    applyProfile(profile)
                } else if (!message.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun applyProfile(profile: org.json.JSONObject) {
        val name = profile.optString("name", "User")
        val email = profile.optString("email", auth.currentUser?.email ?: "")
        val phone = profile.optString("phone", "")
        val address = profile.optString("address", "")
        val zone = profile.optString("zone", "")

        tvName.text = name
        tvEmail.text = email

        edtName.setText(name)
        edtPhone.setText(phone)
        edtAddress.setText(address)
        edtZone.setText(zone)

        val prefs = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("HOUSEHOLD_ID", profile.optString("_id"))
            .putString("HOUSEHOLD_NAME", name)
            .putString("HOUSEHOLD_EMAIL", email)
            .putString("HOUSEHOLD_PHONE", phone)
            .putString("HOUSEHOLD_ADDRESS", address)
            .putString("HOUSEHOLD_ZONE", zone)
            .putString("HOUSEHOLD_POINTS", profile.optInt("points", 0).toString())
            .apply()
    }

    private fun updateProfile() {
        val prefs = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("ID_TOKEN", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Login token missing", Toast.LENGTH_SHORT).show()
            return
        }

        val name = edtName.text.toString().trim()
        val phone = edtPhone.text.toString().trim()
        val address = edtAddress.text.toString().trim()
        val zone = edtZone.text.toString().trim()

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            Toast.makeText(requireContext(), "Name, phone and address are required", Toast.LENGTH_SHORT).show()
            return
        }

        MobileBackendApi.updateMyHouseholdProfile(token, name, phone, address, zone) { success, profile, message ->
            activity?.runOnUiThread {
                if (success && profile != null) {
                    applyProfile(profile)
                    Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), message ?: "Profile update failed", Toast.LENGTH_LONG).show()
                }
            }
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
                edtPassword.setText("")
                Toast.makeText(requireContext(), "Password updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logoutUser() {
        auth.signOut()
        requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
