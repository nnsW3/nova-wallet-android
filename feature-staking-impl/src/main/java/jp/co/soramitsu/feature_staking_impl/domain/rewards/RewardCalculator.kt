package jp.co.soramitsu.feature_staking_impl.domain.rewards

import jp.co.soramitsu.common.utils.median
import jp.co.soramitsu.common.utils.sumBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.pow

private const val PARACHAINS_ENABLED = false

private const val MINIMUM_INFLATION = 0.025
private val INFLATION_IDEAL = if (PARACHAINS_ENABLED) 0.2 else 0.1

private val STAKED_PORTION_IDEAL = if (PARACHAINS_ENABLED) 0.5 else 0.75

private const val DECAY_RATE = 0.05

private const val DAYS_IN_YEAR = 365

class RewardCalculator(
    val validators: List<RewardCalculationTarget>,
    val totalIssuance: BigInteger
) {

    private val totalStaked = validators.sumBy(RewardCalculationTarget::totalStake).toDouble()

    private val stakedPortion = totalStaked / totalIssuance.toDouble()

    private val yearlyInflation = calculateYearlyInflation()

    private val averageValidatorStake = totalStaked / validators.size

    private val averageValidatorRewardPercentage = yearlyInflation / stakedPortion

    private val apyByValidator = validators.associateBy(
        keySelector = RewardCalculationTarget::accountIdHex,
        valueTransform = ::calculateValidatorAPY
    )

    private val expectedAPY = calculateExpectedAPY()

    private fun calculateExpectedAPY(): Double {
        val medianCommission = validators.map { it.commission.toDouble() }.median()

        return averageValidatorRewardPercentage * (1 - medianCommission)
    }

    private fun calculateValidatorAPY(validator: RewardCalculationTarget): Double {
        val yearlyRewardPercentage = averageValidatorRewardPercentage * averageValidatorStake / validator.totalStake.toDouble()

        return yearlyRewardPercentage * (1 - validator.commission.toDouble())
    }

    private fun calculateYearlyInflation(): Double {
        return MINIMUM_INFLATION + if (stakedPortion in 0.0..STAKED_PORTION_IDEAL) {
            stakedPortion * (INFLATION_IDEAL - MINIMUM_INFLATION / STAKED_PORTION_IDEAL)
        } else {
            (INFLATION_IDEAL * STAKED_PORTION_IDEAL - MINIMUM_INFLATION) * 2.0.pow((STAKED_PORTION_IDEAL - stakedPortion) / DECAY_RATE)
        }
    }

    fun getApyFor(targetIdHex: String): BigDecimal {
        return apyByValidator[targetIdHex]?.toBigDecimal() ?: error("Validator $targetIdHex was not found")
    }

    suspend fun calculateReturns(
        amount: BigDecimal,
        days: Int,
        isCompound: Boolean
    ): BigDecimal = withContext(Dispatchers.Default) {
        val dailyPercentage = expectedAPY / DAYS_IN_YEAR

        calculateReward(amount.toDouble(), days, dailyPercentage, isCompound).toBigDecimal()
    }

    suspend fun calculateReturns(
        amount: Double,
        days: Int,
        isCompound: Boolean,
        targetIdHex: String
    ): BigDecimal = withContext(Dispatchers.Default) {
        val validatorAPY = apyByValidator[targetIdHex] ?: error("Validator with $targetIdHex was not found")
        val dailyPercentage = validatorAPY / DAYS_IN_YEAR

        calculateReward(amount, days, dailyPercentage, isCompound).toBigDecimal()
    }

    private fun calculateReward(
        amount: Double,
        days: Int,
        dailyPercentage: Double,
        isCompound: Boolean
    ) = if (isCompound) {
        calculateCompoundReward(amount, days, dailyPercentage)
    } else {
        calculateSimpleReward(amount, days, dailyPercentage)
    }

    private fun calculateSimpleReward(amount: Double, days: Int, dailyPercentage: Double): Double {
        return amount * dailyPercentage * days
    }

    private fun calculateCompoundReward(amount: Double, days: Int, dailyPercentage: Double): Double {
        return amount * ((1 + dailyPercentage).pow(days))
    }
}