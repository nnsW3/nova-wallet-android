package io.novafoundation.nova.feature_account_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.proxy.ProxySyncService
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.GenericLedgerAddAccountRepository
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.LegacyLedgerAddAccountRepository
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.proxied.ProxiedAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.ledger.RealGenericLedgerAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.ledger.RealLegacyLedgerAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.paritySigner.ParitySignerAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.proxied.RealProxiedAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.JsonAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.MnemonicAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.SeedAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.watchOnly.WatchOnlyAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.AccountDataSource
import io.novafoundation.nova.feature_account_impl.data.secrets.AccountSecretsFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novasama.substrate_sdk_android.encrypt.json.JsonSeedDecoder

@Module
class AddAccountsModule {

    @Provides
    @FeatureScope
    fun provideMnemonicAddAccountRepository(
        accountDataSource: AccountDataSource,
        accountSecretsFactory: AccountSecretsFactory,
        chainRegistry: ChainRegistry,
        proxySyncService: ProxySyncService,
        metaAccountChangesEventBus: MetaAccountChangesEventBus
    ) = MnemonicAddAccountRepository(
        accountDataSource,
        accountSecretsFactory,
        chainRegistry,
        proxySyncService,
        metaAccountChangesEventBus
    )

    @Provides
    @FeatureScope
    fun provideJsonAddAccountRepository(
        accountDataSource: AccountDataSource,
        accountSecretsFactory: AccountSecretsFactory,
        jsonSeedDecoder: JsonSeedDecoder,
        chainRegistry: ChainRegistry,
        proxySyncService: ProxySyncService,
        metaAccountChangesEventBus: MetaAccountChangesEventBus
    ) = JsonAddAccountRepository(
        accountDataSource,
        accountSecretsFactory,
        jsonSeedDecoder,
        chainRegistry,
        proxySyncService,
        metaAccountChangesEventBus
    )

    @Provides
    @FeatureScope
    fun provideSeedAddAccountRepository(
        accountDataSource: AccountDataSource,
        accountSecretsFactory: AccountSecretsFactory,
        chainRegistry: ChainRegistry,
        proxySyncService: ProxySyncService,
        metaAccountChangesEventBus: MetaAccountChangesEventBus
    ) = SeedAddAccountRepository(
        accountDataSource,
        accountSecretsFactory,
        chainRegistry,
        proxySyncService,
        metaAccountChangesEventBus
    )

    @Provides
    @FeatureScope
    fun provideWatchOnlyAddAccountRepository(
        accountDao: MetaAccountDao,
        proxySyncService: ProxySyncService,
        metaAccountChangesEventBus: MetaAccountChangesEventBus
    ) = WatchOnlyAddAccountRepository(
        accountDao,
        proxySyncService,
        metaAccountChangesEventBus
    )

    @Provides
    @FeatureScope
    fun provideParitySignerAddAccountRepository(
        accountDao: MetaAccountDao,
        proxySyncService: ProxySyncService,
        metaAccountChangesEventBus: MetaAccountChangesEventBus
    ) = ParitySignerAddAccountRepository(
        accountDao,
        proxySyncService,
        metaAccountChangesEventBus
    )

    @Provides
    @FeatureScope
    fun provideProxiedAddAccountRepository(
        accountDao: MetaAccountDao,
        chainRegistry: ChainRegistry
    ): ProxiedAddAccountRepository = RealProxiedAddAccountRepository(
        accountDao,
        chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideLegacyLedgerAddAccountRepository(
        accountDao: MetaAccountDao,
        chainRegistry: ChainRegistry,
        secretStoreV2: SecretStoreV2,
        proxySyncService: ProxySyncService,
        metaAccountChangesEventBus: MetaAccountChangesEventBus
    ): LegacyLedgerAddAccountRepository = RealLegacyLedgerAddAccountRepository(
        accountDao,
        chainRegistry,
        secretStoreV2,
        proxySyncService,
        metaAccountChangesEventBus
    )

    @Provides
    @FeatureScope
    fun provideGenericLedgerAddAccountRepository(
        accountDao: MetaAccountDao,
        secretStoreV2: SecretStoreV2,
        proxySyncService: ProxySyncService,
        metaAccountChangesEventBus: MetaAccountChangesEventBus
    ): GenericLedgerAddAccountRepository = RealGenericLedgerAddAccountRepository(
        accountDao,
        secretStoreV2,
        proxySyncService,
        metaAccountChangesEventBus
    )
}
