package com.cyprus.trash.repository

import com.cyprus.trash.model.Challenge
import com.cyprus.trash.model.Tag
import com.cyprus.trash.repo.ChallengeRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class ChallengeRepositoryTest : MongodbTestBase() {

    @Test
    fun `full life cycle`(): Unit = runBlocking {
        val challengeId = UUID.randomUUID().toString()
        val tagId = UUID.randomUUID().toString()
        val nftId = UUID.randomUUID().toString()
        val challenge = Challenge(
            id = challengeId,
            title = "supper challenge",
            description = "challenge some",
            tagIds = listOf(tagId),
            prizes = listOf(nftId),
            deadline = Instant.now().plusSeconds(100_000).truncatedTo(ChronoUnit.SECONDS)
        )

        val repo = ChallengeRepository(template)

        assertEquals(listOf<Tag>(), repo.findAll().toList())
        assertEquals(challenge, repo.save(challenge))
        repo.findAll().toList().also {
            assertEquals(1, it.size)
            assertEquals(challenge, it.first())
        }
    }
}
