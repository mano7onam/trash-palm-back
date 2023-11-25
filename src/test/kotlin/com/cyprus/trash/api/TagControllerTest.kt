package com.cyprus.trash.api

import com.cyprus.trash.api.controller.TagController
import com.cyprus.trash.api.model.DataContainer
import com.cyprus.trash.model.Account
import com.cyprus.trash.model.Tag
import com.cyprus.trash.repo.AccountRepository
import com.cyprus.trash.repo.TagRepository
import com.cyprus.trash.service.AccountService
import com.cyprus.trash.service.HederaService
import com.cyprus.trash.service.TagService
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import java.util.*

@ExtendWith(SpringExtension::class)
@WebFluxTest(controllers = [TagController::class])
@Import(TagService::class, AccountService::class)
class TagControllerTest {
    @MockBean
    private lateinit var tagRepository: TagRepository

    @MockBean
    private lateinit var hederaService: HederaService

    @MockBean
    private lateinit var accountRepository: AccountRepository

    @Autowired
    private lateinit var webClient: WebTestClient

    private val tagId = UUID.randomUUID().toString()
    private val email = "someEmail@gmail.com"
    private val comment = "supper comment"
    private val cryptoId = "id"
    private val cryptoPrivateKey = "key"
    private val tag = Tag(
        id = tagId,
        lon = 5.0,
        lat = 5.0,
        title = "title",
        description = "description",
        owner = email
    )
    private val account = Account(
        email = email,
        cryptoId = "",
        cryptoPrivateKey = "",
        name = "some"
    )

    @Test
    fun save() {
        hederaService.stub {
            onBlocking { createNewAccount(0) } doReturn HederaService.TransactionableImpl(cryptoId, cryptoPrivateKey)
        }
        accountRepository.stub {
            onBlocking { get(email) } doReturn account
        }
        tagRepository.stub {
            onBlocking { save(any()) } doReturn tag
        }

        webClient.post()
            .uri(URI)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(tag))
            .exchange().also {
                it.expectStatus().is2xxSuccessful
                it.expectBody<Tag>().isEqualTo(tag)
            }
    }

    @Test
    fun `add comment`() {
        tagRepository.stub {
            onBlocking { addComment(tagId, comment) } doReturn true
        }

        webClient.post()
            .uri("$URI/{id}/comments", tagId)
            .body(BodyInserters.fromValue(DataContainer(comment)))
            .exchange().also {
                it.expectStatus().is2xxSuccessful
                it.expectBody<DataContainer>().isEqualTo(DataContainer(true))
            }
    }

    @Test
    fun `get all`() {
        tagRepository.stub {
            onBlocking { findAll() } doReturn flowOf(tag)
        }

        webClient.get()
            .uri(URI)
            .exchange().also {
                it.expectStatus().is2xxSuccessful
                it.expectBody<List<Tag>>().isEqualTo(listOf(tag))
            }
    }

    companion object {
        private const val URI = "/tags"
    }
}
