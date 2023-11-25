package com.cyprus.trash.api

import com.cyprus.trash.api.controller.AccountController
import com.cyprus.trash.model.Account
import com.cyprus.trash.repo.AccountRepository
import com.cyprus.trash.service.AccountService
import com.cyprus.trash.service.HederaService
import com.cyprus.trash.service.HederaService.TransactionableImpl
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@ExtendWith(SpringExtension::class)
@WebFluxTest(controllers = [AccountController::class])
@Import(AccountService::class)
class AccountControllerTest {
    @MockBean
    private lateinit var accountRepository: AccountRepository

    @MockBean
    private lateinit var hederaService: HederaService

    @Autowired
    private lateinit var webClient: WebTestClient

    @Test
    fun `create or get account`() {
        val cryptoId = "id"
        val cryptoPrivateKey = "key"
        val login = "some"
        val email = "$login@gmail.com"
        val account = Account(
            email = email,
            cryptoId = "",
            cryptoPrivateKey = "",
            name = login
        )

        hederaService.stub {
            onBlocking { createNewAccount(0) } doReturn TransactionableImpl(cryptoId, cryptoPrivateKey)
        }

        accountRepository.stub {
            onBlocking { get(any()) } doReturn null
            onBlocking { save(account.copy(cryptoId = cryptoId, cryptoPrivateKey = cryptoPrivateKey)) } doReturn account
        }

        webClient.get()
            .uri(URI)
            .header("email", email)
            .exchange().also {
                it.expectStatus().is2xxSuccessful
                it.expectBody<Account>().isEqualTo(account)
            }
    }

    companion object {
        private const val URI = "/accounts"
    }

}


