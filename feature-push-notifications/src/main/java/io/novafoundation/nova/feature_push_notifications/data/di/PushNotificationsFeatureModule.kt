package io.novafoundation.nova.feature_push_notifications.data.di

import android.content.Context
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.feature_push_notifications.data.data.GoogleApiAvailabilityProvider
import io.novafoundation.nova.feature_push_notifications.data.data.PushNotificationsService
import io.novafoundation.nova.feature_push_notifications.data.data.PushTokenCache
import io.novafoundation.nova.feature_push_notifications.data.data.RealPushNotificationsService
import io.novafoundation.nova.feature_push_notifications.data.data.RealPushTokenCache
import io.novafoundation.nova.feature_push_notifications.data.data.settings.PushSettingsProvider
import io.novafoundation.nova.feature_push_notifications.data.data.settings.RealPushSettingsProvider
import io.novafoundation.nova.feature_push_notifications.data.data.subscription.PushSubscriptionService
import io.novafoundation.nova.feature_push_notifications.data.data.subscription.RealPushSubscriptionService
import io.novafoundation.nova.feature_push_notifications.data.domain.interactor.PushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.data.domain.interactor.RealPushNotificationsInteractor
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module()
class PushNotificationsFeatureModule {

    @Provides
    @FeatureScope
    fun provideGoogleApiAvailabilityProvider(
        context: Context
    ): GoogleApiAvailabilityProvider {
        return GoogleApiAvailabilityProvider(context)
    }

    @Provides
    @FeatureScope
    fun providePushTokenCache(
        preferences: Preferences
    ): PushTokenCache {
        return RealPushTokenCache(preferences)
    }

    @Provides
    @FeatureScope
    fun providePushSettingsProvider(
        gson: Gson,
        preferences: Preferences
    ): PushSettingsProvider {
        return RealPushSettingsProvider(gson, preferences)
    }

    @Provides
    @FeatureScope
    fun providePushSubscriptionService(
        prefs: Preferences,
        chainRegistry: ChainRegistry,
        googleApiAvailabilityProvider: GoogleApiAvailabilityProvider
    ): PushSubscriptionService {
        return RealPushSubscriptionService(
            prefs,
            chainRegistry,
            googleApiAvailabilityProvider
        )
    }

    @Provides
    @FeatureScope
    fun providePushNotificationsService(
        pushSettingsProvider: PushSettingsProvider,
        pushSubscriptionService: PushSubscriptionService,
        rootScope: RootScope,
        preferences: Preferences,
        pushTokenCache: PushTokenCache,
        googleApiAvailabilityProvider: GoogleApiAvailabilityProvider
    ): PushNotificationsService {
        return RealPushNotificationsService(
            pushSettingsProvider,
            pushSubscriptionService,
            rootScope,
            preferences,
            pushTokenCache,
            googleApiAvailabilityProvider
        )
    }

    @Provides
    @FeatureScope
    fun providePushNotificationsInteractor(
        pushNotificationsService: PushNotificationsService,
        pushSettingsProvider: PushSettingsProvider
    ): PushNotificationsInteractor {
        return RealPushNotificationsInteractor(pushNotificationsService, pushSettingsProvider)
    }
}
