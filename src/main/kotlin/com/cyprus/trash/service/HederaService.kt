package com.cyprus.trash.service

import com.hedera.hashgraph.sdk.*
import io.github.cdimascio.dotenv.Dotenv
import java.util.concurrent.TimeoutException


object HederaService {
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

    data class AccountInfo(val accountId: AccountId, val key: PrivateKey)

    /**
     * Creates a new account with the specified initial balance.
     *
     * @param client The Hedera client.
     * @param initialBalance The initial balance for the new account (default value is 10).
     * @return An [AccountInfo] object containing the account ID and private key of the created account,
     *         or null if the account creation fails.
     * @throws ReceiptStatusException if there was an error retrieving the receipt for the account creation transaction.
     * @throws TimeoutException if the operation times out.
     * @throws PrecheckStatusException if the transaction failed pre-check.
     */
    @Throws(ReceiptStatusException::class, TimeoutException::class, PrecheckStatusException::class)
    fun createNewAccount(client: Client, initialBalance: Long = 10): AccountInfo? {
        val accountPrivateKey = PrivateKey.generateED25519()
        val account = AccountCreateTransaction()
            .setKey(accountPrivateKey.publicKey)
            .setInitialBalance(Hbar(initialBalance))
            .execute(client)

        val accountId = account.getReceipt(client).accountId
        println("New account created with ID $accountId")

        if (null == accountId) return null
        return AccountInfo(accountId, accountPrivateKey)
    }

    fun getAccountInfo(accountId: String, privateKey: String): AccountInfo {
        return AccountInfo(
            AccountId.fromString(accountId),
            PrivateKey.fromString(privateKey)
        )
    }

    fun makeTransferNft(client: Client, tokenId: TokenId, sender: AccountInfo, receiver: AccountInfo) {
        val tokenTransferTx: TransferTransaction = TransferTransaction()
            .addNftTransfer(NftId(tokenId, 1), sender.accountId, receiver.accountId)
            .freezeWith(client)
            .sign(sender.key)

        val tokenTransferSubmit: TransactionResponse = tokenTransferTx.execute(client)
        val tokenTransferRx: TransactionReceipt = tokenTransferSubmit.getReceipt(client)

        println("NFT transfer from sender to receiver: " + tokenTransferRx.status)
    }

    /**
     * Retrieves the account balance of the specified account ID.
     *
     * @param accountId the ID of the account
     * @return the account balance in Hbars
     */
    fun getAccountBalance(accountId: AccountId): Hbar {
        return AccountBalanceQuery()
            .setAccountId(accountId)
            .execute(client).hbars
    }

    /**
     * Transfers a specified amount of HBAR from one account to another.
     *
     * @param senderId the ID of the account from which the HBAR is to be transferred
     * @param receiverId the ID of the account to which the HBAR is to be transferred
     * @param amount the amount of HBAR to be transferred
     * @return the response of the transaction
     */
    fun transferHBAR(senderId: AccountId, receiverId: AccountId, amount: Hbar): TransactionResponse {
        return TransferTransaction()
            .addHbarTransfer(senderId, amount)
            .addHbarTransfer(receiverId, amount)
            .execute(client)
    }

    /**
     * Executes a transaction with a given key and returns the transaction receipt.
     * If the execution fails due to a BUSY status, the method will retry up to 5 times.
     * If the execution fails after the maximum number of retries, an exception will be thrown.
     *
     * @param transaction The transaction to execute.
     * @param key The private key to sign the transaction.
     * @return The transaction receipt.
     * @throws Exception if the execution fails after the maximum number of retries or any other unexpected error occurs.
     */
    @Throws(Exception::class)
    private fun executeTransaction(transaction: Transaction<*>, key: PrivateKey): TransactionReceipt {
        val MAX_RETRIES = 5

        var retries = 0

        while (retries < MAX_RETRIES) {
            try {
                val txResponse = transaction.sign(key).execute(client)
                val txReceipt = txResponse.getReceipt(client)

                return txReceipt
            } catch (e: PrecheckStatusException) {
                if (e.status == Status.BUSY) {
                    retries++
                    println("Retry attempt: $retries")
                } else {
                    throw e
                }
            }
        }

        throw Exception("Transaction failed after $MAX_RETRIES attempts")
    }

    /**
     * Creates [numberOfChallengers] NFTs for a challenge.
     *
     * @param numberOfChallengers The number of NFTs to create.
     * @param challengeName The name of the challenge.
     * @param challengeSymbol The symbol of the challenge NFTs.
     * @return An [AccountInfo] object containing the account ID and private key of the created account, or null if the account creation fails.
     * @throws RuntimeException if failed to create a new account or to create the token.
     */
    fun makeNftsForChallenge(numberOfChallengers: Int, challengeName: String, challengeSymbol: String): AccountInfo? {
        val accountInfo: AccountInfo? = createNewAccount(client)
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
                }
                catch (ex: PrecheckStatusException) {
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

        return accountInfo
    }
}