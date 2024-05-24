package io.novafoundation.nova.feature_ledger_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.sign.LedgerSignCommunicator
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.selectAddress.di.AddLedgerChainAccountSelectAddressComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.selectLedger.di.AddChainAccountSelectLedgerComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.SelectLedgerAddressInterScreenCommunicator
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.fillWallet.di.FillWalletImportLedgerComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.finish.di.FinishImportLedgerComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectAddress.di.SelectAddressImportLedgerComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectLedger.di.SelectLedgerImportLedgerComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.start.di.StartImportLedgerComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.sign.di.SignLedgerComponent
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        LedgerFeatureDependencies::class,
    ],
    modules = [
        LedgerFeatureModule::class,
    ]
)
@FeatureScope
interface LedgerFeatureComponent : LedgerFeatureApi {

    @Component.Factory
    interface Factory {

        fun create(
            deps: LedgerFeatureDependencies,
            @BindsInstance router: LedgerRouter,
            @BindsInstance selectLedgerAddressInterScreenCommunicator: SelectLedgerAddressInterScreenCommunicator,
            @BindsInstance signInterScreenCommunicator: LedgerSignCommunicator,
        ): LedgerFeatureComponent
    }

    fun startImportLedgerComponentFactory(): StartImportLedgerComponent.Factory
    fun fillWalletImportLedgerComponentFactory(): FillWalletImportLedgerComponent.Factory
    fun selectLedgerImportComponentFactory(): SelectLedgerImportLedgerComponent.Factory
    fun selectAddressImportLedgerComponentFactory(): SelectAddressImportLedgerComponent.Factory
    fun finishImportLedgerComponentFactory(): FinishImportLedgerComponent.Factory

    fun signLedgerComponentFactory(): SignLedgerComponent.Factory

    fun addChainAccountSelectLedgerComponentFactory(): AddChainAccountSelectLedgerComponent.Factory
    fun addChainAccountSelectAddressComponentFactory(): AddLedgerChainAccountSelectAddressComponent.Factory

    @Component(
        dependencies = [
            CommonApi::class,
            RuntimeApi::class,
            WalletFeatureApi::class,
            AccountFeatureApi::class,
            DbApi::class,
        ]
    )
    interface LedgerFeatureDependenciesComponent : LedgerFeatureDependencies
}
