package com.cyprus.trash.api.controller

import com.cyprus.trash.api.model.DataContainer
import com.cyprus.trash.model.Challenge
import com.cyprus.trash.service.ChallengeService
import kotlinx.coroutines.flow.toList
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("challenges")
class ChallengeController(
    private val challengeService: ChallengeService
) {

    @PostMapping
    suspend fun create(@RequestBody body: Challenge): ResponseEntity<Challenge> {
        return ResponseEntity.ok(
            challengeService.create(body)
        )
    }

    @GetMapping
    suspend fun getAll(): ResponseEntity<List<Challenge>> {
        return ResponseEntity.ok(
            challengeService.getAll().toList()
        )
    }

    @PostMapping("/{id}/join")
    suspend fun join(@RequestHeader email: String, @PathVariable id: String): ResponseEntity<DataContainer> {
        return ResponseEntity.ok(
            DataContainer(challengeService.join(id, email))
        )
    }

    @PostMapping("/{id}/finish")
    suspend fun finish(@RequestHeader email: String, @PathVariable id: String): ResponseEntity<DataContainer> {
        return ResponseEntity.ok(
            DataContainer(challengeService.finish(id, email))
        )
    }
}
