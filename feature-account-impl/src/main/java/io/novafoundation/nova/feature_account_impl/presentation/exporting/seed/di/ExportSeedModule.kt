package io.novafoundation.nova.feature_account_impl.presentation.exporting.seed.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.export.seed.ExportSeedInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload
import io.novafoundation.nova.feature_account_impl.presentation.exporting.seed.ExportSeedViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class ExportSeedModule {

    @Provides
    @IntoMap
    @ViewModelKey(ExportSeedViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        resourceManager: ResourceManager,
        accountInteractor: AccountInteractor,
        interactor: ExportSeedInteractor,
        chainRegistry: ChainRegistry,
        payload: ExportPayload,
    ): ViewModel {
        return ExportSeedViewModel(
            router,
            interactor,
            resourceManager,
            accountInteractor,
            chainRegistry,
            payload
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): ExportSeedViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ExportSeedViewModel::class.java)
    }
}