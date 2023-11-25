package com.cyprus.trash.service

import com.cyprus.trash.model.Account
import com.cyprus.trash.repo.AccountRepository
import org.springframework.stereotype.Service

@Service
class AccountService(
    private val accountRepository: AccountRepository
) {

    companion object {
        const val G_MAIL = "@gmail.com"
    }

    suspend fun getOrCreate(login: String): Account {
        val email = login + G_MAIL
        val account = accountRepository.get(email)
        if (account == null) {
            // int with crypto
            val account = Account(
                email = email,
                cryptoId = "",
                cryptoPrivateKey = "",
                name = login
            )
            return accountRepository.save(account)
        }

        return account
    }
}
