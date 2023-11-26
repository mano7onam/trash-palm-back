package com.cyprus.trash.api

import com.cyprus.trash.api.controller.ChallengeController
import com.cyprus.trash.model.Challenge
import com.cyprus.trash.model.Nft
import com.cyprus.trash.repo.ChallengeRepository
import com.cyprus.trash.service.ChallengeService
import com.cyprus.trash.service.HederaService
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

    @MockBean
    private lateinit var hederaService: HederaService

    @Autowired
    private lateinit var webClient: WebTestClient

    private val challengeId = UUID.randomUUID().toString()
    private val tagId = UUID.randomUUID().toString()
    private val nftId = UUID.randomUUID().toString()
    private val cryptoId = "id"
    private val cryptoPrivateKey = "key"
    private val tokenId = "tokenId"
    private val supplyKey = "supplyKey"
    private val challenge = Challenge(
        id = challengeId,
        title = "supper challenge",
        description = "challenge some",
        tagIds = listOf(tagId),
        deadlineSeconds = Instant.now().plusSeconds(100_000).truncatedTo(ChronoUnit.SECONDS).epochSecond
    )

    @Test
    fun save() {
        val accountInfo = HederaService.TransactionableImpl(cryptoId, cryptoPrivateKey)
        val nftCreationKey = HederaService.NftTokenInfo(tokenId, supplyKey)

        challengeRepository.stub {
            onBlocking {
                save(
                    challenge.copy(
                        cryptoId = accountInfo.cryptoId,
                        cryptoPrivateKey = accountInfo.cryptoPrivateKey,
                        tokenId = nftCreationKey.tokenId,
                        supplyKey = nftCreationKey.supplyKey,
                        nfts = challenge.nfts + Nft(
                            id = nftId,
                            value = 10
                        )
                    )
                )
            } doReturn challenge
        }

        hederaService.stub {
            on { createNewAccount(0) } doReturn accountInfo
            on { createNftTokenForChallenge(accountInfo, challenge.title, challenge.title.take(5)) } doReturn nftCreationKey
            on { mintNftTokenForChallenger(accountInfo, "${challenge.title} 0", nftCreationKey.tokenId, nftCreationKey.supplyKey, 1) } doReturn nftId
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
