package com.cyprus.trash.api

import com.cyprus.trash.api.controller.ChallengeController
import com.cyprus.trash.api.model.DataContainer
import com.cyprus.trash.model.Account
import com.cyprus.trash.model.Challenge
import com.cyprus.trash.model.ChallengeStatus
import com.cyprus.trash.model.Nft
import com.cyprus.trash.model.Participant
import com.cyprus.trash.repo.AccountRepository
import com.cyprus.trash.repo.ChallengeRepository
import com.cyprus.trash.service.AccountService
import com.cyprus.trash.service.ChallengeService
import com.cyprus.trash.service.HederaService
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
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@ExtendWith(SpringExtension::class)
@WebFluxTest(controllers = [ChallengeController::class])
@Import(ChallengeService::class, AccountService::class)
class ChallengeControllerTest {
    @MockBean
    private lateinit var challengeRepository: ChallengeRepository

    @MockBean
    private lateinit var accountRepository: AccountRepository

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
    private val claimEmail = "claim@gmail.com"
    private val challenge = Challenge(
        id = challengeId,
        title = "supper challenge",
        description = "challenge some",
        tagIds = listOf(tagId),
        deadlineSeconds = Instant.now().plusSeconds(100_000).truncatedTo(ChronoUnit.SECONDS).epochSecond
    )
    private val accountInfo = HederaService.TransactionableImpl(cryptoId, cryptoPrivateKey)
    private val nftCreationKey = HederaService.NftTokenInfo(tokenId, supplyKey)
    private val claimAccount = Account(
        email = claimEmail,
        cryptoId = "",
        cryptoPrivateKey = "",
        name = "claim",
    )

    @Test
    fun save() {
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
            on { mintNftTokenForChallenger(accountInfo, "${challenge.title} 0", nftCreationKey.tokenId, nftCreationKey.supplyKey, 0L) } doReturn nftId
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

    @Test
    fun join_first() {
        accountRepository.stub {
            onBlocking { get(claimEmail) } doReturn claimAccount
        }

        challengeRepository.stub {
            onBlocking { findById(challengeId) } doReturn challenge.copy(
                cryptoId = accountInfo.cryptoId,
                cryptoPrivateKey = accountInfo.cryptoPrivateKey,
                tokenId = nftCreationKey.tokenId,
                supplyKey = nftCreationKey.supplyKey,
                nfts = challenge.nfts + Nft(
                    id = nftId,
                    value = 10
                )
            )
            onBlocking { addParticipant(challengeId, Participant(claimAccount.email, claimAccount.name)) } doReturn true
        }

        webClient.post()
            .uri("$URI/{id}/join", challengeId)
            .contentType(MediaType.APPLICATION_JSON)
            .header("email", claimEmail)
            .exchange().also {
                it.expectStatus().is2xxSuccessful
                it.expectBody<DataContainer>().isEqualTo(DataContainer(true))
            }
    }

    @Test
    fun join_second() {
        val enrichedChallenge = challenge.copy(
            cryptoId = accountInfo.cryptoId,
            cryptoPrivateKey = accountInfo.cryptoPrivateKey,
            tokenId = nftCreationKey.tokenId,
            supplyKey = nftCreationKey.supplyKey,
            nfts = challenge.nfts + Nft(
                id = nftId,
                value = 10
            ),
            participants = listOf(
                Participant("some", "some")
            )
        )
        accountRepository.stub {
            onBlocking { get(claimEmail) } doReturn claimAccount
        }

        challengeRepository.stub {
            onBlocking { findById(challengeId) } doReturn enrichedChallenge
            onBlocking { addParticipantAndNft(challengeId, Participant(claimAccount.email, claimAccount.name), Nft(nftId, 10)) } doReturn true
        }

        hederaService.stub {
            on { mintNftTokenForChallenger(enrichedChallenge, "${challenge.title} 1", nftCreationKey.tokenId, nftCreationKey.supplyKey, 1L) } doReturn nftId
        }

        webClient.post()
            .uri("$URI/{id}/join", challengeId)
            .contentType(MediaType.APPLICATION_JSON)
            .header("email", claimEmail)
            .exchange().also {
                it.expectStatus().is2xxSuccessful
                it.expectBody<DataContainer>().isEqualTo(DataContainer(true))
            }
    }

    @Test
    fun finish() {
        val nft = Nft(
            id = nftId,
            value = 10
        )
        val enrichedChallenge = challenge.copy(
            cryptoId = accountInfo.cryptoId,
            cryptoPrivateKey = accountInfo.cryptoPrivateKey,
            tokenId = nftCreationKey.tokenId,
            supplyKey = nftCreationKey.supplyKey,
            nfts = challenge.nfts + nft,
            participants = listOf(
                Participant(claimAccount.email, claimAccount.name)
            )
        )

        challengeRepository.stub {
            onBlocking { findById(challengeId) } doReturn enrichedChallenge
            onBlocking { save(any()) } doReturn enrichedChallenge.copy(
                status = ChallengeStatus.FINISHED,
                nfts = listOf()
            )
        }

        accountRepository.stub {
            onBlocking { findAll(listOf(claimEmail)) } doReturn flowOf(claimAccount)
            onBlocking { addNft(claimAccount, nft) } doReturn true
        }

        webClient.post()
            .uri("$URI/{id}/finish", challengeId)
            .contentType(MediaType.APPLICATION_JSON)
            .header("email", claimEmail)
            .exchange().also {
                it.expectStatus().is2xxSuccessful
                it.expectBody<DataContainer>().isEqualTo(DataContainer(true))
            }
    }

    companion object {
        private const val URI = "/challenges"
    }
}
