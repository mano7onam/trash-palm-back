package com.cyprus.trash.service

import com.cyprus.trash.model.Tag
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
        return tagRepository.save(body)
    }

    suspend fun addComment(tagId: String, comment: String): Boolean {
        return tagRepository.addComment(tagId, comment)
    }

    suspend fun addPhoto(tagId: String, photoUrl: String): Boolean {
        return tagRepository.addPhoto(tagId, photoUrl)
    }
}
