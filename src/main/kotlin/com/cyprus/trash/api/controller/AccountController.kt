package com.cyprus.trash.api.controller

import com.cyprus.trash.model.Account
import com.cyprus.trash.service.AccountService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("accounts")
class AccountController(
    private val accountService: AccountService
) {

    @GetMapping
    suspend fun getOrCreate(@RequestHeader email: String?): ResponseEntity<Account> {
        if (email.isNullOrEmpty()) {
            return ResponseEntity.badRequest().build()
        }

        val account = accountService.getOrCreate(email)

        return ResponseEntity.ok(account)
    }
}
