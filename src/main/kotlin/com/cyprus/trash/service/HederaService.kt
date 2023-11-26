package com.cyprus.trash.service

import com.cyprus.trash.model.Transactionable
import com.google.gson.Gson
import com.hedera.hashgraph.sdk.*
import io.github.cdimascio.dotenv.Dotenv
import org.springframework.stereotype.Service
import java.util.concurrent.TimeoutException


@Service
class HederaService {
    val client: Client by lazy { createHederaNftClient() }

    private fun createHederaNftClient(): Client {
        val dotenv = Dotenv.load()
        val myAccountId = AccountId.fromString(dotenv["ACCOUNT_ID"]!!)
        val myPrivateKey = PrivateKey.fromString(dotenv["ACCOUNT_DER_PRIVATE_KEY"]!!)

        // Create your Hedera testnet client
        val client = Client.forTestnet()
        client.setOperator(myAccountId, myPrivateKey)

        return client
    }

    data class AccountInfo(val accountId: AccountId, val key: PrivateKey) {
        companion object {
            fun fromTransactionable(transactionable: Transactionable): AccountInfo {
                return AccountInfo(
                    AccountId.fromString(transactionable.cryptoId),
                    PrivateKey.fromString(transactionable.cryptoPrivateKey)
                )
            }
        }
    }

    enum class TransactionResult { Ok, Error }

    class TransactionableImpl(override val cryptoId: String, override val cryptoPrivateKey: String) : Transactionable

    /**
     * Creates a new account with an optional initial balance.
     *
     * @param initialBalance The initial balance of the new account. Defaults to 10 if not specified.
     * @return An instance of [Transactionable] containing the crypto ID and private key of the newly created account,
     *         or null if the account creation fails.
     * @throws ReceiptStatusException if there is an issue with the transaction receipt.
     * @throws TimeoutException if the transaction execution times out.
     * @throws PrecheckStatusException if the transaction fails the pre-check validations.
     */
    @Throws(ReceiptStatusException::class, TimeoutException::class, PrecheckStatusException::class)
    fun createNewAccount(initialBalance: Long = 10): Transactionable? {
        val accountInfo = createNewAccountImpl(Hbar(initialBalance)) ?: return null
        return TransactionableImpl(accountInfo.accountId.toString(), accountInfo.key.toString())
    }

    @Throws(ReceiptStatusException::class, TimeoutException::class, PrecheckStatusException::class)
    private fun createNewAccountImpl(initialBalance: Hbar = Hbar.ZERO): AccountInfo? {
        val accountPrivateKey = PrivateKey.generateED25519()
        val account = AccountCreateTransaction()
            .setKey(accountPrivateKey.publicKey)
            .setInitialBalance(initialBalance)
            .execute(client)

        val accountId = account.getReceipt(client).accountId
        println("New account created with ID $accountId")

        if (null == accountId) return null
        return AccountInfo(accountId, accountPrivateKey)
    }

    fun deleteAccount(account: Transactionable): TransactionResult {
        val accountInfo =
            AccountInfo(AccountId.fromString(account.cryptoId), PrivateKey.fromString(account.cryptoPrivateKey))
        deleteAccount(accountInfo)
        return TransactionResult.Ok
    }

    private fun deleteAccount(account: AccountInfo): Status {
        val operatorAccountId = client.operatorAccountId ?: throw RuntimeException("Cannot find operator account")

        return AccountDeleteTransaction()
            .setAccountId(account.accountId)
            .setTransferAccountId(operatorAccountId)
            .freezeWith(client)
            .sign(account.key)
            .execute(client)
            .getReceipt(client)
            .status
    }

    /**
     * Retrieves the account balance for a given transactionable.
     *
     * @param transactionable The transactionable object with cryptoId and cryptoPrivateKey properties.
     * @return The account balance as a Long value.
     */
    fun getAccountBalance(transactionable: Transactionable): Long {
        return getAccountBalance(AccountId.fromString(transactionable.cryptoId)).value.toLong()
    }

    private fun getAccountBalance(accountId: AccountId): Hbar {
        return AccountBalanceQuery()
            .setAccountId(accountId)
            .execute(client).hbars
    }

    /**
     * Tops up the HBARs in the given [transactionable] account with the specified [amount].
     *
     * @param transactionable the account to top up with HBARs
     * @param amount the amount of HBARs to top up
     * @return the result of the transaction, which can be [TransactionResult.Ok] if the top-up is successful
     */
    fun topUpHBARs(transactionable: Transactionable, amount: Long): TransactionResult {
        val accountInfo = AccountInfo(
            AccountId.fromString(transactionable.cryptoId),
            PrivateKey.fromString(transactionable.cryptoPrivateKey)
        )
        topUpHBARs(accountInfo, Hbar(amount))
        return TransactionResult.Ok
    }

    private fun topUpHBARs(account: AccountInfo, amount: Hbar): Status {
        val tempAccount =
            createNewAccountImpl(amount) ?: throw RuntimeException("Cannot create temp account for withdraw")
        val status = transferHBARs(tempAccount, account, amount)
        deleteAccount(tempAccount)
        return status
    }

    /**
     * Withdraws a specified amount of HBAR from a given `Transactionable`.
     *
     * @param transactionable The object representing the transaction details, must implement the `Transactionable` interface.
     * @param amount The amount of HBAR to be withdrawn.
     * @return The result of the transaction, which is an instance of the `TransactionResult` enum.
     */
    fun withdrawHBARs(transactionable: Transactionable, amount: Long): TransactionResult {
        val accountInfo = AccountInfo(
            AccountId.fromString(transactionable.cryptoId),
            PrivateKey.fromString(transactionable.cryptoPrivateKey)
        )
        withdrawHBARs(accountInfo, Hbar(amount))
        return TransactionResult.Ok
    }

    private fun withdrawHBARs(account: AccountInfo, amount: Hbar): Status {
        val tempAccount = createNewAccountImpl() ?: throw RuntimeException("Cannot create temp account for withdraw")
        val status = transferHBARs(account, tempAccount, amount)
        deleteAccount(tempAccount)
        return status
    }

    /**
     * Transfers a specified amount of HBARs from the sender to the receiver.
     *
     * @param sender The sender object implementing the [Transactionable] interface.
     * @param receiver The receiver object implementing the [Transactionable] interface.
     * @param amount The amount of HBARs to transfer.
     * @return [TransactionResult] indicating the result of the transaction.
     */
    fun transferHBARs(sender: Transactionable, receiver: Transactionable, amount: Long): TransactionResult {
        val senderInfo =
            AccountInfo(AccountId.fromString(sender.cryptoId), PrivateKey.fromString(sender.cryptoPrivateKey))
        val receiverInfo =
            AccountInfo(AccountId.fromString(receiver.cryptoId), PrivateKey.fromString(receiver.cryptoPrivateKey))
        transferHBARs(senderInfo, receiverInfo, Hbar(amount))
        return TransactionResult.Ok
    }

    private fun transferHBARs(sender: AccountInfo, receiver: AccountInfo, amount: Hbar): Status {
        return TransferTransaction()
            .addHbarTransfer(sender.accountId, amount.negated())
            .addHbarTransfer(receiver.accountId, amount)
            .freezeWith(client)
            .sign(sender.key)
            .execute(client)
            .getReceipt(client)
            .status
    }

    data class NftTreasuryInfo(val tokenId: TokenId, val treasuryAccountInfo: AccountInfo)

    /**
     * Creates a batch of non-fungible tokens (NFTs) for a challenge.
     *
     * @param numberOfChallengers The number of NFTs to create for the challenge.
     * @param challengeName The name of the challenge.
     * @param challengeSymbol The symbol of the challenge.
     * @return An instance of [NftTreasuryInfo] containing the token ID and treasury account information,
     *         or null if the token creation fails.
     * @throws RuntimeException if the new account creation fails.
     * @throws RuntimeException if the token creation fails.
     */
    fun makeNftsForChallenge(
        numberOfChallengers: Int,
        challengeName: String,
        challengeSymbol: String
    ): NftTreasuryInfo? {
        val accountInfo: AccountInfo? = createNewAccountImpl()
        if (accountInfo == null) {
            throw RuntimeException("Failed to create new account")
        }
        val supplyKey = PrivateKey.generateED25519()
        val supplyPublicKey = supplyKey.publicKey

        val treasuryId = accountInfo.accountId
        val nftCreate = TokenCreateTransaction()
            .setTokenName("Challenge $challengeName NFT")
            .setTokenSymbol(challengeSymbol)
            .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
            .setDecimals(0)
            .setInitialSupply(0)
            .setTreasuryAccountId(treasuryId)
            .setSupplyType(TokenSupplyType.FINITE)
            .setMaxSupply(250)
            .setSupplyKey(supplyKey)
            .freezeWith(client)

        // Sign the transaction with the treasury key
        val nftCreateTxSign = nftCreate.sign(accountInfo.key)
        // Submit the transaction to a Hedera network
        val nftCreateSubmit = nftCreateTxSign.execute(client)
        // Get the transaction receipt
        val nftCreateRx = nftCreateSubmit.getReceipt(client)
        // Get the token ID
        val tokenId = nftCreateRx.tokenId ?: throw RuntimeException("Failed to create token")
        // Log the token ID
        println("Created NFT with token ID $tokenId")

        fun mintBatchOfNfts(curCounterValue: Int): Int {
            val MAX_RETRIES = 5
            var retries = 0
            while (retries < MAX_RETRIES) {
                try {
                    var batchNftsCounter = curCounterValue

                    val MAX_TRANSACTION_FEE = 20
                    var mintTx = TokenMintTransaction()
                        .setTokenId(tokenId)
                        .setMaxTransactionFee(Hbar(MAX_TRANSACTION_FEE.toLong()))

                    for (i in 0 until 10) {
                        batchNftsCounter++
                        mintTx.addMetadata("$challengeName $batchNftsCounter".toByteArray())
                        if (batchNftsCounter >= numberOfChallengers) {
                            break
                        }
                    }

                    mintTx = mintTx.freezeWith(client)

                    val mintTxSign = mintTx.sign(supplyKey)
                    val mintTxSubmit = mintTxSign.execute(client)
                    val mintRx = mintTxSubmit.getReceipt(client)
                    println("Created NFT " + tokenId + " with serial: " + mintRx.serials)
                    return batchNftsCounter
                } catch (ex: PrecheckStatusException) {
                    if (ex.status == Status.BUSY) {
                        retries++;
                        println("Retry attempt: " + retries);
                    } else {
                        throw ex;
                    }
                }
            }
            return curCounterValue
        }

        var nftsCounter = 0
        while (nftsCounter < numberOfChallengers) {
            nftsCounter = mintBatchOfNfts(nftsCounter)
        }

        return NftTreasuryInfo(tokenId, accountInfo)
    }

    data class NftTokenInfo(val tokenId: String, val supplyKey: String)

    /**
     * Creates a non-fungible token (NFT) for a challenge.
     *
     * @param challenge The challenge object implementing the [Transactionable] interface.
     * @param challengeName The name of the challenge.
     * @param challengeSymbol The symbol of the challenge.
     * @return The token ID of the created NFT as a string.
     * @throws RuntimeException if the token creation fails.
     */
    fun createNftTokenForChallenge(
        challenge: Transactionable,
        challengeName: String,
        challengeSymbol: String
    ): NftTokenInfo {
        val accountInfo = AccountInfo.fromTransactionable(challenge)
        val supplyKey = PrivateKey.generateED25519()
        val supplyPublicKey = supplyKey.publicKey

        val treasuryId = accountInfo.accountId
        val nftCreate = TokenCreateTransaction()
            .setTokenName("Challenge $challengeName NFT")
            .setTokenSymbol(challengeSymbol)
            .setTokenType(TokenType.NON_FUNGIBLE_UNIQUE)
            .setDecimals(0)
            .setInitialSupply(0)
            .setTreasuryAccountId(treasuryId)
            .setSupplyType(TokenSupplyType.FINITE)
            .setMaxSupply(250)
            .setSupplyKey(supplyKey)
            .freezeWith(client)

        // Sign the transaction with the treasury key
        val nftCreateTxSign = nftCreate.sign(accountInfo.key)
        // Submit the transaction to a Hedera network
        val nftCreateSubmit = nftCreateTxSign.execute(client)
        // Get the transaction receipt
        val nftCreateRx = nftCreateSubmit.getReceipt(client)
        // Get the token ID
        val tokenId = nftCreateRx.tokenId ?: throw RuntimeException("Failed to create token")
        // Log the token ID
        println("Created NFT with token ID $tokenId")

        return NftTokenInfo(tokenId.toString(), supplyKey.toString())
    }

    /**
     * Mint a non-fungible token (NFT) for a challenger.
     *
     * @param challenge The challenge object implementing the [Transactionable] interface.
     * @param challengerName The name of the challenger.
     * @param tokenIdStr The string representation of the token ID.
     * @param supplyKeyStr The string representation of the supply key.
     * @param serial The serial number of the NFT.
     * @return The token ID of the created NFT as a string.
     * @throws RuntimeException if the token creation fails.
     */
    fun mintNftTokenForChallenger(
        challenge: Transactionable,
        challengerName: String,
        tokenIdStr: String,
        supplyKeyStr: String,
        serial: Long,
    ): String {
        val tokenId = TokenId.fromString(tokenIdStr)
        val supplyKey = PrivateKey.fromString(supplyKeyStr)

        val MAX_RETRIES = 20
        val MAX_ATTEMPTS = 100
        var retries = 0
        while (retries < MAX_RETRIES) {
            try {
                val MAX_TRANSACTION_FEE = 20
                var mintTx = TokenMintTransaction()
                    .setTokenId(tokenId)
                    .setMaxTransactionFee(Hbar(MAX_TRANSACTION_FEE.toLong()))
                    .setMaxAttempts(MAX_ATTEMPTS)

                mintTx.addMetadata(challengerName.toByteArray())

                mintTx = mintTx.freezeWith(client)

                val mintTxSign = mintTx.sign(supplyKey)
                val mintTxSubmit = mintTxSign.execute(client)
                val mintRx = mintTxSubmit.getReceipt(client)

                return NftId(tokenId, serial).toString()
            } catch (ex: PrecheckStatusException) {
                if (ex.status == Status.BUSY) {
                    retries++;
                    println("Retry attempt: " + retries);
                    Thread.sleep(1000)
                } else {
                    throw ex;
                }
            }
            catch (ex: MaxAttemptsExceededException) {
                retries++
                println("Retry attempt: " + retries);
                Thread.sleep(1000)
            }
        }

        throw RuntimeException("Cannot create NFT token for challenger")
    }

    /**
     * Retrieves the metadata of a non-fungible token (NFT) based on its ID.
     *
     * @param nftIdStr The string representation of the NFT ID.
     * @return The metadata of the NFT as a string.
     */
    fun getNftInfo(nftIdStr: String): String {
        val nftId = NftId.fromString(nftIdStr)
        val metadata = TokenNftInfoQuery()
            .setNftId(nftId)
            .execute(client)[0].metadata
        val result = String(metadata, Charsets.UTF_8)
        println("String from data $result")
        return result
    }

    private fun transferNftToAccount(
        treasuryAccountInfo: AccountInfo,
        receiverAccountInfo: AccountInfo,
        tokenId: TokenId,
        serial: Long
    ) {
        val receiverAccountId: AccountId = receiverAccountInfo.accountId
        val receiverKey: PrivateKey = receiverAccountInfo.key

        val treasuryId = treasuryAccountInfo.accountId
        val treasuryKey = treasuryAccountInfo.key

        // Create the associate transaction and sign with Alice's key
        val associateAliceTx = TokenAssociateTransaction()
            .setAccountId(receiverAccountId)
            .setTokenIds(listOf(tokenId))
            .freezeWith(client)
            .sign(receiverKey)


        // Submit the transaction to a Hedera network
        val associateAliceTxSubmit = associateAliceTx.execute(client)


        // Get the transaction receipt
        val associateAliceRx = associateAliceTxSubmit.getReceipt(client)


        // Confirm the transaction was successful
        println("NFT association with challenger account: " + associateAliceRx.status)


        // Check the balance before the NFT transfer for the treasury account
        val balanceCheckTreasury = AccountBalanceQuery().setAccountId(treasuryId)
            .execute(client)
        println("Treasury balance: " + balanceCheckTreasury.tokens + "NFTs of ID " + tokenId)


        // Check the balance before the NFT transfer for Alice's account
        val balanceCheckChallenger = AccountBalanceQuery().setAccountId(receiverAccountId)
            .execute(client)
        println("Challenger user balance: " + balanceCheckChallenger.tokens + "NFTs of ID " + tokenId)


        // Transfer NFT from treasury to Alice
        // Sign with the treasury key to authorize the transfer
        val tokenTransferTx = TransferTransaction()
            .addNftTransfer(NftId(tokenId, serial), treasuryId, receiverAccountId)
            .freezeWith(client)
            .sign(treasuryKey)

        val tokenTransferSubmit = tokenTransferTx.execute(client)
        val tokenTransferRx = tokenTransferSubmit.getReceipt(client)

        println("NFT transfer from Treasury to Challenger: " + tokenTransferRx.status)


        // Check the balance for the treasury account after the transfer
        val balanceCheckTreasury2 = AccountBalanceQuery().setAccountId(treasuryId)
            .execute(client)
        println("Treasury balance: " + balanceCheckTreasury2.tokens + "NFTs of ID " + tokenId)


        // Check the balance for Alice's account after the transfer
        val balanceCheckChallenger2 = AccountBalanceQuery().setAccountId(receiverAccountId)
            .execute(client)
        println("Challenger balance: " + balanceCheckChallenger2.tokens + "NFTs of ID " + tokenId)
    }

    /**
     * Distributes non-fungible tokens (NFTs) to a list of challengers.
     *
     * @param challenge The challenge object implementing the [Transactionable] interface.
     * @param challengers The list of challenger objects implementing the [Transactionable] interface.
     * @param tokenId The ID of the token to be distributed.
     */
    fun distributeNftsToChallengers(challenge: Transactionable, challengers: List<Transactionable>, tokenId: String) {
        val challengeAccount = AccountInfo.fromTransactionable(challenge)
        for (challenger in challengers.withIndex()) {
            val challengerAccount = AccountInfo.fromTransactionable(challenger.value)
            transferNftToAccount(
                challengeAccount,
                challengerAccount,
                TokenId.fromString(tokenId),
                (challenger.index + 1).toLong()
            )
        }
    }
}
