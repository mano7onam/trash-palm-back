package com.cyprus.trash.service

import com.cyprus.trash.model.Challenge
import com.cyprus.trash.repo.ChallengeRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

@Service
class ChallengeService(
    private val challengeRepository: ChallengeRepository
) {

    suspend fun create(challenge: Challenge): Challenge {
        return challengeRepository.save(challenge)
    }

    fun getAll(): Flow<Challenge> {
        return challengeRepository.findAll()
    }
}
