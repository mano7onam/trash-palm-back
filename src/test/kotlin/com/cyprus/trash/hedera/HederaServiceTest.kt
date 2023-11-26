package com.cyprus.trash.hedera

import com.cyprus.trash.service.HederaService
import com.hedera.hashgraph.sdk.NftId
import com.hedera.hashgraph.sdk.PrivateKey
import com.hedera.hashgraph.sdk.ReceiptStatusException
import com.hedera.hashgraph.sdk.TokenId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HederaServiceTest {
    @Test
    fun `create new account with amount`(): Unit = runBlocking {
        val newAccount = HederaService().createNewAccount(42)
        assert(newAccount != null)
        val balance = HederaService().getAccountBalance(newAccount!!)
        assert(balance == 42L)
        HederaService().deleteAccount(newAccount)
    }

    @Test
    fun `delete account with amount`(): Unit = runBlocking {
        val newAccount = HederaService().createNewAccount(42)
        assert(newAccount != null)
        val balance = HederaService().getAccountBalance(newAccount!!)
        assert(balance == 42L)
        HederaService().deleteAccount(newAccount)
    }

    @Test
    fun `test private key to string ED25519`(): Unit = runBlocking {
        val accountPrivateKey = PrivateKey.generateED25519()
        val accountPrivateKeyString = accountPrivateKey.toString()
        val privateKeyFromString = PrivateKey.fromString(accountPrivateKeyString)
        val privateKeyFromStringStirng = privateKeyFromString.toString()
        assert(accountPrivateKeyString == privateKeyFromStringStirng)
    }

    @Test
    fun `test private key to string ECDSA`(): Unit = runBlocking {
        val accountPrivateKey = PrivateKey.generateECDSA()
        val accountPrivateKeyString = accountPrivateKey.toString()
        val privateKeyFromString = PrivateKey.fromString(accountPrivateKeyString)
        val privateKeyFromStringStirng = privateKeyFromString.toString()
        assert(accountPrivateKeyString == privateKeyFromStringStirng)
    }

    @Test
    fun `test withdraw 1`(): Unit = runBlocking {
        val account1 = HederaService().createNewAccount(42)
        assert(account1 != null)

        HederaService().withdrawHBARs(account1!!, 10L)
        val balance = HederaService().getAccountBalance(account1)
        assert(balance == 32L)

        HederaService().deleteAccount(account1)
    }

    @Test
    fun `test withdraw 2`() {
        val account1 = HederaService().createNewAccount(42)
        assert(account1 != null)

        HederaService().withdrawHBARs(account1!!, 10L)
        assert(HederaService().getAccountBalance(account1) == 32L)
        HederaService().withdrawHBARs(account1, 1L)
        assert(HederaService().getAccountBalance(account1) == 31L)
        HederaService().withdrawHBARs(account1, 10L)
        assert(HederaService().getAccountBalance(account1) == 21L)
        assertThrows<ReceiptStatusException> {
            HederaService().withdrawHBARs(account1, 22L)
        }

        HederaService().deleteAccount(account1)
    }

    @Test
    fun `test top up 1`(): Unit = runBlocking {
        val account1 = HederaService().createNewAccount(42)
        assert(account1 != null)

        HederaService().topUpHBARs(account1!!, 10L)
        assert(HederaService().getAccountBalance(account1) == 52L)

        HederaService().deleteAccount(account1)
    }

    @Test
    fun `test top up and withdraw`(): Unit = runBlocking {
        val account1 = HederaService().createNewAccount(42)
        assert(account1 != null)

        HederaService().topUpHBARs(account1!!, 10L)
        assert(HederaService().getAccountBalance(account1) == 52L)
        HederaService().withdrawHBARs(account1, 1L)
        assert(HederaService().getAccountBalance(account1) == 51L)
        HederaService().withdrawHBARs(account1, 10L)
        assert(HederaService().getAccountBalance(account1) == 41L)
        HederaService().topUpHBARs(account1, 4L)
        assert(HederaService().getAccountBalance(account1) == 45L)

        HederaService().deleteAccount(account1)
    }

    @Test
    fun `test transfer 1`(): Unit = runBlocking {
        val account1 = HederaService().createNewAccount(10)
        assert(account1 != null)
        val account2 = HederaService().createNewAccount(10)
        assert(account2 != null)

        HederaService().transferHBARs(account1!!, account2!!, 1L)
        assert(HederaService().getAccountBalance(account2) == 11L)
        assert(HederaService().getAccountBalance(account1) == 9L)

        HederaService().deleteAccount(account1)
        HederaService().deleteAccount(account2)
    }

    @Test
    fun `test transfer 2`(): Unit = runBlocking {
        val account1 = HederaService().createNewAccount(10)
        assert(account1 != null)
        val account2 = HederaService().createNewAccount(10)
        assert(account2 != null)
        val account3 = HederaService().createNewAccount(10)
        assert(account3 != null)

        HederaService().transferHBARs(account1!!, account2!!, 2L)
        assert(HederaService().getAccountBalance(account2) == 12L)
        assert(HederaService().getAccountBalance(account1) == 8L)

        HederaService().transferHBARs(account2, account3!!, 5L)
        assert(HederaService().getAccountBalance(account2) == 7L)
        assert(HederaService().getAccountBalance(account3) == 15L)

        HederaService().transferHBARs(account3, account1, 10L)
        assert(HederaService().getAccountBalance(account3) == 5L)
        assert(HederaService().getAccountBalance(account1) == 18L)

        assertThrows<ReceiptStatusException> {
            HederaService().transferHBARs(account2, account1, 8L)
        }
        assert(HederaService().getAccountBalance(account1) == 18L)
        assert(HederaService().getAccountBalance(account2) == 7L)
        assert(HederaService().getAccountBalance(account3) == 5L)

        HederaService().deleteAccount(account1)
        HederaService().deleteAccount(account2)
        HederaService().deleteAccount(account3)
    }

    @Test
    fun `test challenge nfts`(): Unit {
        val account1 = HederaService().createNewAccount(10)
        val account2 = HederaService().createNewAccount(10)
        val account3 = HederaService().createNewAccount(10)
        val accounts = listOf(account1!!, account2!!, account3!!)

        val challengeAccount = HederaService().createNewAccount(10)!!
        val tokenInfo = HederaService().createNftTokenForChallenge(challengeAccount, "The Challenge", "TH")

        for (account in accounts.withIndex()) {
            val challengerName = "Challenger ${account.index + 1}"
            val nftIdStr = HederaService().mintNftTokenForChallenger(
                challengeAccount,
                challengerName,
                tokenInfo.tokenId,
                tokenInfo.supplyKey,
                (account.index + 1).toLong()
            )
            val nftInfoAgain = HederaService().getNftInfo(nftIdStr)
            assert(nftInfoAgain == challengerName)
        }

        HederaService().distributeNftsToChallengers(challengeAccount, accounts, tokenInfo.tokenId)

        for (i in 1..accounts.size) {
            val challengerName = "Challenger $i"
            val nftIdStr = NftId(TokenId.fromString(tokenInfo.tokenId), i.toLong()).toString()
            val nftInfo = HederaService().getNftInfo(nftIdStr)
            assert(nftInfo == challengerName)
        }
    }
}