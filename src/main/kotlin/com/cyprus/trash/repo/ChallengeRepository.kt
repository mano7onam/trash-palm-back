package com.cyprus.trash.repo

import com.cyprus.trash.model.Challenge
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Repository

@Repository
class ChallengeRepository(
    private val mongoTemplate: ReactiveMongoTemplate
) {

    suspend fun save(challenge: Challenge): Challenge {
        return mongoTemplate.save<Challenge>(challenge, COLLECTION_NAME).awaitFirst()
    }

    fun findAll(): Flow<Challenge> {
        return mongoTemplate.findAll(CLASS, COLLECTION_NAME).asFlow()
    }

    companion object {
        private val CLASS = Challenge::class.java
        private const val COLLECTION_NAME = "challenges"
    }
}
