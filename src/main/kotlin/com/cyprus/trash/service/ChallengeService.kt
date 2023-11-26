package com.cyprus.trash.service

import com.cyprus.trash.model.Challenge
import com.cyprus.trash.model.ChallengeStatus
import com.cyprus.trash.model.Nft
import com.cyprus.trash.model.Participant
import com.cyprus.trash.repo.ChallengeRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

@Service
class ChallengeService(
    private val challengeRepository: ChallengeRepository,
    private val hederaService: HederaService,
    private val accountService: AccountService
) {

    suspend fun create(challenge: Challenge): Challenge {
        val accountInfo = hederaService.createNewAccount(0)
        requireNotNull(accountInfo)
        val nftCreationKey = hederaService.createNftTokenForChallenge(accountInfo, challenge.title, challenge.title.take(5))

        val nftIdStr = hederaService.mintNftTokenForChallenger(
            accountInfo,
            createChallengerName(challenge.title, challenge.participants.size),
            nftCreationKey.tokenId,
            nftCreationKey.supplyKey,
            challenge.participants.size.toLong()
        )

        val enrichedChallenge = challenge.copy(
            cryptoId = accountInfo.cryptoId,
            cryptoPrivateKey = accountInfo.cryptoPrivateKey,
            tokenId = nftCreationKey.tokenId,
            supplyKey = nftCreationKey.supplyKey,
            nfts = challenge.nfts + Nft(nftIdStr, 10)
        )

        return challengeRepository.save(enrichedChallenge)
    }

    fun getAll(): Flow<Challenge> {
        return challengeRepository.findAll()
    }

    private fun createChallengerName(challengeName: String, index: Int): String {
        return "$challengeName $index"
    }

    suspend fun join(challengeId: String, email: String): Boolean {
        val challenge = challengeRepository.findById(challengeId) ?: return false
        val account = accountService.get(email) ?: return false
        val participant = Participant(account.email, account.name)

        if (challenge.participants.isEmpty()) {
            return challengeRepository.addParticipant(challengeId, participant)
        }

        if (challenge.participants.contains(participant)) return false

        val nftIdStr = hederaService.mintNftTokenForChallenger(
            challenge,
            createChallengerName(challenge.title, challenge.participants.size),
            challenge.tokenId,
            challenge.supplyKey,
            challenge.participants.size.toLong()
        )

        return challengeRepository.addParticipantAndNft(challengeId, participant, Nft(nftIdStr, 10))
    }

    suspend fun finish(challengeId: String, email: String): Boolean {
        val challenge = challengeRepository.findById(challengeId) ?: return false
        val accounts = accountService.findAll(challenge.participants.map { it.email })

        hederaService.distributeNftsToChallengers(challenge, accounts, challenge.tokenId)
        challenge.nfts.forEachIndexed { index, nft ->
            val account = accounts[index]
            accountService.addNft(account, nft)
        }

        return challengeRepository.save(challenge.copy(nfts = listOf(), status = ChallengeStatus.FINISHED)).status == ChallengeStatus.FINISHED
    }
}
