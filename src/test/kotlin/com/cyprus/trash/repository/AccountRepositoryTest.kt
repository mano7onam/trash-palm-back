package com.cyprus.trash.repository

import com.cyprus.trash.model.Account
import com.cyprus.trash.repo.AccountRepository
import com.cyprus.trash.service.AccountService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AccountRepositoryTest : MongodbTestBase() {

    @Test
    fun `create or get account`(): Unit = runBlocking {
        val login = "some"
        val email = "$login@gmail.com"
        val account = Account(
            email = email,
            cryptoId = "",
            cryptoPrivateKey = "",
            name = login
        )

        val repo = AccountRepository(template)

        assertEquals(null, repo.get(email))
        assertEquals(account, repo.save(account))
        assertEquals(account, repo.get(email))
    }
}
