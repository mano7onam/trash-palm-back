package com.cyprus.trash.model

import org.springframework.data.annotation.Id

data class Tag (
    @Id val id: String,
    val longitude: Double,
    val latitude: Double,
    val title: String,
    val description: String,
    val createdBy: String,
    val prize: Nft? = null,
    val photoUrls: List<String> = listOf(),
    val type: TagType = TagType.USER,
    val comments: List<String> = listOf(),
    val status: TagStatus = TagStatus.ACTIVE,
    val claimer: String? = null
)
