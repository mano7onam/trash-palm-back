package com.cyprus.trash.model

import org.springframework.data.annotation.Id

data class Account(
    @Id val email: String,
    val cryptoId: String,
    val cryptoPrivateKey: String,
    val name: String,
    val balance: Long = 0,
    val nfts: List<Nft> = listOf(),
)
