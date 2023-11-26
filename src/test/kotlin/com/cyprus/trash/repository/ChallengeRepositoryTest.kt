package com.cyprus.trash.repository

import com.cyprus.trash.model.Account
import com.cyprus.trash.model.Challenge
import com.cyprus.trash.model.Nft
import com.cyprus.trash.model.Participant
import com.cyprus.trash.model.Tag
import com.cyprus.trash.repo.ChallengeRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class ChallengeRepositoryTest : MongodbTestBase() {

    @Test
    fun `full life cycle`(): Unit = runBlocking {
        val login = "some"
        val email = "$login@gmail.com"
        val account = Account(
            email = email,
            cryptoId = "",
            cryptoPrivateKey = "",
            name = login
        )
        val login1 = "some"
        val email1 = "$login1@gmail.com"
        val account1 = Account(
            email = email1,
            cryptoId = "",
            cryptoPrivateKey = "",
            name = login1
        )
        val challengeId = UUID.randomUUID().toString()
        val tagId = UUID.randomUUID().toString()
        val nftId = UUID.randomUUID().toString()
        val nftId1 = UUID.randomUUID().toString()
        val challenge = Challenge(
            id = challengeId,
            title = "supper challenge",
            description = "challenge some",
            tagIds = listOf(tagId),
            nfts = listOf(
                Nft(
                    id = nftId,
                    value = 10
                )
            ),
            deadlineSeconds = Instant.now().plusSeconds(100_000).truncatedTo(ChronoUnit.SECONDS).epochSecond
        )

        val repo = ChallengeRepository(template)

        assertEquals(listOf<Tag>(), repo.findAll().toList())
        assertEquals(challenge, repo.save(challenge))
        repo.findAll().toList().also {
            assertEquals(1, it.size)
            assertEquals(challenge, it.first())
        }
        assertEquals(challenge, repo.findById(challengeId))
        assertTrue(repo.addParticipant(challengeId, Participant(account.email, account.name)))
        assertTrue(repo.addParticipantAndNft(challengeId, Participant(account1.email, account1.name), Nft(nftId1, 10)))
    }
}
