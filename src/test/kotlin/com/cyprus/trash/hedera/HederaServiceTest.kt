package com.cyprus.trash.hedera

import com.cyprus.trash.service.HederaService
import com.hedera.hashgraph.sdk.PrivateKey
import com.hedera.hashgraph.sdk.ReceiptStatusException
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
    fun `test transfer`(): Unit = runBlocking {
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
}