package com.cyprus.trash.model

data class Tag (
    val id: String,
    val longitude: Double,
    val latitude: Double,
    val title: String,
    val description: String,
    val createdBy: String,
    val prize: Nft,
    val videoUrl: String? = null,
    val photoUrls: List<String> = listOf(),
    val type: TagType = TagType.USER,
    val comments: List<String> = listOf(),
)
