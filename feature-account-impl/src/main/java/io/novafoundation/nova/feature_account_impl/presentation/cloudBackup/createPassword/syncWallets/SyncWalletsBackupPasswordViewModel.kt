package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.syncWallets

import io.novafoundation.nova.common.base.showError
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordResponder
import io.novafoundation.nova.feature_account_impl.domain.cloudBackup.createPassword.CreateCloudBackupPasswordInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.base.BackupCreatePasswordViewModel
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.mapWriteBackupFailureToUi

class SyncWalletsBackupPasswordViewModel(
    router: AccountRouter,
    resourceManager: ResourceManager,
    interactor: CreateCloudBackupPasswordInteractor,
    actionBottomSheetLauncherFactory: ActionBottomSheetLauncherFactory,
    private val syncWalletsBackupPasswordCommunicator: SyncWalletsBackupPasswordCommunicator
) : BackupCreatePasswordViewModel(
    router,
    resourceManager,
    interactor,
    actionBottomSheetLauncherFactory
) {

    override suspend fun internalContinueClicked(password: String) {
        interactor.uploadInitialBackup(password)
            .onSuccess {
                syncWalletsBackupPasswordCommunicator.respond(SyncWalletsBackupPasswordResponder.Response(isSyncingSuccessful = true))
                router.back()
            }.onFailure { throwable ->
                val titleAndMessage = mapWriteBackupFailureToUi(resourceManager, throwable)
                showError(titleAndMessage)
            }
    }

    override fun backClicked() {
        syncWalletsBackupPasswordCommunicator.respond(SyncWalletsBackupPasswordResponder.Response(isSyncingSuccessful = false))
        super.backClicked()
    }
}
