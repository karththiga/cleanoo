package com.example.rewardrecycleapp

import HomeFragment
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

        if (savedInstanceState == null) {
            loadFragment(CollectorDashboardFragment())
        }

        binding.collectorBottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(CollectorDashboardFragment())
                R.id.nav_history -> loadFragment(CollectorHistoryFragment())
                R.id.nav_notification -> loadFragment(NotificationFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.collectorDashboardContainer, fragment)
            .commit()
    }
}
