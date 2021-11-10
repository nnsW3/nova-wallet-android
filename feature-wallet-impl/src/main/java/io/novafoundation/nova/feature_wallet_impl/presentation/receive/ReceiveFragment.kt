package io.novafoundation.nova.feature_wallet_impl.presentation.receive

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.FileProvider
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.feature_wallet_impl.R
import io.novafoundation.nova.feature_wallet_impl.di.WalletFeatureComponent
import io.novafoundation.nova.feature_wallet_impl.presentation.AssetPayload
import io.novafoundation.nova.feature_wallet_impl.presentation.receive.model.QrSharingPayload
import kotlinx.android.synthetic.main.fragment_receive.accountView
import kotlinx.android.synthetic.main.fragment_receive.novaToolbar
import kotlinx.android.synthetic.main.fragment_receive.qrImg

private const val KEY_PAYLOAD = "KEY_PAYLOAD"

class ReceiveFragment : BaseFragment<ReceiveViewModel>() {

    companion object {

        fun getBundle(assetPayload: AssetPayload) = Bundle().apply {
            putParcelable(KEY_PAYLOAD, assetPayload)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_receive, container, false)

    override fun initViews() {
        accountView.setWholeClickListener { viewModel.recipientClicked() }

        novaToolbar.setHomeButtonListener {
            viewModel.backClicked()
        }

        novaToolbar.setRightActionClickListener {
            viewModel.shareButtonClicked()
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<WalletFeatureComponent>(
            requireContext(),
            WalletFeatureApi::class.java
        )
            .receiveComponentFactory()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ReceiveViewModel) {
        setupExternalActions(viewModel)

        viewModel.qrBitmapFlow.observe {
            qrImg.setImageBitmap(it)
        }

        viewModel.accountAddressModelFlow.observe {
            accountView.setAccountIcon(it.image)
            accountView.setText(it.address)
            it.name?.let(accountView::setTitle)
        }

        viewModel.shareEvent.observeEvent(::startQrSharingIntent)
    }

    private fun startQrSharingIntent(qrSharingPayload: QrSharingPayload) {
        val imageUri = FileProvider.getUriForFile(activity!!, "${activity!!.packageName}.provider", qrSharingPayload.qrFile)

        if (imageUri != null) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, qrSharingPayload.shareMessage)
            }

            startActivity(Intent.createChooser(intent, getString(R.string.wallet_receive_description)))
        }
    }
}