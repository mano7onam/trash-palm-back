package com.cyprus.trash.service

import com.cyprus.trash.api.model.TagDecision
import com.cyprus.trash.model.Tag
import com.cyprus.trash.model.TagStatus
import com.cyprus.trash.repo.TagRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service

@Service
class TagService(
    private val tagRepository: TagRepository
) {
    fun getAll(): Flow<Tag> {
        return tagRepository.findAll()
    }

    suspend fun create(body: Tag): Tag {
        //todo enrich by crypto info
        return tagRepository.save(body)
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

    suspend fun decision(tagId: String, email: String, decision: TagDecision): Boolean {
        val tag = tagRepository.findBy(tagId)
        requireNotNull(tag)
        require(tag.status == TagStatus.PROCESSING) { "wrong tag status" }
        require(tag.owner == email) { "wrong owner" }

        return when (decision) {
            TagDecision.CONFIRM -> tagRepository.saveDecision(tag.id, TagStatus.ACTIVE)
            TagDecision.DECLINE -> {
                // todo add real logic of transferring
                return tagRepository.saveDecision(tag.id, TagStatus.FINISHED)
            }
        }
    }
}
