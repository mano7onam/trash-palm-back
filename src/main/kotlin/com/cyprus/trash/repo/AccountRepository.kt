package com.cyprus.trash.repo

import com.cyprus.trash.model.Account
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
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

    suspend fun changeBalance(email: String, amount: Long, increase: Boolean): Boolean {
        return mongoTemplate.updateFirst(
            Query().apply {
                addCriteria(
                    where(Account::email).isEqualTo(email)
                )
            },
            Update().inc(Account::balance.name, if (increase) amount else -amount),
            CLASS,
            COLLECTION_NAME
        ).awaitFirst().modifiedCount > 0
    }

    companion object {
        private val CLASS = Account::class.java
        private const val COLLECTION_NAME = "accounts"
    }
}
