package com.cyprus.trash.model

import org.springframework.data.annotation.Id

data class Tag (
    @Id val id: String,
    val lon: Double,
    val lat: Double,
    val title: String,
    val description: String,
    val owner: String,
    override val cryptoId: String = "",
    override val cryptoPrivateKey: String = "",
    val prize: Long = 0,
    val photoUrls: List<String> = listOf(),
    val type: TagType = TagType.USER,
    val comments: List<String> = listOf(),
    val status: TagStatus = TagStatus.ACTIVE,
    val claimer: String? = null,
    val voters: List<String> = listOf()
) : Transactionable
