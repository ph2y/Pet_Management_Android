package com.sju18001.petmanagement.restapi.dao

data class Review(
    val id: Long,
    val author: Account,
    val placeId: Long,
    val contents: String,
    val rating: Int,
    val timestamp: String,
    val edited: Boolean,
    val mediaAttachments: String?
)
