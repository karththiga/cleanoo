package com.example.rewardrecycleapp

import kotlin.math.roundToInt

object RewardEarningCalculator {
    private const val BASE_POINTS_PER_KG = 10.0
    private const val RECYCLABLE_MULTIPLIER = 1.8
    private const val NON_RECYCLABLE_MULTIPLIER = 1.0

    private val recyclableWasteTypes = setOf(
        "plastic",
        "paper",
        "cardboard",
        "glass",
        "metal",
        "aluminum",
        "steel",
        "e-waste",
        "ewaste",
        "textile"
    )

    /**
     * Calculates reward points from waste weight and waste type.
     * Recyclable waste gets a higher multiplier than non-recyclable waste.
     */
    fun calculateRewardPoints(weightKg: Double, wasteType: String): Int {
        if (weightKg <= 0) return 0

        val normalizedWasteType = wasteType.trim().lowercase()
        val multiplier = if (normalizedWasteType in recyclableWasteTypes) {
            RECYCLABLE_MULTIPLIER
        } else {
            NON_RECYCLABLE_MULTIPLIER
        }

        return (weightKg * BASE_POINTS_PER_KG * multiplier)
            .roundToInt()
            .coerceAtLeast(0)
    }
}
