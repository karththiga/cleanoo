package com.example.rewardrecycleapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.rewardrecycleapp.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarSignup)
        binding.toolbarSignup.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        auth = FirebaseAuth.getInstance()

        binding.btnSignup.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val address = binding.etAddress.text.toString().trim()
            val pwd = binding.etPassword.text.toString().trim()
            val confirmPwd = binding.etConfirmPassword.text.toString().trim()

            if (name.isEmpty()) {
                binding.etName.error = "Required"
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                binding.etEmail.error = "Required"
                return@setOnClickListener
            }

            if (phone.isEmpty()) {
                binding.etPhone.error = "Required"
                return@setOnClickListener
            }

            if (address.isEmpty()) {
                binding.etAddress.error = "Required"
                return@setOnClickListener
            }

            if (pwd.isEmpty()) {
                binding.etPassword.error = "Required"
                return@setOnClickListener
            }

            if (pwd != confirmPwd) {
                binding.etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            createAccount(name, email, phone, address, pwd)
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun createAccount(name: String, email: String, phone: String, address: String, pwd: String) {
        auth.createUserWithEmailAndPassword(email, pwd)
            .addOnSuccessListener { result ->
                val user = result.user ?: return@addOnSuccessListener
                user.getIdToken(true)
                    .addOnSuccessListener { tokenResult ->
                        val idToken = tokenResult.token
                        if (idToken.isNullOrEmpty()) {
                            Toast.makeText(this, "Failed to get auth token", Toast.LENGTH_LONG).show()
                        } else {

                        MobileBackendApi.createHouseholdProfile(
                            idToken = idToken,
                            name = name,
                            email = email,
                            phone = phone,
                            address = address
                        ) { success, message ->
                            runOnUiThread {
                                if (success) {
                                    Toast.makeText(this, "Account created! Please login.", Toast.LENGTH_SHORT).show()
                                    auth.signOut()
                                    startActivity(Intent(this, LoginActivity::class.java))
                                    finish()
                                } else {
                                    user.delete()
                                    Toast.makeText(this, message ?: "Could not create profile", Toast.LENGTH_LONG).show()
                                }
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
