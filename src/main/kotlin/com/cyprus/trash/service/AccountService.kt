package com.cyprus.trash.service

import com.cyprus.trash.model.Account
import com.cyprus.trash.repo.AccountRepository
import org.springframework.stereotype.Service

@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val hederaService: HederaService
) {

    companion object {
        const val G_MAIL = "@gmail.com"
    }

    suspend fun getOrCreate(login: String): Account {
        val email = login + G_MAIL
        val account = accountRepository.get(email)
        if (account == null) {
            val accountInfo = hederaService.createNewAccount(0)
            requireNotNull(accountInfo)

            return accountRepository.save(
                Account(
                    email = email,
                    cryptoId = accountInfo.cryptoId,
                    cryptoPrivateKey = accountInfo.cryptoPrivateKey,
                    name = login
                )
            )
        }

        return account
    }

    suspend fun get(email: String): Account? {
        return accountRepository.get(email)
    }

    suspend fun increaseBalance(email: String, amount: Long, increase: Boolean = false): Boolean {
        return accountRepository.changeBalance(email, amount, true)
    }

    suspend fun decreaseBalance(email: String, amount: Long): Boolean {
        return accountRepository.changeBalance(email, amount, false)
    }
}
