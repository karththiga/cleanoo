package com.example.rewardrecycleapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class RewardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_reward, container, false)
    }

    /**
     * Call this from your redeem/earn flow using the exact waste type selected in UI,
     * such as "Plastic", "Paper", "General Waste", etc.
     */
    fun getEarnedPoints(weightInKg: Double, wasteType: String): Int {
        return RewardCalculator.calculateRewardPoints(weightInKg, wasteType)
    }
}
