package com.cyprus.trash.model

import org.springframework.data.annotation.Id

data class Account(
    @Id val email: String,
    override val cryptoId: String,
    override val cryptoPrivateKey: String,
    val name: String,
    val balance: Long = 0,
    val nfts: List<Nft> = listOf(),
) : Transactionable
