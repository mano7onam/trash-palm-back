package com.cyprus.trash.service

import com.cyprus.trash.model.Challenge
import com.cyprus.trash.model.Nft
import com.cyprus.trash.repo.ChallengeRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

@Service
class ChallengeService(
    private val challengeRepository: ChallengeRepository,
    private val hederaService: HederaService
) {

    suspend fun create(challenge: Challenge): Challenge {
        val accountInfo = hederaService.createNewAccount(0)
        requireNotNull(accountInfo)
        val nftCreationKey = hederaService.createNftTokenForChallenge(accountInfo, challenge.title, challenge.title.take(5))

        val nftIdStr = hederaService.mintNftTokenForChallenger(
            accountInfo,
            createChallengerName(challenge.title, 0),
            nftCreationKey.tokenId,
            nftCreationKey.supplyKey,
            1
        )

        val nft = Nft(nftIdStr, 10)

        val enrichedChallenge = challenge.copy(
            cryptoId = accountInfo.cryptoId,
            cryptoPrivateKey = accountInfo.cryptoPrivateKey,
            tokenId = nftCreationKey.tokenId,
            supplyKey = nftCreationKey.supplyKey,
            nfts = challenge.nfts + nft
        )

        return challengeRepository.save(enrichedChallenge)
    }

    fun getAll(): Flow<Challenge> {
        return challengeRepository.findAll()
    }

    private fun createChallengerName(challengeName: String, index: Int): String {
        return "$challengeName $index"
    }
}
