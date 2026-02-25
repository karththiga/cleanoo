package com.example.rewardrecycleapp

import org.junit.Assert.assertEquals
import org.junit.Test

class RewardCalculatorTest {

    @Test
    fun `calculateRewardPoints gives higher points for recyclable waste`() {
        val recyclablePoints = RewardCalculator.calculateRewardPoints(
            weightInKg = 2.0,
            wasteType = RewardCalculator.WasteType.RECYCLABLE
        )
        val nonRecyclablePoints = RewardCalculator.calculateRewardPoints(
            weightInKg = 2.0,
            wasteType = RewardCalculator.WasteType.NON_RECYCLABLE
        )

        assertEquals(30, recyclablePoints)
        assertEquals(20, nonRecyclablePoints)
    }

    @Test
    fun `calculateRewardPoints returns zero for non-positive weight`() {
        val zeroWeightPoints = RewardCalculator.calculateRewardPoints(
            weightInKg = 0.0,
            wasteType = RewardCalculator.WasteType.RECYCLABLE
        )
        val negativeWeightPoints = RewardCalculator.calculateRewardPoints(
            weightInKg = -1.0,
            wasteType = RewardCalculator.WasteType.NON_RECYCLABLE
        )

        assertEquals(0, zeroWeightPoints)
        assertEquals(0, negativeWeightPoints)
    }
}
