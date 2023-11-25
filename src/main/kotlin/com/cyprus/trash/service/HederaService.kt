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

    fun getAccountBalance(accountId: AccountId): Hbar {
        return AccountBalanceQuery()
            .setAccountId(accountId)
            .execute(client).hbars
    }

    fun topUpHBARs(accountId: AccountId, amount: Hbar): TransactionResponse {
        return TransferTransaction().addHbarTransfer(accountId, amount).execute(client)
    }

    fun withdrawHBARs(accountId: AccountId, amount: Hbar): TransactionResponse {
        return TransferTransaction().addHbarTransfer(accountId, amount.negated()).execute(client)
    }

    fun transferHBARs(senderId: AccountId, receiverId: AccountId, amount: Hbar): TransactionResponse {
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
    fun makeNftsForChallenge(numberOfChallengers: Int, challengeName: String, challengeSymbol: String): NftTreasuryInfo? {
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

        return NftTreasuryInfo(tokenId, accountInfo)
    }

    /**
     * Transfers the specified non-fungible token (NFT) from the treasury account to the receiver account.
     *
     * @param treasuryAccountInfo The [AccountInfo] object containing the details of the treasury account.
     * @param receiverAccountInfo The [AccountInfo] object containing the details of the receiver account.
     * @param tokenId The ID of the NFT to be transferred.
     * @param serial The serial number of the NFT to be transferred.
     */
    fun transferNftToAccount(
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
        println("NFT association with Alice's account: " + associateAliceRx.status)


        // Check the balance before the NFT transfer for the treasury account
        val balanceCheckTreasury = AccountBalanceQuery().setAccountId(treasuryId)
            .execute(client)
        println("Treasury balance: " + balanceCheckTreasury.tokens + "NFTs of ID " + tokenId)


        // Check the balance before the NFT transfer for Alice's account
        val balanceCheckAlice = AccountBalanceQuery().setAccountId(receiverAccountId)
            .execute(client)
        println("Alice's balance: " + balanceCheckAlice.tokens + "NFTs of ID " + tokenId)


        // Transfer NFT from treasury to Alice
        // Sign with the treasury key to authorize the transfer
        val tokenTransferTx = TransferTransaction()
            .addNftTransfer(NftId(tokenId, serial), treasuryId, receiverAccountId)
            .freezeWith(client)
            .sign(treasuryKey)

        val tokenTransferSubmit = tokenTransferTx.execute(client)
        val tokenTransferRx = tokenTransferSubmit.getReceipt(client)

        println("NFT transfer from Treasury to Alice: " + tokenTransferRx.status)


        // Check the balance for the treasury account after the transfer
        val balanceCheckTreasury2 = AccountBalanceQuery().setAccountId(treasuryId)
            .execute(client)
        println("Treasury balance: " + balanceCheckTreasury2.tokens + "NFTs of ID " + tokenId)


        // Check the balance for Alice's account after the transfer
        val balanceCheckAlice2 = AccountBalanceQuery().setAccountId(receiverAccountId)
            .execute(client)
        println("Alice's balance: " + balanceCheckAlice2.tokens + "NFTs of ID " + tokenId)
    }

    /**
     * Distributes non-fungible tokens (NFTs) to a list of challengers.
     *
     * @param challengeName The name of the challenge.
     * @param challengeSymbol The symbol of the challenge.
     * @param challengers The list of [AccountInfo] objects representing the challengers to whom the NFTs will be distributed.
     */
    fun distributeNftsToChallengers(challengeName: String, challengeSymbol: String, challengers: List<AccountInfo>) {
        val treasuryNftInfo = makeNftsForChallenge(challengers.size, challengeName, challengeSymbol) ?: return
        for (challenger in challengers.withIndex()) {
            transferNftToAccount(treasuryNftInfo.treasuryAccountInfo, challenger.value, treasuryNftInfo.tokenId, (challenger.index + 1).toLong())
            println("-------------------------")
            println()
        }
    }
}