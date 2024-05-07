package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_impl.domain.manualBackup.ManualBackupInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.ManualBackupSelectWalletViewModel

@Module(includes = [ViewModelModule::class])
class ManualBackupSelectWalletModule {

    @Provides
    @IntoMap
    @ViewModelKey(ManualBackupSelectWalletViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        resourceManager: ResourceManager,
        manualBackupInteractor: ManualBackupInteractor,
        walletUiUseCase: WalletUiUseCase
    ): ViewModel {
        return ManualBackupSelectWalletViewModel(
            router,
            resourceManager,
            manualBackupInteractor,
            walletUiUseCase
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ManualBackupSelectWalletViewModel {
        return ViewModelProvider(
            fragment,
            viewModelFactory
        ).get(ManualBackupSelectWalletViewModel::class.java)
    }
}
