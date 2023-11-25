package com.cyprus.trash.repository

import com.cyprus.trash.model.Tag
import com.cyprus.trash.model.TagStatus
import com.cyprus.trash.repo.TagRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class TagRepositoryTest : MongodbTestBase() {

    @Test
    fun `create life cycle`(): Unit = runBlocking {
        val tagId = UUID.randomUUID().toString()
        val creatorEmail = "someEmail@gmail.com"
        val claimingEmail = "claiming@gmail.com"
        val voterEmail = "voter@gmail.com"
        val comment = "supper comment"
        val provingPhotos = listOf("url1", "url2")
        val repo = TagRepository(template)
        val tag = Tag(
            id = tagId,
            lon = 5.0,
            lat = 5.0,
            title = "title",
            description = "description",
            owner = creatorEmail,
            prize = 10
        )

        assertEquals(listOf<Tag>(), repo.findAll().toList())
        assertEquals(tag, repo.save(tag))
        assertEquals(1, repo.findAll().toList().size)
        assertEquals(true, repo.addComment(tagId, comment))
        assertEquals(listOf(comment), repo.findAll().toList().first().comments)
        assertEquals(true, repo.saveClaiming(tagId, claimingEmail, provingPhotos))
        repo.findBy(tagId).also {
            assertEquals(TagStatus.PROCESSING, it?.status)
            assertEquals(claimingEmail, it?.claimer)
        }
        assertEquals(true, repo.saveVote(tagId, voterEmail, 10))
        repo.findBy(tagId).also {
            assertTrue(it?.voters?.contains(voterEmail) ?: false)
            assertEquals(20, it?.prize)
        }
        repo.saveDecision(tagId, TagStatus.FINISHED)
        repo.findBy(tagId).also {
            assertEquals(TagStatus.FINISHED, it?.status)
        }
    }
}
