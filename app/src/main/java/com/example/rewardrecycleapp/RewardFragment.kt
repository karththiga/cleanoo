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

    fun getEarnedPoints(weightInKg: Double, isRecyclable: Boolean): Int {
        val wasteType = if (isRecyclable) {
            RewardCalculator.WasteType.RECYCLABLE
        } else {
            RewardCalculator.WasteType.NON_RECYCLABLE
        }

        return RewardCalculator.calculateRewardPoints(weightInKg, wasteType)
    }
}
