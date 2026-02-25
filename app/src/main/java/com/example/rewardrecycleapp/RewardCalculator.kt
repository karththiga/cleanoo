package com.example.rewardrecycleapp

import kotlin.math.roundToInt

/**
 * Centralized reward calculation for waste submissions.
 *
 * Rules:
 * - Recyclable waste earns more than non-recyclable waste.
 * - Weight is in kilograms.
 */
object RewardCalculator {

    private const val BASE_POINTS_PER_KG = 10
    private const val RECYCLABLE_BONUS_MULTIPLIER = 1.5
    private const val NON_RECYCLABLE_MULTIPLIER = 1.0

    enum class WasteType {
        RECYCLABLE,
        NON_RECYCLABLE
    }

    fun calculateRewardPoints(weightInKg: Double, wasteType: WasteType): Int {
        if (weightInKg <= 0.0) return 0

        val multiplier = when (wasteType) {
            WasteType.RECYCLABLE -> RECYCLABLE_BONUS_MULTIPLIER
            WasteType.NON_RECYCLABLE -> NON_RECYCLABLE_MULTIPLIER
        }

        return (weightInKg * BASE_POINTS_PER_KG * multiplier).roundToInt()
    }
}
