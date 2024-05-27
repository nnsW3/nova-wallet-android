package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.changePassword.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.ChangeBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_impl.domain.cloudBackup.createPassword.CreateCloudBackupPasswordInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.changePassword.ChangeBackupPasswordViewModel

@Module(includes = [ViewModelModule::class])
class ChangeBackupPasswordModule {

    @Provides
    @IntoMap
    @ViewModelKey(ChangeBackupPasswordViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        resourceManager: ResourceManager,
        interactor: CreateCloudBackupPasswordInteractor,
        actionBottomSheetLauncherFactory: ActionBottomSheetLauncherFactory,
        changeBackupPasswordCommunicator: ChangeBackupPasswordCommunicator,
        customDialogProvider: CustomDialogDisplayer.Presentation
    ): ViewModel {
        return ChangeBackupPasswordViewModel(
            router,
            resourceManager,
            interactor,
            actionBottomSheetLauncherFactory,
            changeBackupPasswordCommunicator,
            customDialogProvider
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ChangeBackupPasswordViewModel {
        return ViewModelProvider(
            fragment,
            viewModelFactory
        ).get(ChangeBackupPasswordViewModel::class.java)
    }
}
