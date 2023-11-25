package com.cyprus.trash.model

import java.time.Instant

data class Challange(
    val id: String,
    val title: String,
    val description: String,
    val tagIds: List<Tag>,
    val prizes: List<Nft>,
    val deadline: Instant,
)
