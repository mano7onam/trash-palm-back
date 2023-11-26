package com.cyprus.trash.service

import com.cyprus.trash.model.Account
import com.cyprus.trash.model.Nft
import com.cyprus.trash.repo.AccountRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service

@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val hederaService: HederaService
) {

    suspend fun getOrCreate(email: String): Account {
        val account = accountRepository.get(email)
        if (account == null) {
            val accountInfo = hederaService.createNewAccount(0)
            requireNotNull(accountInfo)

            return accountRepository.save(
                Account(
                    email = email,
                    cryptoId = accountInfo.cryptoId,
                    cryptoPrivateKey = accountInfo.cryptoPrivateKey,
                    name = email.split("@")[0]
                )
            )
        }

        return account
    }

    suspend fun get(email: String): Account? {
        return accountRepository.get(email)
    }

    suspend fun findAll(emails: List<String>): List<Account> {
        return accountRepository.findAll(emails).toList()
    }

    suspend fun increaseBalance(email: String, amount: Long, increase: Boolean = false): Boolean {
        return accountRepository.changeBalance(email, amount, true)
    }

    suspend fun decreaseBalance(email: String, amount: Long): Boolean {
        return accountRepository.changeBalance(email, amount, false)
    }

    suspend fun addNft(account: Account, nft: Nft): Boolean {
        return accountRepository.addNft(account, nft)
    }
}
