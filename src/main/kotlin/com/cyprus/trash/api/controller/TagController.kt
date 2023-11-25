package com.cyprus.trash.api.controller

import com.cyprus.trash.api.model.DataContainer
import com.cyprus.trash.model.Tag
import com.cyprus.trash.service.TagService
import kotlinx.coroutines.flow.toList
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("tags")
class TagController(
    private val tagService: TagService
) {

    @GetMapping
    suspend fun getAll(@RequestHeader email: String?): ResponseEntity<List<Tag>> {
        return ResponseEntity.ok(
            tagService.getAll().toList()
        )
    }

    @PostMapping
    suspend fun create(@RequestBody body: Tag): ResponseEntity<Tag> {
        return ResponseEntity.ok(
            tagService.create(body)
        )
    }

    @PostMapping("/{id}/comments")
    suspend fun addComment(@PathVariable id: String, @RequestBody body: DataContainer): ResponseEntity<DataContainer> {
        return ResponseEntity.ok(
            DataContainer(
                tagService.addComment(id, body.data).toString()
            )
        )
    }

    @PostMapping("/{id}/photos")
    suspend fun addPhoto(@PathVariable id: String, @RequestBody body: DataContainer): ResponseEntity<DataContainer> {
        return ResponseEntity.ok(
            DataContainer(
                tagService.addPhoto(id, body.data).toString()
            )
        )
    }
}
