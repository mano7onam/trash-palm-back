package com.cyprus.trash.api.model

data class DataContainer(
    val data: String
) {
    constructor(data: Boolean) : this(data.toString())
}
