package com.cyprus.trash.repo

import com.cyprus.trash.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
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

    companion object {
        private val CLASS = Tag::class.java
        private val COLLECTION_NAME = "tags"
    }

}
