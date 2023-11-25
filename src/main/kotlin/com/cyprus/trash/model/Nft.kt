package com.cyprus.trash.model

data class Nft(
    val id: String,
    val data: ByteArray,
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
