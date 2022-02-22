package com.sju18001.petmanagement.restapi.dto

import com.sju18001.petmanagement.restapi.dao.Bookmark
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

data class FetchBookmarkReqDto(
    val id: Long?,
    val folder: String?,
    val placeId: Long?
)

data class FetchBookmarkResDto(
    val _metadata: DtoMetadata,
    val bookmarkList: List<Bookmark>,
    val bookmarkedAccountIdList: List<Long>
)

data class DeleteBookmarkReqDto(
    val id: Long?,
    val placeId: Long?
)

data class DeleteBookmarkResDto(
    val _metadata: DtoMetadata
)