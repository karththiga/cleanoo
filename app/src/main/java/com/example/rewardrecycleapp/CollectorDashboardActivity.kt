package com.example.rewardrecycleapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.rewardrecycleapp.databinding.ActivityCollectorDashboardBinding

class CollectorDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCollectorDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCollectorDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbarBackButton()

        if (savedInstanceState == null) {
            loadFragment(CollectorDashboardFragment())
        }

        binding.collectorBottomNav.setOnItemSelectedListener { item ->
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            when (item.itemId) {
                R.id.nav_home -> loadFragment(CollectorDashboardFragment())
                R.id.nav_history -> loadFragment(CollectorHistoryFragment())
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
        setSupportActionBar(binding.toolbarCollectorDashboard)
        binding.toolbarCollectorDashboard.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        updateToolbarUi()
    }

    private fun updateToolbarUi() {
        val hasBackStack = supportFragmentManager.backStackEntryCount > 0
        supportActionBar?.setDisplayHomeAsUpEnabled(hasBackStack)
        supportActionBar?.setHomeButtonEnabled(hasBackStack)
        binding.toolbarCollectorDashboard.navigationIcon = if (hasBackStack) {
            androidx.appcompat.content.res.AppCompatResources.getDrawable(this, R.drawable.baseline_arrow_back_24)
        } else {
            null
        }

        binding.toolbarCollectorDashboard.title = currentToolbarTitle()
    }

    private fun currentToolbarTitle(): String {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.collectorDashboardContainer)
        return when (currentFragment) {
            is CollectorDashboardFragment -> "Dashboard"
            is CollectorHistoryFragment -> "Job History"
            is NotificationFragment -> "Notifications"
            is ProfileFragment -> "My Profile"
            is CollectorJobDetailFragment -> "Job Details"
            is CollectorEvidenceFragment -> "Upload Evidence"
            is CollectorReviewFragment -> "Submit Review"
            is CollectorComplaintFragment -> "Report Complaint"
            else -> getString(R.string.app_name)
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.collectorDashboardContainer, fragment)
            .commit()

        supportFragmentManager.executePendingTransactions()
        updateToolbarUi()
    }
}
