package com.sju18001.petmanagement.restapi.dto

import com.sju18001.petmanagement.restapi.dao.Review
import com.sju18001.petmanagement.restapi.global.DtoMetadata

data class FetchReviewReqDto(
    val id: Long?,
    val placeId: Long?,
    val authorId: Long?
)

data class FetchReviewResDto(
    val _metadata: DtoMetadata,
    val reviewList: List<Review>
)