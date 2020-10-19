package jp.co.soramitsu.feature_wallet_impl.data.repository

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Function3
import jp.co.soramitsu.common.data.network.scale.EncodableStruct
import jp.co.soramitsu.common.utils.mapList
import jp.co.soramitsu.core_db.dao.AssetDao
import jp.co.soramitsu.core_db.dao.TransactionDao
import jp.co.soramitsu.core_db.model.TransactionLocal
import jp.co.soramitsu.core_db.model.TransactionSource
import jp.co.soramitsu.fearless_utils.encrypt.model.Keypair
import jp.co.soramitsu.fearless_utils.ss58.AddressType
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder
import jp.co.soramitsu.feature_account_api.domain.interfaces.AccountRepository
import jp.co.soramitsu.feature_account_api.domain.model.Account
import jp.co.soramitsu.feature_account_api.domain.model.Node
import jp.co.soramitsu.feature_account_api.domain.model.SigningData
import jp.co.soramitsu.feature_wallet_api.domain.interfaces.WalletRepository
import jp.co.soramitsu.feature_wallet_api.domain.model.Asset
import jp.co.soramitsu.feature_wallet_api.domain.model.Fee
import jp.co.soramitsu.feature_wallet_api.domain.model.Transaction
import jp.co.soramitsu.feature_wallet_api.domain.model.TransactionsPage
import jp.co.soramitsu.feature_wallet_api.domain.model.Transfer
import jp.co.soramitsu.feature_wallet_api.domain.model.amountFromPlanks
import jp.co.soramitsu.feature_wallet_impl.data.mappers.mapAssetLocalToAsset
import jp.co.soramitsu.feature_wallet_impl.data.mappers.mapAssetToAssetLocal
import jp.co.soramitsu.feature_wallet_impl.data.mappers.mapFeeRemoteToFee
import jp.co.soramitsu.feature_wallet_impl.data.mappers.mapTransactionLocalToTransaction
import jp.co.soramitsu.feature_wallet_impl.data.mappers.mapTransactionToTransactionLocal
import jp.co.soramitsu.feature_wallet_impl.data.mappers.mapTransferToTransaction
import jp.co.soramitsu.feature_wallet_impl.data.network.blockchain.WssSubstrateSource
import jp.co.soramitsu.feature_wallet_impl.data.network.blockchain.response.FeeRemote
import jp.co.soramitsu.feature_wallet_impl.data.network.blockchain.struct.AccountData.feeFrozen
import jp.co.soramitsu.feature_wallet_impl.data.network.blockchain.struct.AccountData.free
import jp.co.soramitsu.feature_wallet_impl.data.network.blockchain.struct.AccountData.miscFrozen
import jp.co.soramitsu.feature_wallet_impl.data.network.blockchain.struct.AccountData.reserved
import jp.co.soramitsu.feature_wallet_impl.data.network.blockchain.struct.AccountInfo
import jp.co.soramitsu.feature_wallet_impl.data.network.blockchain.struct.AccountInfo.data
import jp.co.soramitsu.feature_wallet_impl.data.network.blockchain.struct.Call
import jp.co.soramitsu.feature_wallet_impl.data.network.blockchain.struct.SignedExtrinsic
import jp.co.soramitsu.feature_wallet_impl.data.network.blockchain.struct.SubmittableExtrinsic
import jp.co.soramitsu.feature_wallet_impl.data.network.blockchain.struct.TransferArgs
import jp.co.soramitsu.feature_wallet_impl.data.network.blockchain.struct.hash
import jp.co.soramitsu.feature_wallet_impl.data.network.model.request.AssetPriceRequest
import jp.co.soramitsu.feature_wallet_impl.data.network.model.request.TransactionHistoryRequest
import jp.co.soramitsu.feature_wallet_impl.data.network.model.response.AssetPriceStatistics
import jp.co.soramitsu.feature_wallet_impl.data.network.model.response.SubscanResponse
import jp.co.soramitsu.feature_wallet_impl.data.network.model.response.TransactionHistory
import jp.co.soramitsu.feature_wallet_impl.data.network.subscan.SubscanNetworkApi
import java.math.BigDecimal
import java.util.Locale

class WalletRepositoryImpl(
    private val substrateSource: WssSubstrateSource,
    private val accountRepository: AccountRepository,
    private val assetDao: AssetDao,
    private val transactionsDao: TransactionDao,
    private val subscanApi: SubscanNetworkApi,
    private val sS58Encoder: SS58Encoder
) : WalletRepository {

    override fun observeAssets(): Observable<List<Asset>> {
        return accountRepository.observeSelectedAccount().switchMap { account ->
            assetDao.observeAssets(account.address)
        }.mapList(::mapAssetLocalToAsset)
    }

    override fun syncAssets(withoutRates: Boolean): Completable {
        return getSelectedAccount()
            .flatMapCompletable { syncAssets(it, withoutRates) }
    }

    override fun observeAsset(token: Asset.Token): Observable<Asset> {
        return accountRepository.observeSelectedAccount().switchMap { account ->
            assetDao.observeAsset(account.address, token)
        }.map(::mapAssetLocalToAsset)
    }

    override fun syncAsset(token: Asset.Token, withoutRates: Boolean): Completable {
        return syncAssets(withoutRates)
    }

    override fun observeTransactionsFirstPage(pageSize: Int): Observable<List<Transaction>> {
        return accountRepository.observeSelectedAccount()
            .switchMap { observeTransactions(it.address) }
    }

    override fun syncTransactionsFirstPage(pageSize: Int): Completable {
        return getSelectedAccount()
            .flatMapCompletable { syncTransactionsFirstPage(pageSize, it) }
    }

    override fun getTransactionPage(pageSize: Int, page: Int): Single<TransactionsPage> {
        return getSelectedAccount()
            .flatMap { getTransactionPage(pageSize, page, it) }
    }

    override fun getContacts(query: String, networkType: Node.NetworkType): Single<List<String>> {
        return transactionsDao.getContacts(query, networkType)
    }

    override fun getTransferFee(transfer: Transfer): Single<Fee> {
        return getSelectedAccount()
            .flatMap { getTransferFeeUpdatingBalance(it, transfer) }
            .map { mapFeeRemoteToFee(it, transfer.token) }
    }

    override fun performTransfer(transfer: Transfer, fee: BigDecimal): Completable {
        return getSelectedAccount().flatMap { account ->
            accountRepository.getSigningData()
                .map(this::mapSigningDataToKeypair)
                .flatMap { keys -> substrateSource.performTransfer(account, transfer, keys) }
                .map { hash -> createTransaction(hash, transfer, account.address, fee) }
                .map { transaction -> mapTransactionToTransactionLocal(transaction, account.address, TransactionSource.APP) }
        }.flatMapCompletable { transactionsDao.insert(it) }
    }

    override fun checkEnoughAmountForTransfer(transfer: Transfer): Single<Boolean> {
        return getSelectedAccount().flatMap { account ->
            substrateSource.fetchAccountInfo(account).flatMap { accountInfo ->
                getTransferFeeUpdatingBalance(account, transfer).map { fee ->
                    checkEnoughAmountForTransfer(transfer, accountInfo, fee)
                }
            }
        }
    }

    override fun listenForUpdates(account: Account): Completable {
        return substrateSource.listenForAccountUpdates(account)
            .flatMapCompletable { change ->
                updateLocalBalance(account, change.newAccountInfo)
                    .andThen(fetchTransactions(account, change.block))
            }
    }

    private fun fetchTransactions(account: Account, blockHash: String): Completable {
        return substrateSource.fetchAccountTransactionInBlock(blockHash, account)
            .mapList { submittableExtrinsic ->
                createTransactionLocal(submittableExtrinsic, account)
            }.flatMapCompletable(transactionsDao::insert)
    }

    private fun createTransaction(hash: String, transfer: Transfer, accountAddress: String, fee: BigDecimal) =
        Transaction(
            hash,
            transfer.token,
            accountAddress,
            transfer.recipient,
            transfer.amount,
            System.currentTimeMillis(),
            isIncome = false,
            fee = Fee(fee, transfer.token),
            status = Transaction.Status.PENDING
        )

    private fun getTransferFeeUpdatingBalance(account: Account, transfer: Transfer): Single<FeeRemote> {
        return substrateSource.getTransferFee(account, transfer)
            .doOnSuccess { updateLocalBalance(account, it.newAccountInfo) }
            .map { it.feeRemote }
    }

    private fun checkEnoughAmountForTransfer(
        transfer: Transfer,
        accountInfo: EncodableStruct<AccountInfo>,
        fee: FeeRemote
    ): Boolean {
        val balance = accountInfo[data][free]

        return fee.partialFee + transfer.amountInPlanks <= balance
    }

    private fun syncTransactionsFirstPage(pageSize: Int, account: Account): Completable {
        return getTransactionPage(pageSize, 0, account)
            .map { it.transactions ?: emptyList() }
            .mapList { mapTransactionToTransactionLocal(it, account.address, TransactionSource.SUBSCAN) }
            .doOnSuccess { transactionsDao.insertFromSubscan(account.address, it) }
            .ignoreElement()
    }

    private fun syncAssets(account: Account, withoutRates: Boolean): Completable {
        return if (withoutRates) {
            balanceAssetSync(account)
        } else {
            fullAssetSync(account)
        }
    }

    private fun getTransactionPage(pageSize: Int, page: Int, account: Account): Single<TransactionsPage> {
        val subDomain = subDomainFor(account.network.type)
        val request = TransactionHistoryRequest(account.address, pageSize, page)

        return getTransactionHistory(subDomain, request)
            .map {
                val transfers = it.content?.transfers

                val transactions = transfers?.map { transfer -> mapTransferToTransaction(transfer, account) }

                val withCachedFallback = transactions ?: getCachedTransactions(page, account)

                TransactionsPage(withCachedFallback)
            }
    }

    private fun getTransactionHistory(
        subDomain: String,
        request: TransactionHistoryRequest
    ): Single<SubscanResponse<TransactionHistory>> {
        return subscanApi.getTransactionHistory(subDomain, request)
            .onErrorReturnItem(SubscanResponse.createEmptyResponse())
    }

    private fun getCachedTransactions(page: Int, account: Account): List<Transaction>? {
        return if (page == 0) {
            transactionsDao.getTransactions(account.address).map(::mapTransactionLocalToTransaction)
        } else {
            null
        }
    }

    private fun fullAssetSync(account: Account): Completable {
        return zipSyncAssetRequests(account)
            .mapList { mapAssetToAssetLocal(it, account.address) }
            .flatMapCompletable(assetDao::insert)
    }

    private fun balanceAssetSync(account: Account): Completable {
        return substrateSource.fetchAccountInfo(account).flatMapCompletable { accountInfo ->
            updateLocalBalance(account, accountInfo)
        }
    }

    private fun updateLocalBalance(account: Account, accountInfo: EncodableStruct<AccountInfo>): Completable {
        return Single.fromCallable {
            listOf(createAsset(account, accountInfo, null, null))
        }
            .mapList { mapAssetToAssetLocal(it, account.address) }
            .flatMapCompletable(assetDao::insert)
    }

    private fun zipSyncAssetRequests(account: Account): Single<List<Asset>> {
        val accountInfoSingle = substrateSource.fetchAccountInfo(account)

        val networkType = account.network.type

        val currentPriceStatsSingle = getAssetPrice(networkType, AssetPriceRequest.createForNow())
        val yesterdayPriceStatsSingle = getAssetPrice(networkType, AssetPriceRequest.createForYesterday())

        return Single.zip(accountInfoSingle,
            currentPriceStatsSingle,
            yesterdayPriceStatsSingle,
            Function3<EncodableStruct<AccountInfo>, SubscanResponse<AssetPriceStatistics>, SubscanResponse<AssetPriceStatistics>, List<Asset>> { accountInfo, nowStats, yesterdayStats ->
                listOf(
                    createAsset(account, accountInfo, nowStats, yesterdayStats)
                )
            })
    }

    private fun createAsset(
        account: Account,
        accountInfo: EncodableStruct<AccountInfo>,
        todayResponse: SubscanResponse<AssetPriceStatistics>?,
        yesterdayResponse: SubscanResponse<AssetPriceStatistics>?
    ): Asset {
        val token = Asset.Token.fromNetworkType(account.network.type)

        val todayStats = todayResponse?.content
        val yesterdayStats = yesterdayResponse?.content

        val data = accountInfo[data]

        var mostRecentPrice = todayStats?.price

        if (mostRecentPrice == null) {
            val cachedAsset = assetDao.getAsset(account.address, token)

            cachedAsset?.let { mostRecentPrice = mapAssetLocalToAsset(cachedAsset).dollarRate }
        }

        val change = todayStats?.calculateRateChange(yesterdayStats)

        return Asset(
            token,
            data[free],
            data[reserved],
            data[miscFrozen],
            data[feeFrozen],
            mostRecentPrice,
            change
        )
    }

    private fun createTransactionLocal(
        extrinsic: EncodableStruct<SubmittableExtrinsic>,
        account: Account
    ): TransactionLocal {
        val hash = extrinsic.hash()

        val localCopy = transactionsDao.getTransaction(hash)

        val fee = localCopy?.feeInPlanks

        val networkType = account.network.type
        val token = Asset.Token.fromNetworkType(networkType)
        val addressType = AddressType.valueOf(networkType.toString())

        val signed = extrinsic[SubmittableExtrinsic.signedExtrinsic]
        val transferArgs = signed[SignedExtrinsic.call][Call.args]

        val senderAddress = sS58Encoder.encode(signed[SignedExtrinsic.accountId], addressType)
        val recipientAddress = sS58Encoder.encode(transferArgs[TransferArgs.recipientId], addressType)

        val amountInPlanks = transferArgs[TransferArgs.amount]

        return TransactionLocal(
            hash = hash,
            accountAddress = account.address,
            senderAddress = senderAddress,
            recipientAddress = recipientAddress,
            source = TransactionSource.BLOCKCHAIN,
            status = Transaction.Status.COMPLETED,
            feeInPlanks = fee,
            token = token,
            amount = token.amountFromPlanks(amountInPlanks),
            date = System.currentTimeMillis(),
            networkType = networkType
        )
    }

    private fun observeTransactions(accountAddress: String): Observable<List<Transaction>> {
        return transactionsDao.observeTransactions(accountAddress)
            .mapList(::mapTransactionLocalToTransaction)
    }

    private fun getSelectedAccount() = accountRepository.observeSelectedAccount().firstOrError()

    private fun getAssetPrice(networkType: Node.NetworkType, request: AssetPriceRequest): Single<SubscanResponse<AssetPriceStatistics>> {
        return subscanApi.getAssetPrice(subDomainFor(networkType), request)
            .onErrorReturnItem(SubscanResponse.createEmptyResponse())
    }

    private fun mapSigningDataToKeypair(singingData: SigningData): Keypair {
        return with(singingData) {
            Keypair(
                publicKey = publicKey,
                privateKey = privateKey,
                nonce = nonce
            )
        }
    }

    private fun subDomainFor(networkType: Node.NetworkType): String {
        return networkType.readableName.toLowerCase(Locale.ROOT)
    }
}
