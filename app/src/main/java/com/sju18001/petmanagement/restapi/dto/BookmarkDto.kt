package com.sju18001.petmanagement.restapi.dto

import com.sju18001.petmanagement.restapi.global.DtoMetadata

data class CreateBookmarkReqDto(
    val placeId: Long,
    val name: String,
    val description: String,
    val folder: String
)

data class CreateBookmarkResDto(
    val _metadata: DtoMetadata
)

data class DeleteBookmarkReqDto(
    val placeId: Long
)

data class DeleteBookmarkResDto(
    val _metadata: DtoMetadata
)