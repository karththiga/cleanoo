package com.example.rewardrecycleapp

import HomeFragment
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.rewardrecycleapp.databinding.ActivityDashboardBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load Home Fragment by default
        loadFragment(HomeFragment())

        // Call backend API here
        fetchUserProfile()

        // Bottom navigation click listener
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_redeem -> loadFragment(RedeemPointsFragment())
                R.id.nav_pickups -> loadFragment(RewardFragment())
                R.id.nav_notification -> loadFragment(NotificationFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            true
        }
    }

    // ✅ MOVE THIS OUTSIDE onCreate
    private fun fetchUserProfile() {

        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val token = prefs.getString("ID_TOKEN", null)

        if (token == null) {
            runOnUiThread {
                Toast.makeText(this, "Token missing. Please login again.", Toast.LENGTH_LONG).show()
            }
            return
        }

        val client = OkHttpClient()

        val request = Request.Builder()
            .url("http://10.0.2.2:7777/api/users/profile")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@DashboardActivity,
                        "API Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                val body = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@DashboardActivity,
                            "Profile loaded ✅",
                            Toast.LENGTH_SHORT
                        ).show()
                        println("User profile: $body")
                    } else {
                        Toast.makeText(
                            this@DashboardActivity,
                            "Auth failed: ${response.code}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

        })
    }

    // Helper function to load fragments
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.dashboardContainer, fragment)
            .commit()
    }
}
