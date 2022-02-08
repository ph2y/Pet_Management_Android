package com.sju18001.petmanagement.restapi.dto

import com.sju18001.petmanagement.restapi.dao.Review
import com.sju18001.petmanagement.restapi.global.DtoMetadata

data class CreateReviewReqDto(
    val placeId: Long,
    val rating: Int,
    val contents: String
)

data class CreateReviewResDto(
    val _metadata: DtoMetadata
)

data class FetchReviewReqDto(
    val id: Long?,
    val placeId: Long?,
    val authorId: Long?
)

data class FetchReviewResDto(
    val _metadata: DtoMetadata,
    val reviewList: List<Review>
)

data class UpdateReviewReqDto(
    val id: Long,
    val rating: Int,
    val contents: String
)

data class UpdateReviewResDto(
    val _metadata: DtoMetadata
)

data class DeleteReviewReqDto(
    val id: Long
)

data class DeleteReviewResDto(
    val _metadata: DtoMetadata
)