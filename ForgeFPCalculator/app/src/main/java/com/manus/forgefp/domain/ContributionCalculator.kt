package com.manus.forgefp.domain

import kotlin.math.ceil
import kotlin.math.max

data class LevelInput(
    val requiredFp: Int,
    val rewards: List<Double>,
    val ownerAlreadyInvested: Int = 0,
    val multiplier: Double = 1.9,
)

data class PlacementResult(
    val placement: Int,
    val baseReward: Double,
    val donorContribution: Int,
    val previousDonorContributions: Int,
    val antiSnipeOwnerTotal: Int,
    val ownerToAdd: Int,
    val isProtected: Boolean,
    val remainingAfterOwnerAdd: Int,
)

data class CalculationResult(
    val requiredFp: Int,
    val ownerAlreadyInvested: Int,
    val multiplier: Double,
    val placements: List<PlacementResult>,
    val sequentialOwnerAdd: List<Int>,
    val totalOwnerAdd: Int,
) {
    val finalOwnerInvestment: Int get() = ownerAlreadyInvested + totalOwnerAdd
}

/**
 * Calculates the exact 1.9 thread positions and safe owner priming.
 *
 * For a position p, the anti-snipe threshold is:
 * required FP - sum(contributions for positions 1..p-1) - 2 * contribution(p).
 * It leaves at most two times the position contribution in the remaining GB,
 * so a potential sniper cannot take that place profitably and lock it.
 */
object ContributionCalculator {
    fun calculate(input: LevelInput): CalculationResult {
        require(input.requiredFp >= 0) { "The required FP must be positive." }
        require(input.ownerAlreadyInvested >= 0) { "Owner contribution cannot be negative." }
        require(input.multiplier > 0.0) { "The multiplier must be greater than zero." }

        val donorContributions = input.rewards.take(5).map { reward ->
            if (reward <= 0.0) 0 else ceil(reward * input.multiplier).toInt()
        }
        val results = donorContributions.mapIndexed { index, contribution ->
            val preceding = donorContributions.take(index).sum()
            val safetyThreshold = max(0, input.requiredFp - preceding - 2 * contribution)
            val addNow = max(0, safetyThreshold - input.ownerAlreadyInvested)
            val remaining = max(0, input.requiredFp - (input.ownerAlreadyInvested + addNow) - preceding)
            PlacementResult(
                placement = index + 1,
                baseReward = input.rewards.getOrElse(index) { 0.0 },
                donorContribution = contribution,
                previousDonorContributions = preceding,
                antiSnipeOwnerTotal = safetyThreshold,
                ownerToAdd = addNow,
                isProtected = input.ownerAlreadyInvested >= safetyThreshold,
                remainingAfterOwnerAdd = remaining,
            )
        }

        var currentOwnerFp = input.ownerAlreadyInvested
        val sequential = donorContributions.mapIndexed { index, contribution ->
            val preceding = donorContributions.take(index).sum()
            val target = max(0, input.requiredFp - preceding - 2 * contribution)
            max(0, target - currentOwnerFp).also { currentOwnerFp += it }
        }
        return CalculationResult(
            requiredFp = input.requiredFp,
            ownerAlreadyInvested = input.ownerAlreadyInvested,
            multiplier = input.multiplier,
            placements = results,
            sequentialOwnerAdd = sequential,
            totalOwnerAdd = sequential.sum(),
        )
    }
}
