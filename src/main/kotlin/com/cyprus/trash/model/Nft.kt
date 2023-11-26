package com.cyprus.trash.model

import org.springframework.data.annotation.Id

data class Nft(
    @Id val id: String,
    val value: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Nft

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
