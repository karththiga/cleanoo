package com.example.rewardrecycleapp

import kotlin.math.roundToInt

/**
 * Centralized reward calculation for waste submissions.
 *
 * Reward model:
 * - Points depend on submitted weight (kg) and waste type.
 * - Recyclable waste types are intentionally rewarded with higher multipliers
 *   than non-recyclable waste types to encourage sustainable behavior.
 */
object RewardCalculator {

    private const val BASE_POINTS_PER_KG = 10

    enum class WasteCategory {
        RECYCLABLE,
        NON_RECYCLABLE
    }

    enum class WasteType(
        val category: WasteCategory,
        val multiplier: Double
    ) {
        // Recyclable
        PLASTIC(WasteCategory.RECYCLABLE, 1.6),
        PAPER(WasteCategory.RECYCLABLE, 1.4),
        GLASS(WasteCategory.RECYCLABLE, 1.3),
        METAL(WasteCategory.RECYCLABLE, 1.7),

        // Non-recyclable
        GENERAL_WASTE(WasteCategory.NON_RECYCLABLE, 0.9),
        SANITARY_WASTE(WasteCategory.NON_RECYCLABLE, 0.7),
        HAZARDOUS_WASTE(WasteCategory.NON_RECYCLABLE, 0.6);

        companion object {
            fun fromInput(type: String): WasteType {
                val normalized = type.trim().uppercase().replace(' ', '_')
                return entries.firstOrNull { it.name == normalized } ?: GENERAL_WASTE
            }
        }
    }

    fun calculateRewardPoints(weightInKg: Double, wasteType: WasteType): Int {
        if (weightInKg <= 0.0) return 0

        val rawPoints = weightInKg * BASE_POINTS_PER_KG * wasteType.multiplier
        return rawPoints.roundToInt()
    }

    fun calculateRewardPoints(weightInKg: Double, wasteTypeInput: String): Int {
        return calculateRewardPoints(weightInKg, WasteType.fromInput(wasteTypeInput))
    }
}
