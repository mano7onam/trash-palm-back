package com.cyprus.trash.model

data class Tag(
    val id: String,
    val longitude: Double,
    val latitude: Double,
    val title: String,
    val description: String,
    val photoUrls: List<String>,
    val videoUrl: String
)
