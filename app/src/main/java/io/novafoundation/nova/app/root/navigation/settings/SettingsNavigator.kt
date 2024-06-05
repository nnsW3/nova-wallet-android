package io.novafoundation.nova.app.root.navigation.settings

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.Navigator
import io.novafoundation.nova.feature_account_impl.presentation.pincode.PinCodeAction
import io.novafoundation.nova.feature_account_impl.presentation.pincode.PincodeFragment
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.ChainNetworkManagementFragment
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.ChainNetworkManagementPayload
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list.WalletConnectSessionsPayload

class SettingsNavigator(
    navigationHolder: NavigationHolder,
    private val walletConnectDelegate: WalletConnectRouter,
    private val delegate: Navigator
) : BaseNavigator(navigationHolder),
    SettingsRouter {

    override fun openWallets() {
        delegate.openWallets()
    }

    override fun openNetworks() {
        performNavigation(R.id.action_open_networkManagement)
    }

    override fun openNetworkDetails(payload: ChainNetworkManagementPayload) {
        performNavigation(
            R.id.action_open_networkManagementDetails,
            args = ChainNetworkManagementFragment.getBundle(payload)
        )
    }

    override fun openPushNotificationSettings() {
        performNavigation(R.id.action_open_pushNotificationsSettings)
    }

    override fun openCurrencies() = performNavigation(R.id.action_mainFragment_to_currenciesFragment)

    override fun openLanguages() = performNavigation(R.id.action_mainFragment_to_languagesFragment)

    override fun openChangePinCode() = performNavigation(
        actionId = R.id.action_change_pin_code,
        args = PincodeFragment.getPinCodeBundle(PinCodeAction.Change)
    )

    override fun openWalletDetails(metaId: Long) {
        delegate.openWalletDetails(metaId)
    }

    override fun openSwitchWallet() {
        delegate.openSwitchWallet()
    }

    override fun openWalletConnectScan() {
        walletConnectDelegate.openScanPairingQrCode()
    }

    override fun openWalletConnectSessions() {
        walletConnectDelegate.openWalletConnectSessions(WalletConnectSessionsPayload(metaId = null))
    }

    override fun openCloudBackupSettings() {
        performNavigation(R.id.action_open_cloudBackupSettings)
    }

    override fun openManualBackup() {
        performNavigation(R.id.action_open_manualBackupSelectWallet)
    }
}
