package com.cyprus.trash.repository

import com.cyprus.trash.model.Account
import com.cyprus.trash.model.Nft
import com.cyprus.trash.repo.AccountRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

class AccountRepositoryTest : MongodbTestBase() {

    @Test
    fun `create or get account`(): Unit = runBlocking {
        val nftId = UUID.randomUUID().toString()
        val nft = Nft(
            id = nftId,
            value = 10
        )
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
        assertEquals(account, repo.findAll(listOf(email)).first())
        assertTrue(repo.addNft(account, nft))
    }
}
