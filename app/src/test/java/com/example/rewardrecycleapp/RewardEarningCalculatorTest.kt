package com.example.rewardrecycleapp

import org.junit.Assert.assertEquals
import org.junit.Test

class RewardEarningCalculatorTest {

    @Test
    fun `returns zero for non-positive weight`() {
        assertEquals(0, RewardEarningCalculator.calculateRewardPoints(0.0, "plastic"))
        assertEquals(0, RewardEarningCalculator.calculateRewardPoints(-3.5, "paper"))
    }

    @Test
    fun `recyclable waste earns more points than non-recyclable for same weight`() {
        val recyclable = RewardEarningCalculator.calculateRewardPoints(2.0, "plastic")
        val nonRecyclable = RewardEarningCalculator.calculateRewardPoints(2.0, "mixed")

        assertEquals(36, recyclable)
        assertEquals(20, nonRecyclable)
    }

    @Test
    fun `waste type matching is case insensitive`() {
        assertEquals(
            RewardEarningCalculator.calculateRewardPoints(1.5, "PLASTIC"),
            RewardEarningCalculator.calculateRewardPoints(1.5, "plastic")
        )
    }
}
