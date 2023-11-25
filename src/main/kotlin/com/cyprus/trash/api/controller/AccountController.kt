package com.cyprus.trash.api.controller

import com.cyprus.trash.model.Account
import com.cyprus.trash.service.AccountService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("accounts")
class AccountController(
    private val accountService: AccountService
) {

    @GetMapping("/{login}")
    suspend fun getOrCreate(@PathVariable login: String?): ResponseEntity<Account> {
        if(login.isNullOrEmpty()) {
            return ResponseEntity.badRequest().build()
        }

        val account = accountService.getOrCreate(login)

        return ResponseEntity.ok(account)
    }
}
