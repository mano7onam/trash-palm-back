package com.cyprus.trash.repository

import com.cyprus.trash.model.Nft
import com.cyprus.trash.model.Tag
import com.cyprus.trash.repo.TagRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class TagRepositoryTest : MongodbTestBase() {

    @Test
    fun `create or get account`(): Unit = runBlocking {
        val tagId = UUID.randomUUID().toString()
        val nftId = UUID.randomUUID().toString()
        val email = "someEmail@gmail.com"
        val comment = "supper comment"
        val repo = TagRepository(template)
        val tag = Tag(
            id = tagId,
            longitude = 5.0,
            latitude = 5.0,
            title = "title",
            description = "description",
            createdBy = email,
            prize = Nft(
                id = nftId,
                data = ByteArray(10),
                value = 10
            )
        )

        assertEquals(listOf<Tag>(), repo.findAll().toList())
        assertEquals(tag, repo.save(tag))
        assertEquals(1, repo.findAll().toList().size)
        assertEquals(true, repo.addComment(tagId, comment))
        assertEquals(listOf(comment), repo.findAll().toList().first.comments)
    }
}
