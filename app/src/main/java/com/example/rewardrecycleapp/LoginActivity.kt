package com.example.rewardrecycleapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.rewardrecycleapp.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        binding.etEmail.addTextChangedListener { editable ->
            val isCollector = editable?.toString()?.trim() == "abcd"
            binding.tvSignup.visibility = if (isCollector) View.GONE else View.VISIBLE
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pwd = binding.etPassword.text.toString().trim()

            if (email.isEmpty()) {
                binding.etEmail.error = "Required"
                return@setOnClickListener
            }

            if (pwd.isEmpty()) {
                binding.etPassword.error = "Required"
                return@setOnClickListener
            }

            if (email == "abcd" && pwd == "123") {
                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, CollectorDashboardActivity::class.java))
                finish()
            } else {
                loginUser(email, pwd)
            }
        }
    }

    private fun loginUser(email: String, pwd: String) {
        auth.signInWithEmailAndPassword(email, pwd)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user == null) {
                    Toast.makeText(this, "User not found", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                user.getIdToken(true)
                    .addOnSuccessListener { tokenResult ->
                        val idToken = tokenResult.token
                        if (idToken.isNullOrEmpty()) {
                            Toast.makeText(this, "Failed to get auth token", Toast.LENGTH_LONG).show()
                        } else {

                        MobileBackendApi.getMyHouseholdProfile(idToken) { success, profile, message ->
                            runOnUiThread {
                                if (!success || profile == null) {
                                    Toast.makeText(this, message ?: "Profile not found", Toast.LENGTH_LONG).show()
                                    return@runOnUiThread
                                }

                                val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
                                prefs.edit()
                                    .putString("ID_TOKEN", idToken)
                                    .putString("HOUSEHOLD_ID", profile.optString("_id"))
                                    .putString("FIREBASE_UID", user.uid)
                                    .putString("HOUSEHOLD_NAME", profile.optString("name"))
                                    .putString("HOUSEHOLD_EMAIL", profile.optString("email"))
                                    .putString("HOUSEHOLD_PHONE", profile.optString("phone"))
                                    .putString("HOUSEHOLD_ADDRESS", profile.optString("address"))
                                    .putString("HOUSEHOLD_ZONE", profile.optString("zone"))
                                    .putString("HOUSEHOLD_POINTS", profile.optInt("points", 0).toString())
                                    .apply()

                                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, DashboardActivity::class.java))
                                finish()
                            }
                        }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, it.localizedMessage, Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, it.localizedMessage, Toast.LENGTH_LONG).show()
            }
    }
}
