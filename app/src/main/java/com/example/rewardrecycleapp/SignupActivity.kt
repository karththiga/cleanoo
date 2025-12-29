package com.example.rewardrecycleapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.rewardrecycleapp.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.btnSignup.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
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

            if (pwd.isEmpty()) {
                binding.etPassword.error = "Required"
                return@setOnClickListener
            }

            if (pwd != confirmPwd) {
                binding.etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            createAccount(name, email, pwd)
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun createAccount(name: String, email: String, pwd: String) {
        auth.createUserWithEmailAndPassword(email, pwd)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener

                val userData = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "uid" to uid,
                    "points" to 0, // reward-based app
                )

                db.collection("users")
                    .document(uid)
                    .set(userData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
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
