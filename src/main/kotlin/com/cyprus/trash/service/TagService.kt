package com.cyprus.trash.service

import com.cyprus.trash.api.model.TagDecision
import com.cyprus.trash.model.Tag
import com.cyprus.trash.model.TagStatus
import com.cyprus.trash.repo.TagRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

@Service
class TagService(
    private val tagRepository: TagRepository,
    private val accountService: AccountService,
    private val hederaService: HederaService
) {

    fun getAll(): Flow<Tag> {
        return tagRepository.findAll()
    }

    suspend fun create(tag: Tag): Tag {
        val account = accountService.get(tag.owner)

        requireNotNull(account)
        require(account.balance >= tag.prize)

        val accountInfo = hederaService.createNewAccount(0)
        requireNotNull(accountInfo)

        val enrichedTag = tag.copy(
            cryptoId = accountInfo.cryptoId,
            cryptoPrivateKey = accountInfo.cryptoPrivateKey
        )

        if (enrichedTag.prize > 0) {
            val rs = hederaService.transferHBARs(enrichedTag, account, enrichedTag.prize)
            require(rs == HederaService.TransactionResult.Ok)
            accountService.decreaseBalance(tag.owner, enrichedTag.prize)
        }

        return tagRepository.save(enrichedTag)
    }

    suspend fun addComment(tagId: String, comment: String): Boolean {
        return tagRepository.addComment(tagId, comment)
    }

    suspend fun addPhoto(tagId: String, photoUrl: String): Boolean {
        return tagRepository.addPhoto(tagId, photoUrl)
    }

    suspend fun claim(email: String, tagId: String, photoUrls: List<String>): Boolean {
        val tag = tagRepository.findBy(tagId)
        requireNotNull(tag)
        require(tag.status == TagStatus.ACTIVE) { "wrong tag status" }

        return tagRepository.saveClaiming(tagId, email, photoUrls)
    }

    suspend fun vote(tagId: String, email: String, amount: Long): Boolean {
        val account = accountService.get(email)
        val tag = tagRepository.findBy(tagId)

        requireNotNull(tag)
        requireNotNull(account)
        require(account.balance > amount)

        val rs = hederaService.transferHBARs(account, tag, amount)
        require(rs == HederaService.TransactionResult.Ok)

        accountService.decreaseBalance(email, amount)
        return tagRepository.saveVote(tagId, email, amount)
    }

    suspend fun decision(tagId: String, email: String, decision: TagDecision): Boolean {
        val tag = tagRepository.findBy(tagId)
        val claimer = accountService.get(email)

        requireNotNull(tag)
        requireNotNull(claimer)

        require(tag.status == TagStatus.PROCESSING) { "wrong tag status" }
        require(tag.owner == email) { "wrong owner" }

        return when (decision) {
            TagDecision.CONFIRM -> {
                val rs = hederaService.transferHBARs(tag, claimer, tag.prize)
                require(rs == HederaService.TransactionResult.Ok)

                accountService.increaseBalance(email, tag.prize, true)
                return tagRepository.saveDecision(tag.id, TagStatus.FINISHED)
            }

            TagDecision.DECLINE -> tagRepository.saveDecision(tag.id, TagStatus.ACTIVE)
        }
    }
}
