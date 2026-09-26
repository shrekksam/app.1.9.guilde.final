package com.manus.forgefp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContributionCalculatorTest {
    @Test
    fun `calculates rounded 1 point 9 contributions`() {
        val result = ContributionCalculator.calculate(
            LevelInput(requiredFp = 1_000, rewards = listOf(100.0, 50.0, 10.5, 0.0, 0.0))
        )

        assertEquals(listOf(190, 95, 20, 0, 0), result.placements.map { it.donorContribution })
    }

    @Test
    fun `leaves at most twice the donor contribution after each safety threshold`() {
        val result = ContributionCalculator.calculate(
            LevelInput(requiredFp = 5_464, rewards = listOf(1_375.0, 690.0, 230.0, 60.0, 10.0))
        )

        assertEquals(listOf(2_613, 1_311, 437, 114, 19), result.placements.map { it.donorContribution })
        assertEquals(listOf(238, 229, 666, 875, 951), result.placements.map { it.antiSnipeOwnerTotal })
        result.placements.forEach { position ->
            assertTrue(position.remainingAfterOwnerAdd <= 2 * position.donorContribution)
        }
    }

    @Test
    fun `does not request FP below an already met owner threshold`() {
        val result = ContributionCalculator.calculate(
            LevelInput(requiredFp = 1_000, rewards = listOf(100.0, 50.0, 10.0, 0.0, 0.0), ownerAlreadyInvested = 700)
        )

        assertTrue(result.placements.all { it.ownerToAdd == 0 })
        assertEquals(0, result.totalOwnerAdd)
    }

    @Test
    fun `sequential additions protect each stage`() {
        val result = ContributionCalculator.calculate(
            LevelInput(requiredFp = 1_000, rewards = listOf(100.0, 50.0, 10.0, 0.0, 0.0))
        )

        assertEquals(listOf(620, 0, 57, 19, 0), result.sequentialOwnerAdd)
        assertEquals(696, result.totalOwnerAdd)
    }
}
