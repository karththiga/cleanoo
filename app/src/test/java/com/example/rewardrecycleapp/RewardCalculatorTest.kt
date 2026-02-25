package com.example.rewardrecycleapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardCalculatorTest {

    @Test
    fun `calculateRewardPoints gives higher points for recyclable than non-recyclable`() {
        val recyclablePoints = RewardCalculator.calculateRewardPoints(
            weightInKg = 2.0,
            wasteType = RewardCalculator.WasteType.PLASTIC
        )
        val nonRecyclablePoints = RewardCalculator.calculateRewardPoints(
            weightInKg = 2.0,
            wasteType = RewardCalculator.WasteType.GENERAL_WASTE
        )

        assertTrue(recyclablePoints > nonRecyclablePoints)
        assertEquals(32, recyclablePoints)
        assertEquals(18, nonRecyclablePoints)
    }

    @Test
    fun `calculateRewardPoints returns zero for non-positive weight`() {
        val zeroWeightPoints = RewardCalculator.calculateRewardPoints(
            weightInKg = 0.0,
            wasteType = RewardCalculator.WasteType.METAL
        )
        val negativeWeightPoints = RewardCalculator.calculateRewardPoints(
            weightInKg = -1.0,
            wasteType = RewardCalculator.WasteType.SANITARY_WASTE
        )

        assertEquals(0, zeroWeightPoints)
        assertEquals(0, negativeWeightPoints)
    }

    @Test
    fun `calculateRewardPoints supports UI string waste type input`() {
        val paperPoints = RewardCalculator.calculateRewardPoints(
            weightInKg = 1.5,
            wasteTypeInput = "Paper"
        )
        val fallbackPoints = RewardCalculator.calculateRewardPoints(
            weightInKg = 1.5,
            wasteTypeInput = "Unknown Type"
        )

        assertEquals(21, paperPoints)
        assertEquals(14, fallbackPoints)
    }
}
