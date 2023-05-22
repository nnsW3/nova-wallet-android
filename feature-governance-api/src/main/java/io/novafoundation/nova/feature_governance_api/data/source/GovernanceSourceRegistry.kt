package io.novafoundation.nova.feature_governance_api.data.source

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.AssetSharedStateAdditionalData
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState

interface GovernanceSourceRegistry {

    suspend fun sourceFor(option: SupportedGovernanceOption): GovernanceSource
}

typealias SupportedGovernanceOption = SelectedAssetOptionSharedState.SupportedAssetOption<GovernanceAdditionalState>

interface GovernanceAdditionalState : AssetSharedStateAdditionalData {

    val governanceType: Chain.Governance
}
