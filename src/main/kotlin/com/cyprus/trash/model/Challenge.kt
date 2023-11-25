package com.cyprus.trash.model

import org.springframework.data.annotation.Id
import java.time.Instant

data class Challenge(
    @Id val id: String,
    val title: String,
    val description: String,
    val tagIds: List<String> = listOf(),
    val nfts: List<Nft> = listOf(),
    val deadline: Instant = Instant.now(),
    val status: ChallengeStatus = ChallengeStatus.ACTIVE,
    override val cryptoId: String = "",
    override val cryptoPrivateKey: String = "",
) : Transactionable
