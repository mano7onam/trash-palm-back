package com.cyprus.trash.api

import com.cyprus.trash.api.controller.AccountController
import com.cyprus.trash.model.Account
import com.cyprus.trash.repo.AccountRepository
import com.cyprus.trash.service.AccountService
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

    @Autowired
    private lateinit var webClient: WebTestClient

    @Test
    fun `create or get account`() {
        val login = "some"
        val email = login + AccountService.G_MAIL
        val account = Account(
            email = email,
            cryptoId = "",
            cryptoPrivateKey = "",
            name = login
        )


        accountRepository.stub {
            onBlocking { get(any()) } doReturn null
        }

        accountRepository.stub {
            onBlocking { save(any()) } doReturn account
        }

        webClient.get()
            .uri(URI, login)
            .exchange().also {
                it.expectStatus().is2xxSuccessful
                it.expectBody<Account>().isEqualTo(account)
            }
    }

    companion object {
        private const val URI = "/accounts/{login}"
    }

}


