package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.createWallet

import io.novafoundation.nova.common.base.showError
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.domain.cloudBackup.createPassword.CreateCloudBackupPasswordInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.base.BackupCreatePasswordViewModel
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.mapWriteBackupFailureToUi

class CreateWalletBackupPasswordViewModel(
    router: AccountRouter,
    resourceManager: ResourceManager,
    interactor: CreateCloudBackupPasswordInteractor,
    actionBottomSheetLauncherFactory: ActionBottomSheetLauncherFactory,
    private val payload: CreateBackupPasswordPayload,
    private val accountInteractor: AccountInteractor,
) : BackupCreatePasswordViewModel(
    router,
    resourceManager,
    interactor,
    actionBottomSheetLauncherFactory
) {

    override suspend fun internalContinueClicked(password: String) {
        interactor.createAndBackupAccount(payload.walletName, password)
            .onSuccess { continueBasedOnCodeStatus() }
            .onFailure {
                val titleAndMessage = mapWriteBackupFailureToUi(resourceManager, it)
                showError(titleAndMessage)
            }
    }

    private suspend fun continueBasedOnCodeStatus() {
        if (accountInteractor.isCodeSet()) {
            router.openMain()
        } else {
            router.openCreatePincode()
        }
    }
}
