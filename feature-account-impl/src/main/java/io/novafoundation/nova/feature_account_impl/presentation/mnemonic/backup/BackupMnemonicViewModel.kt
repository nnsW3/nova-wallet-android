package io.novafoundation.nova.feature_account_impl.presentation.mnemonic.backup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.condition.ConditionMixinFactory
import io.novafoundation.nova.common.mixin.condition.buttonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.sendEvent
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.common.model.toAdvancedEncryptionModel
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.AdvancedEncryptionInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.export.mnemonic.ExportMnemonicInteractor
import io.novafoundation.nova.feature_account_impl.domain.common.AdvancedEncryptionSelectionStoreProvider
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.advancedEncryption.AdvancedEncryptionModePayload
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.ConfirmMnemonicPayload
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.ConfirmMnemonicPayload.CreateExtras
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.MnemonicWord
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class BackupMnemonicViewModel(
    private val resourceManager: ResourceManager,
    private val interactor: AccountInteractor,
    private val exportMnemonicInteractor: ExportMnemonicInteractor,
    private val router: AccountRouter,
    private val payload: BackupMnemonicPayload,
    private val advancedEncryptionInteractor: AdvancedEncryptionInteractor,
    private val advancedEncryptionSelectionStoreProvider: AdvancedEncryptionSelectionStoreProvider,
    private val conditionMixinFactory: ConditionMixinFactory,
) : BaseViewModel() {

    val conditionMixin = conditionMixinFactory.createConditionMixin(
        coroutineScope = viewModelScope,
        conditionsCount = 3
    )

    val buttonState = conditionMixin.buttonState(
        enabledState = resourceManager.getString(R.string.common_confirm),
        disabledState = resourceManager.getString(R.string.backup_mnemonic_disabled_button)
    ).shareInBackground()

    private val advancedEncryptionSelectionStore = async {
        advancedEncryptionSelectionStoreProvider.getSelectionStore(coroutineScope)
    }

    private val mnemonicFlow = flowOf {
        when (payload) {
            is BackupMnemonicPayload.Confirm -> exportMnemonicInteractor.getMnemonic(payload.metaAccountId, payload.chainId)
            is BackupMnemonicPayload.Create -> interactor.generateMnemonic()
        }
    }
        .inBackground()
        .share()

    private val _showMnemonicWarningDialog = MutableLiveData<Event<Unit>>()
    val showMnemonicWarningDialog: LiveData<Event<Unit>> = _showMnemonicWarningDialog

    val mnemonicDisplay = mnemonicFlow.map { mnemonic ->
        mnemonic.wordList.mapIndexed { index, word ->
            MnemonicWord(id = index, content = word, indexDisplay = index.plus(1).format(), removed = false)
        }
    }.shareInBackground()

    init {
        _showMnemonicWarningDialog.sendEvent()
    }

    fun homeButtonClicked() {
        router.back()
    }

    fun optionsClicked() {
        val advancedEncryptionPayload = when (payload) {
            is BackupMnemonicPayload.Confirm -> AdvancedEncryptionModePayload.View(payload.metaAccountId, payload.chainId)
            is BackupMnemonicPayload.Create -> AdvancedEncryptionModePayload.Change(payload.addAccountPayload)
        }

        router.openAdvancedSettings(advancedEncryptionPayload)
    }

    fun warningDeclined() {
        router.back()
    }

    fun nextClicked() = launch {
        val createExtras = (payload as? BackupMnemonicPayload.Create)?.let {
            val advancedEncryption = advancedEncryptionSelectionStore().getCurrentSelection()
                ?: advancedEncryptionInteractor.getRecommendedAdvancedEncryption()

            CreateExtras(
                accountName = it.newWalletName,
                addAccountPayload = it.addAccountPayload,
                advancedEncryptionModel = advancedEncryption.toAdvancedEncryptionModel()
            )
        }

        val payload = ConfirmMnemonicPayload(
            mnemonic = mnemonicFlow.first().wordList,
            createExtras = createExtras
        )

        router.openConfirmMnemonicOnCreate(payload)
    }
}
