package io.novafoundation.nova.feature_push_notifications.data.presentation.settings.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_push_notifications.data.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.data.domain.interactor.PushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.data.presentation.settings.PushSettingsViewModel

@Module(includes = [ViewModelModule::class])
class PushSettingsModule {

    @Provides
    @IntoMap
    @ViewModelKey(PushSettingsViewModel::class)
    fun provideViewModel(
        router: PushNotificationsRouter,
        interactor: PushNotificationsInteractor,
        resourceManager: ResourceManager
    ): ViewModel {
        return PushSettingsViewModel(
            router,
            interactor,
            resourceManager
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): PushSettingsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(PushSettingsViewModel::class.java)
    }
}
