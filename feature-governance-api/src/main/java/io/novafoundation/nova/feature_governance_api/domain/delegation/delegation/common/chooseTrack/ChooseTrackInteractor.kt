package io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack

import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.common.chooseTrack.model.ChooseTrackData
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow

interface ChooseTrackInteractor {

    suspend fun isAllowedToShowRemoveVotesSuggestion(): Boolean

    suspend fun disallowShowRemoveVotesSuggestion()

    fun observeTracksByChain(chainId: ChainId, govType: Chain.Governance): Flow<ChooseTrackData>

    fun observeNewDelegationTrackData(): Flow<ChooseTrackData>

    fun observeEditDelegationTrackData(delegateId: AccountId): Flow<ChooseTrackData>

    fun observeRevokeDelegationTrackData(delegateId: AccountId): Flow<ChooseTrackData>
}
