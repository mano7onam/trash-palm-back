package com.cyprus.trash.api

import com.cyprus.trash.api.controller.ChallengeController
import com.cyprus.trash.model.Challenge
import com.cyprus.trash.model.Nft
import com.cyprus.trash.repo.ChallengeRepository
import com.cyprus.trash.service.ChallengeService
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
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
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@ExtendWith(SpringExtension::class)
@WebFluxTest(controllers = [ChallengeController::class])
@Import(ChallengeService::class)
class ChallengeControllerTest {
    @MockBean
    private lateinit var challengeRepository: ChallengeRepository

    @Autowired
    private lateinit var webClient: WebTestClient

    private val challengeId = UUID.randomUUID().toString()
    private val tagId = UUID.randomUUID().toString()
    private val nftId = UUID.randomUUID().toString()
    private val challenge = Challenge(
        id = challengeId,
        title = "supper challenge",
        description = "challenge some",
        tagIds = listOf(tagId),
        nfts = listOf(
            Nft(
                id = nftId,
                data = ByteArray(10),
                value = 10
            )
        ),
        deadline = Instant.now().plusSeconds(100_000).truncatedTo(ChronoUnit.SECONDS)
    )

    @Test
    fun save() {
        challengeRepository.stub {
            onBlocking { save(challenge) } doReturn challenge
        }

        webClient.post()
            .uri(URI)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(challenge))
            .exchange().also {
                it.expectStatus().is2xxSuccessful
                it.expectBody<Challenge>().isEqualTo(challenge)
            }
    }

    @Test
    fun `get all`() {
        challengeRepository.stub {
            onBlocking { findAll() } doReturn flowOf(challenge)
        }

        webClient.get()
            .uri(URI)
            .exchange().also {
                it.expectStatus().is2xxSuccessful
                it.expectBody<List<Challenge>>().isEqualTo(listOf(challenge))
            }
    }

    companion object {
        private const val URI = "/challenges"
    }
}
