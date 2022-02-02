package com.sju18001.petmanagement.restapi.dao

import com.sju18001.petmanagement.restapi.kakaoapi.Place

data class Review(
    val id: Long,
    val author: Account,
    val place: Place?,
    val placeId: Long,
    val contents: String,
    val rating: Int,
    val timestamp: String,
    val edited: Boolean,
    val mediaAttachments: String?
)
