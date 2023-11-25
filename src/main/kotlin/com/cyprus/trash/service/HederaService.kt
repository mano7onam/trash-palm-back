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
}