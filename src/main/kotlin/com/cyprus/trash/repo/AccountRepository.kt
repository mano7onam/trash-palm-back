package com.cyprus.trash.repo

import com.cyprus.trash.model.Account
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Repository

@Repository
class AccountRepository(
    private val mongoTemplate: ReactiveMongoTemplate
) {
    suspend fun get(email: String): Account? {
        return mongoTemplate.findById(email, CLASS, COLLECTION_NAME).awaitFirstOrNull()
    }

    suspend fun save(account: Account): Account {
        return mongoTemplate.save<Account>(account, COLLECTION_NAME).awaitFirst()
    }

    companion object {
        private val CLASS = Account::class.java
        private val COLLECTION_NAME = "accounts"
    }
}
