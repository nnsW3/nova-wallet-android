package io.novafoundation.nova.feature_staking_impl.domain.validations.rewardDestination

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.validations.AccountRequiredValidation
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughAmountToTransferValidation

typealias RewardDestinationFeeValidation = EnoughAmountToTransferValidation<RewardDestinationValidationPayload, RewardDestinationValidationFailure>
typealias RewardDestinationControllerRequiredValidation = AccountRequiredValidation<RewardDestinationValidationPayload, RewardDestinationValidationFailure>

typealias RewardDestinationValidationSystem = ValidationSystem<RewardDestinationValidationPayload, RewardDestinationValidationFailure>
