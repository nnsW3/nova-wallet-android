package io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.base.showError
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.ConfirmationDialogInfo
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.navigation.awaitResponse
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.progress.ProgressDialogMixin
import io.novafoundation.nova.common.utils.progress.startProgress
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.common.view.input.selector.ListSelectorMixin
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.ChangeBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.ChangeBackupPasswordRequester
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.RestoreBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.RestoreBackupPasswordRequester
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordRequester
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupAuthFailed
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupNotFound
import io.novafoundation.nova.feature_cloud_backup_api.presenter.action.launchDeleteBackupAction
import io.novafoundation.nova.feature_cloud_backup_api.presenter.confirmation.awaitDeleteBackupConfirmation
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.FetchBackupError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.InvalidBackupPasswordError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.PasswordNotSaved
import io.novafoundation.nova.feature_cloud_backup_api.presenter.action.launchDeprecatedPasswordAction
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.mapCloudBackupSyncFailed
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.mapDeleteBackupFailureToUi
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.domain.CloudBackupSettingsInteractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class BackupSettingsViewModel(
    private val resourceManager: ResourceManager,
    private val router: SettingsRouter,
    private val cloudBackupSettingsInteractor: CloudBackupSettingsInteractor,
    private val syncWalletsBackupPasswordCommunicator: SyncWalletsBackupPasswordCommunicator,
    private val changeBackupPasswordCommunicator: ChangeBackupPasswordCommunicator,
    private val restoreBackupPasswordCommunicator: RestoreBackupPasswordCommunicator,
    private val actionBottomSheetLauncher: ActionBottomSheetLauncher,
    val progressDialogMixin: ProgressDialogMixin,
    actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    listSelectorMixinFactory: ListSelectorMixin.Factory
) : BaseViewModel(), ActionBottomSheetLauncher by actionBottomSheetLauncher {

    val confirmationAwaitableAction = actionAwaitableMixinFactory.confirmingAction<ConfirmationDialogInfo>()

    val listSelectorMixin = listSelectorMixinFactory.create(viewModelScope)

    private val isSyncing = MutableStateFlow(false)

    private val syncedState = MutableStateFlow<BackupSyncOutcome>(BackupSyncOutcome.Ok)

    private val lastSync = cloudBackupSettingsInteractor.observeLastSyncedTime()

    val cloudBackupEnabled = MutableStateFlow(false)

    val cloudBackupStateModel: Flow<CloudBackupStateModel> = combine(
        cloudBackupEnabled,
        isSyncing,
        syncedState,
        lastSync
    ) { backupEnabled, syncingInProgress, state, lastSync ->
        mapCloudBackupStateModel(resourceManager, backupEnabled, syncingInProgress, state, lastSync)
    }

    init {
        syncCloudBackupState()
        observeRequesterResults()

        launch {
            cloudBackupEnabled.value = cloudBackupSettingsInteractor.isSyncCloudBackupEnabled()
        }
    }

    fun backClicked() {
        router.back()
    }

    fun backupSwitcherClicked() {
        launch {
            if (cloudBackupSettingsInteractor.isSyncCloudBackupEnabled()) {
                cloudBackupEnabled.value = false
                cloudBackupSettingsInteractor.setCloudBackupSyncEnabled(false)
            } else {
                cloudBackupEnabled.value = true

                syncBackupInternal(
                    onSuccess = {
                        cloudBackupSettingsInteractor.setCloudBackupSyncEnabled(true)
                    },
                    onBackupNotFound = {
                        syncWalletsBackupPasswordCommunicator.awaitResponse(SyncWalletsBackupPasswordRequester.EmptyRequest)
                        // cloudBackupSyncEnabled is set by syncWalletsBackup flow
                        cloudBackupEnabled.value = cloudBackupSettingsInteractor.isSyncCloudBackupEnabled()
                        syncedState.value = BackupSyncOutcome.Ok
                    },
                    onUnknownPassword = {
                        cloudBackupSettingsInteractor.setCloudBackupSyncEnabled(true)
                    },
                    onOtherError = {
                        cloudBackupSettingsInteractor.setCloudBackupSyncEnabled(true)
                    }
                )
            }
        }
    }

    fun manualBackupClicked() {
        TODO()
    }

    fun cloudBackupManageClicked() {
        when (syncedState.value) {
            BackupSyncOutcome.StorageAuthFailed -> return

            BackupSyncOutcome.UnknownPassword,
            BackupSyncOutcome.CorruptedBackup,
            BackupSyncOutcome.OtherStorageIssue -> {
                listSelectorMixin.showSelector(
                    R.string.manage_cloud_backup,
                    listOf(manageBackupDeleteBackupItem())
                )
            }

            else -> {
                listSelectorMixin.showSelector(
                    R.string.manage_cloud_backup,
                    listOf(manageBackupChangePasswordItem(), manageBackupDeleteBackupItem())
                )
            }
        }
    }

    fun problemButtonClicked() {
        when (syncedState.value) {
            BackupSyncOutcome.UnknownPassword -> openRestorePassword()

            else -> {}
        }
    }

    private fun syncCloudBackupState() {
        launch {
            if (cloudBackupSettingsInteractor.isSyncCloudBackupEnabled()) {
                syncBackupInternal(
                    onSuccess = {},
                    onBackupNotFound = { /* TODO: run create backup using existing password */ },
                    onUnknownPassword = {},
                    onOtherError = {}
                )
            }
        }
    }

    private fun Throwable.toEnableBackupSyncState(): BackupSyncOutcome {
        return when (this) {
            is PasswordNotSaved, is InvalidBackupPasswordError -> BackupSyncOutcome.UnknownPassword
            // not found backup is ok when we enable backup and when we start initial sync since we will create a new backup
            is FetchBackupError.BackupNotFound -> BackupSyncOutcome.Ok
            is FetchBackupError.CorruptedBackup -> BackupSyncOutcome.CorruptedBackup
            is FetchBackupError.Other -> BackupSyncOutcome.UnknownError
            is CloudBackupAuthFailed -> BackupSyncOutcome.StorageAuthFailed
            else -> BackupSyncOutcome.UnknownError
        }
    }

    private suspend fun syncBackupInternal(
        onSuccess: suspend () -> Unit,
        onBackupNotFound: suspend () -> Unit,
        onUnknownPassword: suspend () -> Unit,
        onOtherError: suspend () -> Unit,
    ) {
        isSyncing.value = true

        cloudBackupSettingsInteractor.syncCloudBackup()
            .onSuccess { syncedState.value = BackupSyncOutcome.Ok; onSuccess() }
            .onFailure { throwable ->
                syncedState.value = throwable.toEnableBackupSyncState()

                // TODO Antony: handle `PasswordNotSaved`
                when (throwable) {
                    is CloudBackupNotFound -> onBackupNotFound.invoke()
                    is InvalidBackupPasswordError, is PasswordNotSaved -> onUnknownPassword.invoke()
                    else -> onOtherError.invoke()
                }

                val titleAndMessage = mapCloudBackupSyncFailed(
                    resourceManager,
                    throwable,
                    onPasswordDeprecated = { actionBottomSheetLauncher.launchDeprecatedPasswordAction(resourceManager, ::openRestorePassword) }
                )
                titleAndMessage?.let { showError(it) }
            }

        isSyncing.value = false
    }

    private fun manageBackupChangePasswordItem(): ListSelectorMixin.Item {
        return ListSelectorMixin.Item(
            R.drawable.ic_pin,
            R.color.icon_primary,
            R.string.common_change_password,
            R.color.text_primary,
            ::onChangePasswordClicked
        )
    }

    private fun manageBackupDeleteBackupItem(): ListSelectorMixin.Item {
        return ListSelectorMixin.Item(
            R.drawable.ic_delete,
            R.color.icon_negative,
            R.string.backup_settings_delete_backup,
            R.color.text_negative,
            ::onDeleteBackupClicked
        )
    }

    private fun onChangePasswordClicked() {
        changeBackupPasswordCommunicator.openRequest(ChangeBackupPasswordRequester.EmptyRequest)
    }

    private fun onDeleteBackupClicked() {
        actionBottomSheetLauncher.launchDeleteBackupAction(resourceManager, ::confirmCloudBackupDelete)
    }

    private fun observeRequesterResults() {
        changeBackupPasswordCommunicator.responseFlow.syncBackupOnEach()
        restoreBackupPasswordCommunicator.responseFlow.syncBackupOnEach()
    }

    private fun Flow<Any>.syncBackupOnEach() {
        this.onEach {
            syncCloudBackupState()
        }
            .launchIn(this@BackupSettingsViewModel)
    }

    private fun openRestorePassword() {
        restoreBackupPasswordCommunicator.openRequest(RestoreBackupPasswordRequester.EmptyRequest)
    }

    private fun confirmCloudBackupDelete() {
        launch {
            confirmationAwaitableAction.awaitDeleteBackupConfirmation()

            progressDialogMixin.startProgress(R.string.deleting_backup_progress) {
                cloudBackupSettingsInteractor.deleteCloudBackup()
                    .onSuccess {
                        cloudBackupSettingsInteractor.setCloudBackupSyncEnabled(false)
                        cloudBackupEnabled.value = false
                    }
                    .onFailure { throwable ->
                        val titleAndMessage = mapDeleteBackupFailureToUi(resourceManager, throwable)
                        titleAndMessage?.let { showError(it) }
                    }
            }
        }
    }
}

sealed class BackupSyncOutcome {

    object Ok : BackupSyncOutcome()

    object UnknownPassword : BackupSyncOutcome()

    object DestructiveDiff : BackupSyncOutcome()

    object StorageAuthFailed : BackupSyncOutcome()

    object OtherStorageIssue : BackupSyncOutcome()

    object CorruptedBackup : BackupSyncOutcome()

    object UnknownError : BackupSyncOutcome()
}

fun BackupSyncOutcome.isError(): Boolean {
    return this != BackupSyncOutcome.Ok
}
