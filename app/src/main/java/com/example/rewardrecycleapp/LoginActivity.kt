package com.example.rewardrecycleapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.rewardrecycleapp.databinding.ActivityLoginBinding
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private val blockedMessage = "admin blocked you.if you need further information contact municipal council"

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private var selectedRole: UserRole = UserRole.HOUSEHOLD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        disableAutofillAndSuggestions()
        preventInitialInputFocus()

        auth = FirebaseAuth.getInstance()

        bindRoleTabs()

        binding.tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
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

            loginUser(email, pwd)
        }
    }


    private fun disableAutofillAndSuggestions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            binding.etEmail.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            binding.etPassword.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        }
    }


    private fun preventInitialInputFocus() {
        binding.root.isFocusableInTouchMode = true
        binding.root.requestFocus()
    }

    private fun bindRoleTabs() {
        binding.tabRole.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedRole = if (tab?.position == 1) UserRole.COLLECTOR else UserRole.HOUSEHOLD
                binding.tvSignup.visibility = if (selectedRole == UserRole.HOUSEHOLD) View.VISIBLE else View.GONE
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })
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
                            return@addOnSuccessListener
                        }

                        if (selectedRole == UserRole.COLLECTOR) {
                            loginCollector(idToken, user.uid)
                        } else {
                            loginHousehold(idToken, user.uid)
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

    private fun loginHousehold(idToken: String, firebaseUid: String) {
        MobileBackendApi.getMyHouseholdProfile(idToken) { success, profile, message ->
            runOnUiThread {
                if (!success || profile == null) {
                    if (handleBlockedLoginIfNeeded(message, null)) {
                        return@runOnUiThread
                    }
                    Toast.makeText(this, message ?: "Household profile not found", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                if (handleBlockedLoginIfNeeded(null, profile.optString("status"))) {
                    return@runOnUiThread
                }

                getSharedPreferences("auth_prefs", MODE_PRIVATE).edit()
                    .putString("USER_ROLE", UserRole.HOUSEHOLD.value)
                    .putString("ID_TOKEN", idToken)
                    .putString("FIREBASE_UID", firebaseUid)
                    .putString("HOUSEHOLD_ID", profile.optString("_id"))
                    .putString("HOUSEHOLD_NAME", profile.optString("name"))
                    .putString("HOUSEHOLD_EMAIL", profile.optString("email"))
                    .putString("HOUSEHOLD_PHONE", profile.optString("phone"))
                    .putString("HOUSEHOLD_ADDRESS", profile.optString("address"))
                    .putString("HOUSEHOLD_ZONE", profile.optString("zone"))
                    .putString("HOUSEHOLD_POINTS", profile.optInt("points", 0).toString())
                    .remove("COLLECTOR_ID")
                    .remove("COLLECTOR_EMAIL")
                    .remove("COLLECTOR_NAME")
                    .remove("COLLECTOR_PHONE")
                    .remove("COLLECTOR_ZONE")
                    .apply()

                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }
        }
    }

    private fun loginCollector(idToken: String, firebaseUid: String) {
        MobileBackendApi.getMyCollectorProfile(idToken) { success, profile, message ->
            runOnUiThread {
                if (!success || profile == null) {
                    if (handleBlockedLoginIfNeeded(message, null)) {
                        return@runOnUiThread
                    }
                    Toast.makeText(this, message ?: "Collector profile not found", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                if (handleBlockedLoginIfNeeded(null, profile.optString("status"))) {
                    return@runOnUiThread
                }

                getSharedPreferences("auth_prefs", MODE_PRIVATE).edit()
                    .putString("USER_ROLE", UserRole.COLLECTOR.value)
                    .putString("ID_TOKEN", idToken)
                    .putString("FIREBASE_UID", firebaseUid)
                    .putString("COLLECTOR_ID", profile.optString("_id"))
                    .putString("COLLECTOR_EMAIL", profile.optString("email"))
                    .putString("COLLECTOR_NAME", profile.optString("name"))
                    .putString("COLLECTOR_PHONE", profile.optString("phone"))
                    .putString("COLLECTOR_ZONE", profile.optString("zone"))
                    .remove("HOUSEHOLD_ID")
                    .remove("HOUSEHOLD_NAME")
                    .remove("HOUSEHOLD_EMAIL")
                    .remove("HOUSEHOLD_PHONE")
                    .remove("HOUSEHOLD_ADDRESS")
                    .remove("HOUSEHOLD_ZONE")
                    .remove("HOUSEHOLD_POINTS")
                    .apply()

                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, CollectorDashboardActivity::class.java))
                finish()
            }
        }
    }


    private fun handleBlockedLoginIfNeeded(message: String?, profileStatus: String?): Boolean {
        val isBlockedMessage = message?.trim()?.equals(blockedMessage, ignoreCase = true) == true
        val isBlockedStatus = profileStatus?.trim()?.equals("blocked", ignoreCase = true) == true

        if (!isBlockedMessage && !isBlockedStatus) {
            return false
        }

        auth.signOut()
        getSharedPreferences("auth_prefs", MODE_PRIVATE).edit().clear().apply()
        Toast.makeText(this, blockedMessage, Toast.LENGTH_LONG).show()
        return true
    }

    enum class UserRole(val value: String) {
        HOUSEHOLD("household"),
        COLLECTOR("collector")
    }
}
