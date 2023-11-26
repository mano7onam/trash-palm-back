package com.cyprus.trash.repo

import com.cyprus.trash.model.Challenge
import com.cyprus.trash.model.Nft
import com.cyprus.trash.model.Participant
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
class ChallengeRepository(
    private val mongoTemplate: ReactiveMongoTemplate
) {

    suspend fun save(challenge: Challenge): Challenge {
        return mongoTemplate.save<Challenge>(challenge, COLLECTION_NAME).awaitFirst()
    }

    fun findAll(): Flow<Challenge> {
        return mongoTemplate.findAll(CLASS, COLLECTION_NAME).asFlow()
    }

    suspend fun findById(challengeId: String): Challenge? {
        return mongoTemplate.findById(challengeId, CLASS, COLLECTION_NAME).awaitFirstOrNull()
    }

    suspend fun addParticipant(challengeId: String, participant: Participant): Boolean {
        return mongoTemplate.updateFirst(
            Query().apply {
                addCriteria(
                    where(Challenge::id).isEqualTo(challengeId)
                )
            },
            Update().push(Challenge::participants.name, participant),
            CLASS,
            COLLECTION_NAME
        ).awaitFirst().modifiedCount > 0
    }

    suspend fun addParticipantAndNft(challengeId: String, participant: Participant, nft: Nft): Boolean {
        return mongoTemplate.updateFirst(
            Query().apply {
                addCriteria(
                    where(Challenge::id).isEqualTo(challengeId)
                )
            },
            Update().push(Challenge::participants.name, participant)
                .push(Challenge::nfts.name, nft),
            CLASS,
            COLLECTION_NAME
        ).awaitFirst().modifiedCount > 0
    }

    suspend fun removeNft(challengeId: String, nft: Nft): Boolean {
        return mongoTemplate.updateFirst(
            Query().apply {
                addCriteria(
                    where(Challenge::id).isEqualTo(challengeId)
                )
            },
            Update().pull(Challenge::nfts.name, nft),
            CLASS,
            COLLECTION_NAME
        ).awaitFirst().modifiedCount > 0
    }

    companion object {
        private val CLASS = Challenge::class.java
        private const val COLLECTION_NAME = "challenges"
    }
}
