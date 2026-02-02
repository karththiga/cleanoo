package com.example.rewardrecycleapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.rewardrecycleapp.databinding.ActivityCollectorDashboardBinding

class CollectorDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCollectorDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCollectorDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.collectorDashboardContainer, CollectorDashboardFragment())
                .commit()
        }
    }
}
