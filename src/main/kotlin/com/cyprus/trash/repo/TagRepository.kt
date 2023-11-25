package com.cyprus.trash.repo

import com.cyprus.trash.model.Tag
import com.cyprus.trash.model.TagStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Repository

@Repository
class TagRepository(
    private val mongoTemplate: ReactiveMongoTemplate
) {

    fun findAll(): Flow<Tag> {
        return mongoTemplate.findAll(CLASS, COLLECTION_NAME).asFlow()
    }

    suspend fun findBy(id: String): Tag? {
        return mongoTemplate.findById(id, CLASS, COLLECTION_NAME).awaitFirstOrNull()
    }

    suspend fun save(tag: Tag): Tag {
        return mongoTemplate.save<Tag>(tag, COLLECTION_NAME).awaitFirst()
    }

    suspend fun addComment(tagId: String, comment: String): Boolean {
        return mongoTemplate.updateFirst(
            Query().apply {
                addCriteria(where(Tag::id).isEqualTo(tagId))
            },
            Update().push(Tag::comments.name, comment),
            CLASS,
            COLLECTION_NAME
        ).awaitFirst().modifiedCount > 0
    }

    suspend fun addPhoto(tagId: String, photo: String): Boolean {
        return mongoTemplate.updateFirst(
            Query().apply {
                addCriteria(where(Tag::id).isEqualTo(tagId))
            },
            Update().push(Tag::photoUrls.name, photo),
            CLASS,
            COLLECTION_NAME
        ).awaitFirst().modifiedCount > 0
    }

    suspend fun saveClaiming(tagId: String, email: String, photoUrls: List<String>): Boolean {
        return mongoTemplate.updateFirst(
            Query().apply {
                addCriteria(
                    where(Tag::id).isEqualTo(tagId)
                )
                addCriteria(
                    where(Tag::status).isEqualTo(TagStatus.ACTIVE)
                )
            },
            Update().push(Tag::photoUrls.name, photoUrls)
                .set(Tag::claimer.name, email)
                .set(Tag::status.name, TagStatus.PROCESSING),
            CLASS,
            COLLECTION_NAME
        ).awaitFirst().modifiedCount > 0
    }

    suspend fun saveDecision(tagId: String, status: TagStatus): Boolean {
        return mongoTemplate.updateFirst(
            Query().apply {
                addCriteria(
                    where(Tag::id).isEqualTo(tagId)
                )
                addCriteria(
                    where(Tag::status).isEqualTo(TagStatus.PROCESSING)
                )
            },
            Update().set(Tag::status.name, status),
            CLASS,
            COLLECTION_NAME
        ).awaitFirst().modifiedCount > 0
    }

    suspend fun saveVote(tagId: String, email: String, amount: Long): Boolean {
        return mongoTemplate.updateFirst(
            Query().apply {
                addCriteria(
                    where(Tag::id).isEqualTo(tagId)
                )
            },
            Update().inc(Tag::prize.name, amount)
                .push(Tag::voters.name, email),
            CLASS,
            COLLECTION_NAME
        ).awaitFirst().modifiedCount > 0
    }

    companion object {
        private val CLASS = Tag::class.java
        private const val COLLECTION_NAME = "tags"
    }
}
