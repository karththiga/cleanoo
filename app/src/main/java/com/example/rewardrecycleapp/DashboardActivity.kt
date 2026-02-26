package com.example.rewardrecycleapp

import HomeFragment
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.rewardrecycleapp.databinding.ActivityDashboardBinding

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbarBackButton()

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
        fetchUserProfile()

        binding.bottomNav.setOnItemSelectedListener { item ->
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_redeem -> loadFragment(RedeemPointsFragment())
                R.id.nav_pickups -> loadFragment(PickupFragment())
                R.id.nav_notification -> loadFragment(NotificationFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            updateToolbarUi()
            true
        }

        supportFragmentManager.addOnBackStackChangedListener {
            updateToolbarUi()
        }

        updateToolbarUi()
    }

    private fun setupToolbarBackButton() {
        setSupportActionBar(binding.toolbarDashboard)
        binding.toolbarDashboard.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        updateToolbarUi()
    }

    private fun updateToolbarUi() {
        val hasBackStack = supportFragmentManager.backStackEntryCount > 0
        supportActionBar?.setDisplayHomeAsUpEnabled(hasBackStack)
        supportActionBar?.setHomeButtonEnabled(hasBackStack)
        binding.toolbarDashboard.navigationIcon = if (hasBackStack) {
            androidx.appcompat.content.res.AppCompatResources.getDrawable(this, R.drawable.baseline_arrow_back_24)
        } else {
            null
        }

        binding.toolbarDashboard.title = currentToolbarTitle()
    }

    private fun currentToolbarTitle(): String {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.dashboardContainer)
        return when (currentFragment) {
            is HomeFragment -> householdGreetingTitle()
            is PickupFragment -> "Pickups"
            is RedeemPointsFragment -> "Redeem Points"
            is RewardsWalletFragment -> "Rewards Wallet"
            is NotificationFragment -> "Notifications"
            is ProfileFragment -> "My Profile"
            is HouseholdJobDetailFragment -> "Job Details"
            is HouseholdReviewFragment -> "Submit Review"
            is HouseholdComplaintFragment -> "Report Issue"
            else -> "Dashboard"
        }
    }

    private fun householdGreetingTitle(): String {
        val name = getSharedPreferences("auth_prefs", MODE_PRIVATE)
            .getString("HOUSEHOLD_NAME", "there")
            ?.trim()
            .orEmpty()
        val firstName = name.split(" ").firstOrNull().orEmpty()
        return if (firstName.isBlank()) "Hi there 👋" else "Hi $firstName 👋"
    }

    private fun fetchUserProfile() {
        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val token = prefs.getString("ID_TOKEN", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Token missing. Please login again.", Toast.LENGTH_LONG).show()
            return
        }

        MobileBackendApi.getMyHouseholdProfile(token) { success, profile, message ->
            runOnUiThread {
                if (success && profile != null) {
                    prefs.edit()
                        .putString("HOUSEHOLD_ID", profile.optString("_id"))
                        .putString("HOUSEHOLD_NAME", profile.optString("name"))
                        .putString("HOUSEHOLD_EMAIL", profile.optString("email"))
                        .putString("HOUSEHOLD_PHONE", profile.optString("phone"))
                        .putString("HOUSEHOLD_ADDRESS", profile.optString("address"))
                        .putString("HOUSEHOLD_ZONE", profile.optString("zone"))
                        .putString("HOUSEHOLD_POINTS", profile.optInt("points", 0).toString())
                        .apply()
                    updateToolbarUi()
                } else {
                    Toast.makeText(this, message ?: "Failed to load profile", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.dashboardContainer, fragment)
            .commit()

        supportFragmentManager.executePendingTransactions()
        updateToolbarUi()
    }
}
